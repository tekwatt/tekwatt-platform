# OCPP Gateway

Accepts OCPP 1.6 JSON and OCPP 2.0.1 WebSocket connections at `ws://localhost:8094/ocpp/{stationId}`. Chargers negotiate either `ocpp1.6` or `ocpp2.0.1` using the WebSocket subprotocol header. If `OCPP_SHARED_KEY` is configured, chargers must send it in the `X-OCPP-Key` header. The API Gateway exposes the same endpoint at `ws://localhost:8080/ocpp/{stationId}`.

Supported calls include `BootNotification`, `Heartbeat`, `StatusNotification`, `Authorize`, `MeterValues`, OCPP 1.6 `StartTransaction`/`StopTransaction`, and OCPP 2.0.1 `TransactionEvent`/`NotifyEvent`.

- `GET /api/v1/ocpp/connections`
- `GET /api/v1/ocpp/connections/{stationId}`
