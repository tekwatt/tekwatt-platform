# OCPP Gateway

Accepts OCPP 1.6 JSON and OCPP 2.0.1 WebSocket connections at `ws://localhost:8094/ocpp/{stationId}`. Chargers negotiate either `ocpp1.6` or `ocpp2.0.1` using the WebSocket subprotocol header. If `OCPP_SHARED_KEY` is configured, chargers must send it in the `X-OCPP-Key` header. The API Gateway exposes the same endpoint at `ws://localhost:8080/ocpp/{stationId}`.

Supported calls include `BootNotification`, `Heartbeat`, `StatusNotification`, `Authorize`, `MeterValues`, OCPP 1.6 `StartTransaction`/`StopTransaction`, and OCPP 2.0.1 `TransactionEvent`/`NotifyEvent`.

## Real-time simulator setup

Connect the simulator to `ws://localhost:8080/ocpp/{stationId}` (or use the server LAN address) and negotiate the `ocpp2.0.1` WebSocket subprotocol. The `{stationId}` must exactly match a charger created in the web application. Configure at least one connector, assign an active tariff to the charger, and issue an active RFID card to a customer before sending a `TransactionEvent` with `eventType: Started`.

Accepted OCPP events are applied to their owning services: boot and heartbeat events update the charger, status events update the connector, transaction events create/update/complete charging sessions, and sampled values are stored by the telemetry service. The admin portal's server-sent event stream refreshes Dashboard, Live Monitoring, Sessions, and OCPP Logs every five seconds.

- `GET /api/v1/ocpp/connections`
- `GET /api/v1/ocpp/connections/{stationId}`
