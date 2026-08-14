# Swagger and OpenAPI

Start API Gateway and the services, then open:

`http://localhost:8080/swagger-ui.html`

Use the definition selector at the top to switch between all TekWatt services. Click **Authorize** and enter the JWT access token returned by `POST /api/v1/auth/login` or `POST /api/v1/auth/register`.

Each service also exposes its own OpenAPI JSON at `http://localhost:{service-port}/v3/api-docs`. API Gateway proxies those definitions under `/openapi/{service}/v3/api-docs` so the browser only needs port `8080`.
