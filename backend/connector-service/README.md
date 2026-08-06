# Connector Service

Manages the physical charging ports attached to chargers. It runs on port `8087` and stores data in `tekwatt_connectors`.

## Endpoints

- `POST /api/v1/connectors` - create a connector
- `GET /api/v1/connectors/{id}` - get one connector
- `GET /api/v1/connectors?chargerId={chargerId}` - list a charger's connectors
- `PUT /api/v1/connectors/{id}` - update connector capabilities
- `PATCH /api/v1/connectors/{id}/status` - update connector status
