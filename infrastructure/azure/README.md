# TekWatt Azure deployment

This directory contains the production-oriented Azure deployment for the TekWatt web platform.

## Architecture

- Azure Static Web Apps hosts the Vite/React admin portal.
- Azure Container Apps runs the API Gateway and 21 internal microservices.
- Only the API Gateway is public. The other services use Container Apps service discovery.
- Azure Database for MySQL Flexible Server is private and contains one database per service.
- Azure Container Registry stores the service images.
- Log Analytics collects Container Apps logs.
- The API Gateway and OCPP Gateway always keep one replica running. Other services start at zero replicas by default and wake on HTTP traffic.

## Files

- `foundation.bicep` creates networking, MySQL, databases, registry, logging, the Container Apps environment, and Static Web Apps.
- `services.bicep` deploys all 22 backend containers and their configuration.
- `backend.Dockerfile` is the common Java 21 runtime image.
- `tools/deploy-azure.ps1` builds and deploys the whole platform.

Secrets are entered securely at deployment time and are not stored in Git.
