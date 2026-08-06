# Local PostgreSQL setup

Docker Compose initializes one PostgreSQL server with separate databases owned
by the `tekwatt` development user:

| Service | Database | JDBC URL |
| --- | --- | --- |
| Authentication Service | `tekwatt_auth` | `jdbc:postgresql://localhost:5432/tekwatt_auth` |
| User Service | `tekwatt_users` | `jdbc:postgresql://localhost:5432/tekwatt_users` |

## Start and verify

```bash
docker compose up -d postgres
docker compose ps
make db-status
```

Flyway runs each service's migrations when that service starts. The database
creation script only runs when the `postgres-data` volume is initialized for
the first time.

If the volume existed before this setup was added, create the databases
manually with the SQL in `init/01-create-service-databases.sql` or intentionally
recreate the local development volume.

Do not use the example password or shared development role in deployed
environments. Production should provision separate credentials and secrets for
every service.
