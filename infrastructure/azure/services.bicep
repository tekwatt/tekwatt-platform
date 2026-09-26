targetScope = 'resourceGroup'

param containerEnvironmentName string
param containerEnvironmentDefaultDomain string
param acrLoginServer string
param pullIdentityResourceId string
param mysqlHost string
param mysqlAdminUsername string = 'tekwattadmin'

@secure()
param mysqlAdminPassword string

@secure()
@minLength(32)
param jwtSecret string

@secure()
param ocppSharedKey string

param frontendUrl string
param imageTag string
param backendMinReplicas int = 1
param alwaysOnMinReplicas int = 1
param maxReplicas int = 2

var commonServiceEnvironment = [
  {
    name: 'DATABASE_USERNAME'
    value: mysqlAdminUsername
  }
  {
    name: 'DATABASE_PASSWORD'
    secretRef: 'mysql-password'
  }
  {
    name: 'JAVA_TOOL_OPTIONS'
    value: '-XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError'
  }
  {
    name: 'SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE'
    value: '3'
  }
  {
    name: 'SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE'
    value: '0'
  }
  {
    name: 'SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT'
    value: '30000'
  }
  {
    name: 'SPRING_DATASOURCE_HIKARI_MAX_LIFETIME'
    value: '600000'
  }
]

var services = [
  {
    name: 'api-gateway'
    port: 8080
    databaseName: ''
    databaseEnvName: ''
    external: true
    alwaysOn: true
    additionalEnvironment: [
      { name: 'CORS_ALLOWED_ORIGINS', value: frontendUrl }
      { name: 'AUTH_SERVICE_URL', value: 'http://auth-service' }
      { name: 'USER_SERVICE_URL', value: 'http://user-service' }
      { name: 'CHARGER_SERVICE_URL', value: 'http://charger-service' }
      { name: 'CHARGING_SESSION_SERVICE_URL', value: 'http://charging-session-service' }
      { name: 'TENANT_SERVICE_URL', value: 'http://tenant-service' }
      { name: 'ORGANIZATION_SERVICE_URL', value: 'http://organization-service' }
      { name: 'CONNECTOR_SERVICE_URL', value: 'http://connector-service' }
      { name: 'TARIFF_SERVICE_URL', value: 'http://tariff-service' }
      { name: 'RESERVATION_SERVICE_URL', value: 'http://reservation-service' }
      { name: 'BILLING_SERVICE_URL', value: 'http://billing-service' }
      { name: 'INVOICE_SERVICE_URL', value: 'http://invoice-service' }
      { name: 'PAYMENT_SERVICE_URL', value: 'http://payment-service' }
      { name: 'NOTIFICATION_SERVICE_URL', value: 'http://notification-service' }
      { name: 'OCPP_GATEWAY_URL', value: 'https://ocpp-gateway.internal.${containerEnvironmentDefaultDomain}' }
      { name: 'OCPP_GATEWAY_WS_URL', value: 'ws://ocpp-gateway' }
      { name: 'TELEMETRY_SERVICE_URL', value: 'http://telemetry-service' }
      { name: 'FIRMWARE_SERVICE_URL', value: 'http://firmware-service' }
      { name: 'AUDIT_SERVICE_URL', value: 'http://audit-service' }
      { name: 'ANALYTICS_SERVICE_URL', value: 'http://analytics-service' }
      { name: 'REPORTING_SERVICE_URL', value: 'http://reporting-service' }
      { name: 'ADMIN_SERVICE_URL', value: 'http://admin-service' }
      { name: 'SUPPORT_SERVICE_URL', value: 'http://support-service' }
      { name: 'OCPI_SERVICE_URL', value: 'http://ocpi-service' }
    ]
  }
  {
    name: 'auth-service'
    port: 8081
    databaseName: 'tekwatt_auth'
    databaseEnvName: 'DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: [
      { name: 'JWT_SECRET', secretRef: 'jwt-secret' }
      { name: 'JWT_ACCESS_TOKEN_TTL_SECONDS', value: '900' }
      { name: 'JWT_REFRESH_TOKEN_TTL_SECONDS', value: '2592000' }
    ]
  }
  {
    name: 'user-service'
    port: 8082
    databaseName: 'tekwatt_users'
    databaseEnvName: 'DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: []
  }
  {
    name: 'charger-service'
    port: 8083
    databaseName: 'tekwatt_chargers'
    databaseEnvName: 'DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: []
  }
  {
    name: 'charging-session-service'
    port: 8084
    databaseName: 'tekwatt_charging_sessions'
    databaseEnvName: 'DATABASE_URL'
    external: false
    alwaysOn: true
    additionalEnvironment: [
      { name: 'TARIFF_SERVICE_URL', value: 'http://tariff-service' }
      { name: 'BILLING_SERVICE_URL', value: 'http://billing-service' }
      { name: 'INVOICE_SERVICE_URL', value: 'http://invoice-service' }
      { name: 'USER_SERVICE_URL', value: 'http://user-service' }
      { name: 'NOTIFICATION_SERVICE_URL', value: 'http://notification-service' }
      { name: 'CUSTOMER_PORTAL_URL', value: frontendUrl }
      { name: 'PAYMENT_SMS_ENABLED', value: 'false' }
    ]
  }
  {
    name: 'tenant-service'
    port: 8085
    databaseName: 'tekwatt_tenants'
    databaseEnvName: 'DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: []
  }
  {
    name: 'organization-service'
    port: 8086
    databaseName: 'tekwatt_organizations'
    databaseEnvName: 'DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: []
  }
  {
    name: 'connector-service'
    port: 8087
    databaseName: 'tekwatt_connectors'
    databaseEnvName: 'DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: []
  }
  {
    name: 'tariff-service'
    port: 8088
    databaseName: 'tekwatt_tariffs'
    databaseEnvName: 'DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: []
  }
  {
    name: 'reservation-service'
    port: 8089
    databaseName: 'tekwatt_reservations'
    databaseEnvName: 'DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: []
  }
  {
    name: 'billing-service'
    port: 8090
    databaseName: 'tekwatt_billing'
    databaseEnvName: 'DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: []
  }
  {
    name: 'invoice-service'
    port: 8091
    databaseName: 'tekwatt_invoices'
    databaseEnvName: 'DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: []
  }
  {
    name: 'payment-service'
    port: 8092
    databaseName: 'tekwatt_payments'
    databaseEnvName: 'DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: []
  }
  {
    name: 'notification-service'
    port: 8093
    databaseName: 'tekwatt_notifications'
    databaseEnvName: 'DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: []
  }
  {
    name: 'ocpp-gateway'
    port: 8094
    databaseName: 'tekwatt_ocpp'
    databaseEnvName: 'DATABASE_URL'
    external: false
    alwaysOn: true
    additionalEnvironment: [
      { name: 'OCPP_SHARED_KEY', secretRef: 'ocpp-shared-key' }
      { name: 'USER_SERVICE_URL', value: 'http://user-service' }
      { name: 'CHARGER_SERVICE_URL', value: 'http://charger-service' }
      { name: 'CHARGING_SESSION_SERVICE_URL', value: 'http://charging-session-service' }
      { name: 'CONNECTOR_SERVICE_URL', value: 'http://connector-service' }
      { name: 'TELEMETRY_SERVICE_URL', value: 'http://telemetry-service' }
      { name: 'FIRMWARE_SERVICE_URL', value: 'http://firmware-service' }
    ]
  }
  {
    name: 'telemetry-service'
    port: 8095
    databaseName: 'tekwatt_telemetry'
    databaseEnvName: 'DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: []
  }
  {
    name: 'firmware-service'
    port: 8096
    databaseName: 'tekwatt_firmware'
    databaseEnvName: 'DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: [
      { name: 'OCPP_GATEWAY_URL', value: 'http://ocpp-gateway' }
    ]
  }
  {
    name: 'audit-service'
    port: 8097
    databaseName: 'tekwatt_audit'
    databaseEnvName: 'DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: []
  }
  {
    name: 'analytics-service'
    port: 8098
    databaseName: 'tekwatt_analytics'
    databaseEnvName: 'ANALYTICS_DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: []
  }
  {
    name: 'reporting-service'
    port: 8099
    databaseName: 'tekwatt_reporting'
    databaseEnvName: 'REPORTING_DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: [
      { name: 'ANALYTICS_SERVICE_URL', value: 'http://analytics-service' }
    ]
  }
  {
    name: 'admin-service'
    port: 8100
    databaseName: 'tekwatt_admin'
    databaseEnvName: 'ADMIN_DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: [
      { name: 'TENANT_SERVICE_URL', value: 'http://tenant-service' }
      { name: 'USER_SERVICE_URL', value: 'http://user-service' }
      { name: 'CHARGER_SERVICE_URL', value: 'http://charger-service' }
      { name: 'TELEMETRY_SERVICE_URL', value: 'http://telemetry-service' }
      { name: 'ORGANIZATION_SERVICE_URL', value: 'http://organization-service' }
      { name: 'ANALYTICS_SERVICE_URL', value: 'http://analytics-service' }
      { name: 'REPORTING_SERVICE_URL', value: 'http://reporting-service' }
      { name: 'OCPP_GATEWAY_URL', value: 'http://ocpp-gateway' }
    ]
  }
  {
    name: 'support-service'
    port: 8101
    databaseName: 'tekwatt_support'
    databaseEnvName: 'DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: []
  }
  {
    name: 'ocpi-service'
    port: 8110
    databaseName: 'tekwatt_ocpi'
    databaseEnvName: 'OCPI_DATABASE_URL'
    external: false
    alwaysOn: false
    additionalEnvironment: [
      { name: 'OCPI_PUBLIC_BASE_URL', value: 'https://api-gateway.${containerEnvironmentDefaultDomain}' }
      { name: 'CHARGER_SERVICE_URL', value: 'http://charger-service' }
      { name: 'CONNECTOR_SERVICE_URL', value: 'http://connector-service' }
      { name: 'TARIFF_SERVICE_URL', value: 'http://tariff-service' }
      { name: 'CHARGING_SESSION_SERVICE_URL', value: 'http://charging-session-service' }
      { name: 'RESERVATION_SERVICE_URL', value: 'http://reservation-service' }
      { name: 'OCPP_GATEWAY_URL', value: 'http://ocpp-gateway' }
    ]
  }
]

resource environment 'Microsoft.App/managedEnvironments@2024-03-01' existing = {
  name: containerEnvironmentName
}

resource containerApps 'Microsoft.App/containerApps@2024-03-01' = [for service in services: {
  name: service.name
  location: resourceGroup().location
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${pullIdentityResourceId}': {}
    }
  }
  properties: {
    environmentId: environment.id
    workloadProfileName: 'Consumption'
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: {
        external: service.external
        allowInsecure: false
        targetPort: service.port
        transport: 'auto'
        traffic: [
          {
            latestRevision: true
            weight: 100
          }
        ]
      }
      registries: [
        {
          server: acrLoginServer
          identity: pullIdentityResourceId
        }
      ]
      secrets: [
        {
          name: 'mysql-password'
          value: mysqlAdminPassword
        }
        {
          name: 'jwt-secret'
          value: jwtSecret
        }
        {
          name: 'ocpp-shared-key'
          value: ocppSharedKey
        }
      ]
    }
    template: {
      containers: [
        {
          name: service.name
          image: '${acrLoginServer}/tekwatt/${service.name}:${imageTag}'
          env: concat(
            commonServiceEnvironment,
            empty(service.databaseName) ? [] : [
              {
                name: service.databaseEnvName
                value: 'jdbc:mysql://${mysqlHost}:3306/${service.databaseName}?serverTimezone=UTC&sslMode=REQUIRED'
              }
            ],
            service.additionalEnvironment
          )
          resources: {
            cpu: json('0.5')
            memory: '1Gi'
          }
          probes: [
            {
              type: 'Startup'
              httpGet: {
                path: '/actuator/health/liveness'
                port: service.port
                scheme: 'HTTP'
              }
              initialDelaySeconds: 15
              periodSeconds: 10
              timeoutSeconds: 5
              failureThreshold: 30
            }
            {
              type: 'Liveness'
              httpGet: {
                path: '/actuator/health/liveness'
                port: service.port
                scheme: 'HTTP'
              }
              periodSeconds: 30
              timeoutSeconds: 5
              failureThreshold: 3
            }
            {
              type: 'Readiness'
              httpGet: {
                path: '/actuator/health/readiness'
                port: service.port
                scheme: 'HTTP'
              }
              periodSeconds: 10
              timeoutSeconds: 5
              failureThreshold: 12
            }
          ]
        }
      ]
      scale: {
        minReplicas: service.alwaysOn ? alwaysOnMinReplicas : backendMinReplicas
        maxReplicas: maxReplicas
        rules: [
          {
            name: 'http-scaling'
            http: {
              metadata: {
                concurrentRequests: '50'
              }
            }
          }
        ]
      }
    }
  }
}]

output apiGatewayFqdn string = containerApps[0].properties.configuration.ingress.fqdn
output apiGatewayUrl string = 'https://${containerApps[0].properties.configuration.ingress.fqdn}'
output ocppWebSocketUrl string = 'wss://${containerApps[0].properties.configuration.ingress.fqdn}/ocpp/{stationId}'
