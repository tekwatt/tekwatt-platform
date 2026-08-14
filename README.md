# TekWatt EV Charging Platform

TekWatt is a modular EV charging platform for operating chargers, managing charging sessions, pricing, billing, payments, and customer experiences.

## Repository layout

- `backend/` — Java/Spring microservices and Maven modules
- `frontend/` — web portals, mobile app, and shared UI
- `shared/` — reusable platform libraries
- `integration/` — external protocol and provider adapters
- `database/` — database assets, migrations, and seeds
- `infrastructure/` — cloud and Kubernetes infrastructure
- `devops/` — delivery automation and operational tooling
- `testing/` — cross-platform test suites
- `docs/` — product, architecture, API, and operational documentation

## Getting started

1. Copy `.env.example` to `.env` and set development values.
2. Start local dependencies with `docker compose up -d`.
3. Use `make help` to see common commands.

The service implementations will be introduced incrementally; this commit establishes the shared platform foundation.

## Start and stop all backend services

After building with Maven, start every service from PowerShell:

```powershell
.\tools\start-all.ps1 -DatabasePassword "your-database-password"
```

Stop the processes started by the launcher:

```powershell
.\tools\stop-all.ps1
```
