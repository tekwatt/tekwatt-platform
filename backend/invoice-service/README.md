# Invoice Service

Creates customer-facing invoice records from bills and tracks their lifecycle on port `8091`.

- `POST /api/v1/invoices`
- `GET /api/v1/invoices/{id}`
- `GET /api/v1/invoices?tenantId={tenantId}`
- `POST /api/v1/invoices/{id}/issue`
- `POST /api/v1/invoices/{id}/pay`
- `POST /api/v1/invoices/{id}/void`
