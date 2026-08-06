# Audit Service

Stores immutable security and business audit events on port `8097`.

- `POST /api/v1/audit-events`
- `GET /api/v1/audit-events/{id}`
- `GET /api/v1/audit-events?tenantId={tenantId}&action={action}&resourceType={type}&from={instant}&to={instant}`

The write endpoint is intended for trusted internal services. Event records have no update or delete endpoint.
