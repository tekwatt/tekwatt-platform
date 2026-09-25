package com.tekwatt.ocpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/ocpi/{tenantId}/2.2.1/credentials")
@Tag(name = "OCPI 2.2.1 Credentials", description = "Symmetric OCPI registration, rotation and unregistration")
public class OcpiCredentialsController {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final OcpiDataService data;
    private final RestClient client;
    private final String baseUrl;
    private final SecureRandom random = new SecureRandom();

    public OcpiCredentialsController(JdbcTemplate jdbc, ObjectMapper json, OcpiDataService data,
            @org.springframework.beans.factory.annotation.Value("${tekwatt.ocpi.public-base-url}") String baseUrl) {
        this.jdbc = jdbc;
        this.json = json;
        this.data = data;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    @GetMapping
    @Operation(summary = "Retrieve TekWatt credentials")
    public Map<String, Object> get(@PathVariable UUID tenantId, HttpServletRequest request) {
        Connection connection = registeredConnection(tenantId, request);
        return OcpiProtocol.envelope(credentials(tenantId, connection.issuedToken()));
    }

    @PostMapping
    @Operation(summary = "Register a new OCPI roaming platform")
    public Map<String, Object> register(@PathVariable UUID tenantId, @RequestBody JsonNode body) {
        Incoming incoming = validate(body);
        String endpoints = discover(incoming);
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM ocpi_partner_tokens WHERE tenant_id = ? AND partner_country_code = ? AND partner_party_id = ? AND connection_status = 'REGISTERED' AND enabled = TRUE",
                Integer.class, tenantId.toString(), incoming.countryCode(), incoming.partyId());
        if (count != null && count > 0) throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Partner is already registered");
        String issued = newToken();
        jdbc.update("INSERT INTO ocpi_partner_tokens (tenant_id, partner_name, token_sha256, enabled, partner_country_code, partner_party_id, versions_url, outbound_token, issued_token, roles_json, endpoints_json, connection_status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, TRUE, ?, ?, ?, ?, ?, ?, ?, 'REGISTERED', ?, ?)",
                tenantId.toString(), incoming.partnerName(), OcpiProtocol.sha256(issued), incoming.countryCode(), incoming.partyId(),
                incoming.versionsUrl(), incoming.outboundToken(), issued, incoming.rolesJson(), endpoints,
                Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
        jdbc.update("UPDATE ocpi_partner_tokens SET enabled = FALSE, connection_status = 'CONSUMED' WHERE tenant_id = ? AND partner_country_code IS NULL AND connection_status = 'BOOTSTRAP'",
                tenantId.toString());
        return OcpiProtocol.envelope(credentials(tenantId, issued));
    }

    @PutMapping
    @Operation(summary = "Update or rotate OCPI credentials")
    public Map<String, Object> update(@PathVariable UUID tenantId, HttpServletRequest request, @RequestBody JsonNode body) {
        Connection current = registeredConnection(tenantId, request);
        Incoming incoming = validate(body);
        String endpoints = discover(incoming);
        String issued = newToken();
        jdbc.update("UPDATE ocpi_partner_tokens SET partner_name=?, token_sha256=?, partner_country_code=?, partner_party_id=?, versions_url=?, outbound_token=?, issued_token=?, roles_json=?, endpoints_json=?, updated_at=? WHERE id=?",
                incoming.partnerName(), OcpiProtocol.sha256(issued), incoming.countryCode(), incoming.partyId(), incoming.versionsUrl(),
                incoming.outboundToken(), issued, incoming.rolesJson(), endpoints, Timestamp.from(Instant.now()), current.id());
        return OcpiProtocol.envelope(credentials(tenantId, issued));
    }

    @DeleteMapping
    @Operation(summary = "Unregister an OCPI roaming platform")
    public Map<String, Object> unregister(@PathVariable UUID tenantId, HttpServletRequest request) {
        Connection connection = registeredConnection(tenantId, request);
        jdbc.update("UPDATE ocpi_partner_tokens SET enabled=FALSE, connection_status='UNREGISTERED', issued_token=NULL, outbound_token=NULL, updated_at=? WHERE id=?",
                Timestamp.from(Instant.now()), connection.id());
        return OcpiProtocol.envelope(Map.of());
    }

    private Map<String, Object> credentials(UUID tenantId, String token) {
        OcpiController.Party party = data.party(tenantId);
        return Map.of("token", token, "url", baseUrl + "/ocpi/" + tenantId + "/versions", "roles", List.of(Map.of(
                "role", "CPO", "party_id", party.partyId(), "country_code", party.countryCode(),
                "business_details", Map.of("name", party.businessName()))));
    }

    private Incoming validate(JsonNode body) {
        if (!body.hasNonNull("token") || !body.hasNonNull("url") || !body.path("roles").isArray() || body.path("roles").isEmpty())
            throw new IllegalArgumentException("Credentials require token, url and at least one role");
        String outboundToken = body.path("token").asText();
        if (outboundToken.length() > 64 || outboundToken.isEmpty()
                || outboundToken.chars().anyMatch(character -> character < 0x21 || character > 0x7e))
            throw new IllegalArgumentException("Credentials token must be 1-64 printable non-whitespace ASCII characters");
        JsonNode role = body.path("roles").get(0);
        Set<String> identities = new java.util.HashSet<>();
        Set<String> allowedRoles = Set.of("CPO", "EMSP", "HUB", "NAP", "NSP", "OTHER", "SCSP");
        for (JsonNode candidate : body.path("roles")) {
            String candidateCountry = candidate.path("country_code").asText().toUpperCase();
            String candidateParty = candidate.path("party_id").asText().toUpperCase();
            String candidateRole = candidate.path("role").asText().toUpperCase();
            String candidateName = candidate.path("business_details").path("name").asText();
            if (candidateCountry.length() != 2 || candidateParty.length() != 3 || candidateName.isBlank()
                    || !allowedRoles.contains(candidateRole))
                throw new IllegalArgumentException("Invalid OCPI role identity");
            if (!identities.add(candidateRole + "|" + candidateCountry + "|" + candidateParty))
                throw new IllegalArgumentException("Duplicate OCPI role identity");
        }
        String country = role.path("country_code").asText().toUpperCase();
        String party = role.path("party_id").asText().toUpperCase();
        String name = role.path("business_details").path("name").asText();
        String url = body.path("url").asText();
        validateUrl(url, "Credentials versions URL");
        try { return new Incoming(outboundToken, url, country, party, name, json.writeValueAsString(body.path("roles"))); }
        catch (Exception exception) { throw new IllegalArgumentException("Credentials roles are invalid", exception); }
    }

    private String discover(Incoming incoming) {
        try {
            String authorization = "Token " + Base64.getEncoder().encodeToString(
                    incoming.outboundToken().getBytes(StandardCharsets.UTF_8));
            JsonNode versions = get(incoming.versionsUrl(), authorization);
            JsonNode supported = versions.path("data");
            if (!supported.isArray()) throw new IllegalArgumentException("Partner versions response has no data array");
            String detailUrl = null;
            for (JsonNode version : supported) {
                if ("2.2.1".equals(version.path("version").asText())) {
                    detailUrl = version.path("url").asText(null);
                    break;
                }
            }
            if (detailUrl == null || detailUrl.isBlank())
                throw new IllegalArgumentException("Partner does not advertise OCPI 2.2.1");
            validateUrl(detailUrl, "Partner version detail URL");
            JsonNode details = get(detailUrl, authorization).path("data");
            if (!"2.2.1".equals(details.path("version").asText()) || !details.path("endpoints").isArray())
                throw new IllegalArgumentException("Partner OCPI 2.2.1 endpoint response is invalid");
            return json.writeValueAsString(details.path("endpoints"));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not discover the partner OCPI 2.2.1 endpoints", exception);
        }
    }

    private JsonNode get(String url, String authorization) {
        JsonNode result = client.get().uri(url).header(HttpHeaders.AUTHORIZATION, authorization)
                .header("X-Request-ID", UUID.randomUUID().toString())
                .header("X-Correlation-ID", UUID.randomUUID().toString())
                .retrieve().body(JsonNode.class);
        if (result == null) throw new IllegalArgumentException("Partner returned an empty OCPI response");
        return result;
    }

    private static void validateUrl(String value, String field) {
        java.net.URI uri;
        try { uri = java.net.URI.create(value); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException(field + " is invalid", exception); }
        boolean local = "http".equalsIgnoreCase(uri.getScheme())
                && ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
        if (!("https".equalsIgnoreCase(uri.getScheme()) || local) || uri.getHost() == null)
            throw new IllegalArgumentException(field + " must use HTTPS");
    }

    private Connection registeredConnection(UUID tenantId, HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Token ") ? header.substring(6).trim() : "";
        List<String> candidates = new java.util.ArrayList<>();
        if (!token.isBlank()) candidates.add(OcpiProtocol.sha256(token));
        try { candidates.add(OcpiProtocol.sha256(new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8))); }
        catch (IllegalArgumentException ignored) { }
        for (String hash : candidates) {
            List<Connection> rows = jdbc.query("SELECT id, issued_token FROM ocpi_partner_tokens WHERE tenant_id=? AND token_sha256=? AND enabled=TRUE AND connection_status='REGISTERED'",
                    (rs, row) -> new Connection(rs.getLong(1), rs.getString(2)), tenantId.toString(), hash);
            if (!rows.isEmpty()) return rows.getFirst();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Registered OCPI credentials are required");
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record Connection(long id, String issuedToken) {}
    private record Incoming(String outboundToken, String versionsUrl, String countryCode, String partyId,
                            String partnerName, String rolesJson) {}
}
