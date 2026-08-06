# Local MySQL setup

TekWatt uses one MySQL 8 server with a separate database for each service. Docker users can run `docker compose up -d mysql`. For a locally installed server, execute `init/01-create-service-databases.sql` as the `tekwatt` database user, then start each service; Flyway creates its tables automatically.

Keep real passwords in a local `.env` file. Never commit them.
