# Notification Service

Queues email, SMS, and push notifications and tracks delivery attempts on port `8093`.

- `POST /api/v1/notifications`
- `GET /api/v1/notifications/{id}`
- `GET /api/v1/notifications?tenantId={tenantId}`
- `POST /api/v1/notifications/{id}/send`
- `PATCH /api/v1/notifications/{id}/result`
- `POST /api/v1/notifications/{id}/retry`
