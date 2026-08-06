# Telemetry Service

Stores normalized OCPP sampled values on port `8095`. The generic measurand model supports OCPP 1.6 and OCPP 2.0.1 values such as energy, power, voltage, current, state of charge, temperature, and frequency.

- `POST /api/v1/telemetry/readings`
- `POST /api/v1/telemetry/readings/batch`
- `GET /api/v1/telemetry/readings?chargerId={chargerId}&from={instant}&to={instant}`
- `GET /api/v1/telemetry/readings/latest?connectorId={connectorId}`
