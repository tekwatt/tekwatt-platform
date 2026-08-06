# Firmware Service

Manages firmware artifacts and charger update jobs for OCPP 1.6 `UpdateFirmware` and OCPP 2.0.1 `UpdateFirmware` workflows on port `8096`.

- `POST/GET /api/v1/firmware/packages`
- `GET /api/v1/firmware/packages/{id}`
- `POST/GET /api/v1/firmware/jobs`
- `GET /api/v1/firmware/jobs/{id}`
- `PATCH /api/v1/firmware/jobs/{id}/status`
