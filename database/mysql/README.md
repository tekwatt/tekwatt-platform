# Local MySQL setup

TekWatt uses one MySQL 8 server with a separate database for each service. Docker users can run `docker compose up -d mysql`. For a locally installed server, execute `init/01-create-service-databases.sql` as a MySQL administrator with database creation and grant privileges, then start each service; Flyway creates its tables automatically.

Keep real passwords in a local `.env` file. Never commit them.

The OCPI roaming service uses the new `tekwatt_ocpi` database. If MySQL was initialized before OCPI was added, rerun the updated `init/01-create-service-databases.sql` before starting all services. See [OCPI setup](../../docs/ocpi.md) for tenant and partner-token provisioning.
