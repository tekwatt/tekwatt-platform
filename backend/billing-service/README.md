# Billing Service

Creates immutable charging cost breakdowns and tracks payment status on port `8090`.

- `POST /api/v1/bills`
- `GET /api/v1/bills/{id}`
- `GET /api/v1/bills?tenantId={tenantId}`
- `POST /api/v1/bills/{id}/pay`
- `POST /api/v1/bills/{id}/void`
