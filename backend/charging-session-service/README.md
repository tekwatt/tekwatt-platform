# Charging Session Service

Tracks EV charging sessions, meter readings, energy consumption, and estimated cost. Runs on port `8084` with the `tekwatt_charging_sessions` database.

## Endpoints

- `POST /api/v1/charging-sessions` - start a session
- `GET /api/v1/charging-sessions/{id}` - get a session
- `GET /api/v1/charging-sessions?tenantId={tenantId}` - list tenant sessions
- `POST /api/v1/charging-sessions/{id}/meter-values` - record a cumulative meter value
- `POST /api/v1/charging-sessions/{id}/stop` - stop and calculate the session total
