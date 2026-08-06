# Charger Service

Manages the charger inventory, operational status, and heartbeat timestamps. It runs on port `8083` and stores data in the `tekwatt_chargers` MySQL database.

## Endpoints

- `POST /api/v1/chargers` - register a charger
- `GET /api/v1/chargers/{id}` - get a charger
- `GET /api/v1/chargers?tenantId={tenantId}` - list chargers for a tenant
- `PUT /api/v1/chargers/{id}` - update charger details
- `PATCH /api/v1/chargers/{id}/status` - update operational status
- `POST /api/v1/chargers/{id}/heartbeat` - record a heartbeat
