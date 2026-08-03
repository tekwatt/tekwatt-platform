# API Gateway

The API Gateway is the public entry point for TekWatt APIs. It routes versioned
requests to backend services and provides consistent CORS and health-check
behaviour.

## Routes

| Path | Target environment variable | Default |
| --- | --- | --- |
| `/api/v1/auth/**` | `AUTH_SERVICE_URL` | `http://localhost:8081` |
| `/api/v1/users/**` | `USER_SERVICE_URL` | `http://localhost:8082` |
| `/api/v1/chargers/**` | `CHARGER_SERVICE_URL` | `http://localhost:8083` |
| `/api/v1/charging-sessions/**` | `CHARGING_SESSION_SERVICE_URL` | `http://localhost:8084` |

## Run locally

From the repository root:

```bash
mvn -pl backend/api-gateway spring-boot:run
```

Health checks are available at `/actuator/health`, `/actuator/health/liveness`,
and `/actuator/health/readiness`.
