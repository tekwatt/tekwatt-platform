# Admin Service

Platform administration facade on port `8100`. It reads tenant, user, charger, and OCPP connection data from their owning services and records every mutating action.

- `GET /api/v1/admin/tenants`
- `GET /api/v1/admin/users?tenantId={id}`
- `GET /api/v1/admin/chargers?tenantId={id}`
- `GET /api/v1/admin/ocpp-connections`
- `POST /api/v1/admin/actions`
- `GET /api/v1/admin/actions?actorId={id}`
- `GET /api/v1/admin/actions/{id}`

Supported actions: `TENANT_STATUS_CHANGE`, `CHARGER_STATUS_CHANGE`, `USER_DEACTIVATE`, and `CHARGER_HEARTBEAT`.
