# Analytics Service

Aggregates tenant charging performance on port `8098`.

- `POST /api/v1/analytics/events` records an idempotent completed-session metric.
- `GET /api/v1/analytics/overview?tenantId={id}&from={instant}&to={instant}` returns totals.
- `GET /api/v1/analytics/daily?tenantId={id}&from={date}&to={date}` returns daily trends.

Amounts use the currency's major unit and energy is measured in kWh.
