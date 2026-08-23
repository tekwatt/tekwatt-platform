# Deploy TekWatt to Azure

## What is deployed

| Layer | Azure service | Exposure |
|---|---|---|
| Admin web application | Azure Static Web Apps | Public HTTPS |
| API Gateway | Azure Container Apps | Public HTTPS and secure WebSocket |
| 21 backend services | Azure Container Apps | Internal only |
| 21 service databases | Azure Database for MySQL Flexible Server | Private VNet only |
| Container images | Azure Container Registry | Azure identity access |
| Logs | Log Analytics | Azure portal and CLI |

The default backend region is Central India. Static Web Apps uses East Asia because it is the nearest supported Static Web Apps region in this deployment.

## Before deployment

1. Open a new PowerShell window after installing Azure CLI.
2. Go to the repository:

   ```powershell
   Set-Location C:\Users\magpier\tekwatt
   ```

3. Sign in and select the subscription:

   ```powershell
   az login
   az account list --output table
   az account set --subscription "YOUR SUBSCRIPTION ID"
   ```

4. Stop the locally running Java services. This avoids locked JAR files during the build:

   ```powershell
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\tools\stop-all.ps1" -ForceAllJava
   ```

## Deploy all three layers

Run:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\tools\deploy-azure.ps1"
```

The script asks for three new production secrets:

- MySQL administrator password: at least 12 characters.
- JWT signing secret: at least 32 characters.
- OCPP shared key: at least 16 characters.

Do not reuse the local development password. The script then shows the Azure subscription and billable resources and requires `DEPLOY` confirmation.

The first deployment builds and publishes 22 service images, so it commonly takes 30–60 minutes. Later deployments can reuse already built JAR files:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\tools\deploy-azure.ps1" -SkipBackendBuild
```

## URLs printed at completion

- Frontend: the Azure Static Web Apps URL.
- API Gateway: the public backend URL.
- Swagger: `<API Gateway URL>/swagger-ui.html`.
- OCPP 2.0.1: `wss://<API Gateway host>/ocpp/{stationId}`.

The frontend is compiled with the Azure API Gateway address. The backend CORS policy is restricted to the deployed frontend address.

## Scaling and cost controls

The defaults keep the API Gateway and OCPP Gateway continuously available, while business services can scale to zero. For production traffic, keep all services warm with:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\tools\deploy-azure.ps1" -BackendMinReplicas 1
```

This improves response time but increases monthly cost because 22 Java containers remain active.

## Check deployment health

```powershell
$resourceGroup = "tekwatt-prod-rg"
$gatewayHost = az containerapp show --name api-gateway --resource-group $resourceGroup --query properties.configuration.ingress.fqdn --output tsv
Invoke-RestMethod "https://$gatewayHost/actuator/health"
az containerapp list --resource-group $resourceGroup --output table
```

View a service's recent logs:

```powershell
az containerapp logs show --name auth-service --resource-group tekwatt-prod-rg --type system --tail 100
az containerapp logs show --name auth-service --resource-group tekwatt-prod-rg --type console --tail 100
```

## Remove the Azure environment

This deletes the deployed frontend, backend, database, images, and logs:

```powershell
az group delete --name tekwatt-prod-rg
```

Azure asks for confirmation before deletion.
