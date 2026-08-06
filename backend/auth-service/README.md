# Authentication Service

The service provides user registration, login, short-lived JWT access tokens,
and rotating refresh tokens for TekWatt clients.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | Register a driver account |
| POST | `/api/v1/auth/login` | Exchange credentials for tokens |
| POST | `/api/v1/auth/refresh` | Rotate a refresh token |

Set a unique `JWT_SECRET` of at least 32 characters in every deployed
environment. The default local port is 8081.
