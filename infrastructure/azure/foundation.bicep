targetScope = 'resourceGroup'

@description('Short lowercase prefix used in globally unique Azure resource names.')
@minLength(3)
@maxLength(12)
param prefix string = 'tekwatt'

@description('Azure region for the backend and database.')
param location string = resourceGroup().location

@description('Azure Static Web Apps supported region nearest to India.')
param staticWebLocation string = 'eastasia'

@description('MySQL administrator login. This is used by the application services.')
param mysqlAdminUsername string = 'tekwattadmin'

@secure()
@description('MySQL administrator password.')
param mysqlAdminPassword string

var unique = take(uniqueString(subscription().subscriptionId, resourceGroup().id), 8)
var acrName = '${prefix}${unique}acr'
var mysqlServerName = '${prefix}-${unique}-mysql'
var containerEnvironmentName = '${prefix}-${unique}-env'
var staticWebAppName = '${prefix}-${unique}-web'
var identityName = '${prefix}-${unique}-pull'

var databaseNames = [
  'tekwatt_auth'
  'tekwatt_users'
  'tekwatt_tenants'
  'tekwatt_organizations'
  'tekwatt_chargers'
  'tekwatt_connectors'
  'tekwatt_charging_sessions'
  'tekwatt_tariffs'
  'tekwatt_reservations'
  'tekwatt_billing'
  'tekwatt_invoices'
  'tekwatt_payments'
  'tekwatt_notifications'
  'tekwatt_ocpp'
  'tekwatt_telemetry'
  'tekwatt_firmware'
  'tekwatt_audit'
  'tekwatt_analytics'
  'tekwatt_reporting'
  'tekwatt_admin'
  'tekwatt_support'
  'tekwatt_ocpi'
]

resource vnet 'Microsoft.Network/virtualNetworks@2024-05-01' = {
  name: '${prefix}-vnet'
  location: location
  properties: {
    addressSpace: {
      addressPrefixes: [
        '10.42.0.0/16'
      ]
    }
    subnets: [
      {
        name: 'container-apps'
        properties: {
          addressPrefix: '10.42.0.0/23'
          delegations: [
            {
              name: 'Microsoft.App-environments'
              properties: {
                serviceName: 'Microsoft.App/environments'
              }
            }
          ]
        }
      }
      {
        name: 'mysql'
        properties: {
          addressPrefix: '10.42.2.0/24'
          delegations: [
            {
              name: 'Microsoft.DBforMySQL-flexibleServers'
              properties: {
                serviceName: 'Microsoft.DBforMySQL/flexibleServers'
              }
            }
          ]
        }
      }
    ]
  }
}

resource containerSubnet 'Microsoft.Network/virtualNetworks/subnets@2024-05-01' existing = {
  name: 'container-apps'
  parent: vnet
}

resource mysqlSubnet 'Microsoft.Network/virtualNetworks/subnets@2024-05-01' existing = {
  name: 'mysql'
  parent: vnet
}

resource mysqlPrivateDns 'Microsoft.Network/privateDnsZones@2024-06-01' = {
  name: '${prefix}.mysql.database.azure.com'
  location: 'global'
}

resource mysqlDnsLink 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2024-06-01' = {
  name: '${prefix}-mysql-link'
  parent: mysqlPrivateDns
  location: 'global'
  properties: {
    registrationEnabled: false
    virtualNetwork: {
      id: vnet.id
    }
  }
}

resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2023-09-01' = {
  name: '${prefix}-${unique}-logs'
  location: location
  properties: {
    retentionInDays: 30
    sku: {
      name: 'PerGB2018'
    }
  }
}

resource containerEnvironment 'Microsoft.App/managedEnvironments@2024-03-01' = {
  name: containerEnvironmentName
  location: location
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: logAnalytics.properties.customerId
        sharedKey: logAnalytics.listKeys().primarySharedKey
      }
    }
    vnetConfiguration: {
      infrastructureSubnetId: containerSubnet.id
      internal: false
    }
    workloadProfiles: [
      {
        name: 'Consumption'
        workloadProfileType: 'Consumption'
      }
    ]
  }
}

resource registry 'Microsoft.ContainerRegistry/registries@2023-11-01-preview' = {
  name: acrName
  location: location
  sku: {
    name: 'Basic'
  }
  properties: {
    adminUserEnabled: false
    publicNetworkAccess: 'Enabled'
  }
}

resource pullIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: identityName
  location: location
}

resource acrPull 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(registry.id, pullIdentity.id, 'AcrPull')
  scope: registry
  properties: {
    principalId: pullIdentity.properties.principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '7f951dda-4ed3-4680-a7ca-43fe172d538d')
  }
}

resource mysql 'Microsoft.DBforMySQL/flexibleServers@2023-12-30' = {
  name: mysqlServerName
  location: location
  sku: {
    name: 'Standard_B1ms'
    tier: 'Burstable'
  }
  properties: {
    administratorLogin: mysqlAdminUsername
    administratorLoginPassword: mysqlAdminPassword
    version: '8.0.21'
    availabilityZone: '1'
    backup: {
      backupRetentionDays: 7
      geoRedundantBackup: 'Disabled'
    }
    highAvailability: {
      mode: 'Disabled'
    }
    network: {
      delegatedSubnetResourceId: mysqlSubnet.id
      privateDnsZoneResourceId: mysqlPrivateDns.id
    }
    storage: {
      autoGrow: 'Enabled'
      storageSizeGB: 32
      iops: 360
    }
  }
  dependsOn: [
    mysqlDnsLink
  ]
}

resource databases 'Microsoft.DBforMySQL/flexibleServers/databases@2023-12-30' = [for databaseName in databaseNames: {
  name: databaseName
  parent: mysql
  properties: {
    charset: 'utf8mb4'
    collation: 'utf8mb4_unicode_ci'
  }
}]

resource staticWebApp 'Microsoft.Web/staticSites@2023-12-01' = {
  name: staticWebAppName
  location: staticWebLocation
  sku: {
    name: 'Free'
    tier: 'Free'
  }
  properties: {
    allowConfigFileUpdates: true
  }
}

output acrName string = registry.name
output acrLoginServer string = registry.properties.loginServer
output containerEnvironmentName string = containerEnvironment.name
output containerEnvironmentDefaultDomain string = containerEnvironment.properties.defaultDomain
output pullIdentityResourceId string = pullIdentity.id
output mysqlHost string = mysql.properties.fullyQualifiedDomainName
output staticWebAppName string = staticWebApp.name
output staticWebAppUrl string = 'https://${staticWebApp.properties.defaultHostname}'
