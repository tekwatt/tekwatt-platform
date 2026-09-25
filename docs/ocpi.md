# OCPI 2.2.1 roaming integration

TekWatt exposes a tenant-scoped OCPI 2.2.1 CPO interface. The implementation connects OCPI directly to the existing charger, connector, tariff, charging-session and OCPP services so that roaming data stays aligned with the operational platform.

## Implemented modules

| Module | TekWatt role | Supported operations |
|---|---|---|
| Credentials | Receiver | Register, retrieve, rotate and unregister credentials; discover and store the partner's 2.2.1 endpoints |
| Locations | Sender | Paginated live Locations, EVSEs and Connectors from charger and connector services |
| Tokens | Receiver | PUT/PATCH token cache, GET/list and real-time authorization |
| Tariffs | Sender | Paginated pull of CPO tariffs |
| Sessions | Sender | Paginated pull of live/completed sessions; charging preferences are validated and recorded but return `NOT_POSSIBLE` until smart charging is enabled |
| CDRs | Sender | Paginated immutable CDR snapshots generated from completed sessions |
| Commands | Receiver | START_SESSION, STOP_SESSION, RESERVE_NOW, CANCEL_RESERVATION and UNLOCK_CONNECTOR |

Commands are asynchronous. TekWatt immediately returns `ACCEPTED`, translates the command to OCPP 1.6J or OCPP 2.0.1, waits for the charging station's CALLRESULT/CALLERROR, and posts the final result to the supplied `response_url`. Command requests and callback results are retained for audit.

This is an interoperable OCPI implementation, but it is not a claim of formal OCPI certification. Run conformance and partner acceptance tests before production roaming. The current CPO data modules support the pull model. Partner-side push delivery and automatic retry queues can be added when required by a specific roaming agreement.

## Endpoint discovery

The service runs locally on port `8110` and is exposed by the API Gateway under `/ocpi/{tenant-id}`.

```text
GET /ocpi/{tenant-id}/versions
GET /ocpi/{tenant-id}/2.2.1
```

The version-detail response advertises Credentials Receiver, Locations Sender, Tokens Receiver, Tariffs Sender, Sessions Sender, CDRs Sender and Commands Receiver.

Set `OCPI_PUBLIC_BASE_URL` to the public HTTPS API Gateway origin in production. The local default is `http://localhost:8080`.

## Prepare a tenant

The service uses the `tekwatt_ocpi` MySQL database. The shared database initialization script creates the database and grants the TekWatt database user access. Flyway creates and upgrades all OCPI tables when the service starts.

Insert the tenant's registered OCPI identity once:

```sql
INSERT INTO tekwatt_ocpi.ocpi_parties
  (tenant_id, country_code, party_id, business_name, country, time_zone)
VALUES
  ('YOUR-TENANT-UUID', 'IN', 'TKW', 'TekWatt Charging Solutions', 'IND', 'Asia/Kolkata');
```

`country_code` is the ISO-3166 alpha-2 code and `party_id` is the three-character OCPI party identifier agreed with roaming partners.

## Credentials exchange

Create a different one-time bootstrap token for every roaming partner. Never commit a raw token to Git or place it in a migration. The following PowerShell creates the token and its SHA-256 database value:

```powershell
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$token = [Convert]::ToBase64String($bytes)
$sha = [Security.Cryptography.SHA256]::Create()
$hash = [BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($token))).Replace('-', '').ToLowerInvariant()
$sha.Dispose()
```

Store only the hash in the bootstrap record:

```sql
INSERT INTO tekwatt_ocpi.ocpi_partner_tokens
  (tenant_id, partner_name, token_sha256, connection_status)
VALUES
  ('YOUR-TENANT-UUID', 'Roaming partner name', 'THE-LOWERCASE-SHA256-HASH', 'BOOTSTRAP');
```

Share `$token` through a secure channel. The partner uses it for the initial `POST /ocpi/{tenant-id}/2.2.1/credentials`. TekWatt then:

1. validates the partner role identity and HTTPS versions URL;
2. calls the partner's versions endpoint with the received token;
3. verifies OCPI 2.2.1 support and stores the discovered endpoints;
4. creates a new random token for subsequent calls; and
5. invalidates the one-time bootstrap token.

Production partner and callback URLs must use HTTPS. Plain HTTP is accepted only for `localhost` and `127.0.0.1` development endpoints. A Credentials `PUT` rotates both directions of the connection and performs discovery again. `DELETE` revokes the connection immediately.

## Authorization and request headers

OCPI requests use:

```text
Authorization: Token <base64-encoded-token>
X-Request-ID: <UUID>
X-Correlation-ID: <UUID>
```

For a local check:

```powershell
$wireToken = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($token))
$headers = @{
  Authorization = "Token $wireToken"
  'X-Request-ID' = [guid]::NewGuid().ToString()
  'X-Correlation-ID' = [guid]::NewGuid().ToString()
}
Invoke-RestMethod "http://localhost:8080/ocpi/YOUR-TENANT-UUID/versions" -Headers $headers
Invoke-RestMethod "http://localhost:8080/ocpi/YOUR-TENANT-UUID/2.2.1/locations" -Headers $headers
```

All list endpoints accept `date_from`, `date_to`, `offset` and `limit`. Responses include `X-Total-Count`, `X-Limit`, and a `Link` header when another page exists.

## Token-to-user mapping

The Tokens Receiver stores the original OCPI token and its authorization fields. TekWatt also accepts the optional internal extension `local_user_id`; when present, Sessions and CDRs can associate the roaming token with the corresponding TekWatt user. This extension is never required from an external partner.

## Command behavior

The Commands Receiver maps operations as follows:

| OCPI command | OCPP 1.6J | OCPP 2.0.1 |
|---|---|---|
| START_SESSION | RemoteStartTransaction | RequestStartTransaction |
| STOP_SESSION | RemoteStopTransaction | RequestStopTransaction |
| RESERVE_NOW | ReserveNow | ReserveNow |
| CANCEL_RESERVATION | CancelReservation | CancelReservation |
| UNLOCK_CONNECTOR | UnlockConnector | UnlockConnector |

The requested station must currently be connected to the OCPP Gateway and its negotiated protocol must match the registered charger protocol. A station rejection, OCPP CALLERROR, connection failure or 30-second timeout is returned asynchronously to the partner's `response_url`.

## Swagger and verification

After starting the platform, open `http://localhost:8080/swagger-ui.html` and choose **OCPI 2.2.1 Service**. Select **Authorize** and enter the complete value `Token <base64-encoded-token>`. The aggregated OpenAPI document is available at `http://localhost:8080/openapi/ocpi/v3/api-docs`.

Build and test the two affected backend modules with:

```powershell
& "C:\software\maven\apache-maven-3.9.16\bin\mvn.cmd" "-Dmaven.repo.local=$PWD\.maven-repository" -pl backend/ocpi-service,backend/ocpp-gateway -am test
```

Before onboarding a production partner, verify tenant identity, partner URLs, token rotation, pagination, token authorization, tariff VAT/currency, Session-to-CDR totals, every OCPP command on the target charger models, and failed callback retry requirements in the commercial roaming agreement.
