# Tenant Service

Manages TekWatt tenant organizations and lifecycle status on port 8085.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/v1/tenants` | Create tenant |
| GET | `/api/v1/tenants/{id}` | Get tenant |
| GET | `/api/v1/tenants` | List tenants |
| PUT | `/api/v1/tenants/{id}` | Update tenant |
| PATCH | `/api/v1/tenants/{id}/status` | Change lifecycle status |
