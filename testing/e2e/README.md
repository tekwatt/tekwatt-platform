# Platform end-to-end tests

This opt-in suite tests the live platform through API Gateway. It covers authentication, tenant/user setup, charger and connector provisioning, OCPP 2.0.1 BootNotification and TransactionEvent, session metering, billing, analytics, and PDF reporting.

## Prerequisites

Start API Gateway and services on their default ports (`8080`-`8100`) with MySQL available. Reporting Service must be able to reach Analytics Service.

## Run

```powershell
$env:E2E_BASE_URL = "http://localhost:8080"
$env:E2E_WS_URL = "ws://localhost:8080"
& "C:\software\maven\apache-maven-3.9.16\bin\mvn.cmd" -pl testing/e2e -Pe2e verify
```

If OCPP Gateway has a shared key, also set `E2E_OCPP_KEY`. Each run uses unique email, tenant slug, station ID, serial number, and transaction ID values.
