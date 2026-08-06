# Payment Service

Tracks payment provider attempts and refunds on port `8092`. Provider adapters will call the result endpoint after external processing.

- `POST /api/v1/payments`
- `GET /api/v1/payments/{id}`
- `GET /api/v1/payments?tenantId={tenantId}`
- `PATCH /api/v1/payments/{id}/result`
- `POST /api/v1/payments/{id}/refund`
