# Organization Service

Manages tenant-owned organizational units and parent-child hierarchy on port 8086.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/v1/organizations` | Create organization |
| GET | `/api/v1/organizations/{id}` | Get organization |
| GET | `/api/v1/organizations?tenantId={tenantId}` | List organizations |
| PUT | `/api/v1/organizations/{id}` | Update organization |
| DELETE | `/api/v1/organizations/{id}` | Deactivate organization |
