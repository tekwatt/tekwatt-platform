# User Service

The User Service owns user profile data linked to Authentication Service users.
It listens on port 8082 and is routed through `/api/v1/users/**` by the gateway.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/v1/users` | Create a profile |
| GET | `/api/v1/users/{id}` | Get a profile |
| GET | `/api/v1/users/by-auth-user/{authUserId}` | Resolve an authentication user |
| GET | `/api/v1/users?tenantId={tenantId}` | List profiles with optional tenant filter |
| PUT | `/api/v1/users/{id}` | Update profile details |
| DELETE | `/api/v1/users/{id}` | Deactivate a profile |
