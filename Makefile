.PHONY: help up down logs db-status db-shell

help:
	@echo "Available commands: up, down, logs, db-status, db-shell"

up:
	docker compose up -d

down:
	docker compose down

logs:
	docker compose logs -f

db-status:
	docker compose exec postgres psql -U tekwatt -d tekwatt -c "SELECT datname FROM pg_database WHERE datname LIKE 'tekwatt%';"

db-shell:
	docker compose exec postgres psql -U tekwatt -d tekwatt
