# OCPI roaming integration (first release)

TekWatt exposes tenant-scoped OCPI 2.2.1 **CPO Locations Sender** endpoints. It reads live station and connector records from the existing services; no separate location copy needs to be kept in sync. This is a read-only roaming integration, **not yet a complete OCPI implementation**. Credentials exchange, Tokens, Tariffs, Sessions, CDRs and Commands are not advertised or implemented. A partner must accept manual token exchange to use this first release.

## Configure a tenant and partner

The OCPI service uses the `tekwatt_ocpi` database. For an existing local MySQL installation, create that database and grant the TekWatt user access as in [01-create-service-databases.sql](../database/mysql/init/01-create-service-databases.sql). Flyway creates its tables when the service starts. Do not put raw partner tokens in Git or in SQL scripts.

Generate a distinct cryptographically random token for each roaming partner and compute its SHA-256 hash. For example, in PowerShell:

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

Store `$token` securely and give it to the partner through a secure channel. Use `$hash` (not the token) in the database. Replace the sample tenant UUID, country/party identifiers, and legal business details with your real registered CPO identity:

```sql
INSERT INTO tekwatt_ocpi.ocpi_parties
  (tenant_id, country_code, party_id, business_name, country, time_zone)
VALUES
  ('YOUR-TENANT-UUID', 'IN', 'TKW', 'Your registered CPO name', 'IND', 'Asia/Kolkata');

INSERT INTO tekwatt_ocpi.ocpi_partner_tokens
  (tenant_id, partner_name, token_sha256)
VALUES
  ('YOUR-TENANT-UUID', 'Your roaming partner', 'THE-LOWERCASE-SHA256-HASH');
```

Disabling or deleting a partner-token row immediately revokes its access. Each partner has its own token; all location queries are restricted to the tenant in the URL.

## Start and test

The new service runs on port 8110 and is routed through the API Gateway at `/ocpi/{tenant-id}`. Set `OCPI_PUBLIC_BASE_URL` to the public HTTPS gateway origin in production; the local default is `http://localhost:8080`. Start the services using the normal `tools/start-all.ps1` command after building the Maven reactor. The service also needs the charger and connector services available.

OCPI sends the token base64-encoded in `Authorization: Token ...`. For a quick local PowerShell check:

```powershell
$wireToken = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($token))
$headers = @{ Authorization = "Token $wireToken"; 'X-Request-ID' = [guid]::NewGuid().ToString(); 'X-Correlation-ID' = [guid]::NewGuid().ToString() }
Invoke-RestMethod "http://localhost:8080/ocpi/YOUR-TENANT-UUID/versions" -Headers $headers
Invoke-RestMethod "http://localhost:8080/ocpi/YOUR-TENANT-UUID/2.2.1/locations" -Headers $headers
```

The locations endpoint supports OCPI `date_from`, `date_to`, `limit` and `offset`, with `X-Total-Count`, `X-Limit` and a next-page `Link` header. An individual location is at `/ocpi/{tenant-id}/2.2.1/locations/{country-code}/{party-id}/{charger-uuid}`.

Only active stations with coordinates, address, city and at least one supported connector with voltage/current are published. OCPI connector format is inferred from connector type (Type 2 and GB/T AC as socket; other supported types as cable) because the current connector model does not record physical format. Confirm this assumption against hardware before offering locations to a roaming network. The tenant's configured country and time zone are used for every location; use separate tenant identities if stations span countries/time zones.

The current first release does not push updates to partners; they must poll Locations. Station or connector changes become visible on the next GET. Before production roaming, implement partner Credentials exchange, physical connector format, per-station country/time zone, and the other modules required by your roaming partner.
