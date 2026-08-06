# Reporting Service

Creates persistent CSV and HTML reports from Analytics Service data on port `8099`. Set `format` to `CSV` or `HTML`; omitted format defaults to `CSV`.

- `POST /api/v1/reports` generates an `OVERVIEW` or `DAILY` report.
- `GET /api/v1/reports/{id}` returns report status and metadata.
- `GET /api/v1/reports?tenantId={id}` lists a tenant's reports.
- `GET /api/v1/reports/{id}/download` downloads a completed CSV file.
