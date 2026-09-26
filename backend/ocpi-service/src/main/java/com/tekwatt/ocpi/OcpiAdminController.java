package com.tekwatt.ocpi;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/ocpi")
public class OcpiAdminController {
    private final JdbcTemplate jdbc;
    private final OcpiCredentialsController credentials;
    private final String bootstrapAdminKey;
    private final SecureRandom random = new SecureRandom();

    public OcpiAdminController(JdbcTemplate jdbc, OcpiCredentialsController credentials,
            @Value("${tekwatt.ocpi.bootstrap-admin-key:}") String bootstrapAdminKey) {
        this.jdbc = jdbc;
        this.credentials = credentials;
        this.bootstrapAdminKey = bootstrapAdminKey;
    }

    @GetMapping("/configuration")
    public Map<String, Object> configuration(@RequestParam UUID tenantId) {
        return jdbc.query("SELECT country_code,party_id,business_name,country,time_zone,enabled FROM ocpi_parties WHERE tenant_id=?",
                rs -> rs.next() ? map("tenantId", tenantId, "countryCode", rs.getString(1), "partyId", rs.getString(2),
                        "businessName", rs.getString(3), "country", rs.getString(4), "timeZone", rs.getString(5),
                        "enabled", rs.getBoolean(6)) : Map.of(), tenantId.toString());
    }

    @PutMapping("/configuration")
    public Map<String, Object> saveConfiguration(@RequestParam UUID tenantId, @RequestBody JsonNode body) {
        String countryCode = required(body, "countryCode").toUpperCase(Locale.ROOT);
        String partyId = required(body, "partyId").toUpperCase(Locale.ROOT);
        String businessName = required(body, "businessName");
        String country = required(body, "country").toUpperCase(Locale.ROOT);
        String timeZone = required(body, "timeZone");
        if (countryCode.length() != 2 || partyId.length() != 3 || country.length() != 3)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OCPI country code, party ID or country is invalid");
        jdbc.update("INSERT INTO ocpi_parties(tenant_id,country_code,party_id,business_name,country,time_zone,enabled,created_at) VALUES(?,?,?,?,?,?,TRUE,?) " +
                        "ON DUPLICATE KEY UPDATE country_code=VALUES(country_code),party_id=VALUES(party_id),business_name=VALUES(business_name),country=VALUES(country),time_zone=VALUES(time_zone),enabled=TRUE",
                tenantId.toString(), countryCode, partyId, businessName, country, timeZone, Timestamp.from(Instant.now()));
        return configuration(tenantId);
    }

    @GetMapping("/partners")
    public List<PartnerView> partners(@RequestParam UUID tenantId) {
        return jdbc.query("SELECT id,partner_name,partner_country_code,partner_party_id,versions_url,roles_json,connection_status,enabled,created_at,updated_at FROM ocpi_partner_tokens WHERE tenant_id=? AND partner_country_code IS NOT NULL ORDER BY updated_at DESC",
                (rs, row) -> new PartnerView(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getBoolean(8),
                        rs.getTimestamp(9).toInstant(), rs.getTimestamp(10).toInstant()), tenantId.toString());
    }

    @PostMapping("/partners")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@RequestParam UUID tenantId, @RequestBody JsonNode body) {
        if (configuration(tenantId).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Save the local OCPI party configuration before registering a partner");
        return credentials.register(tenantId, body);
    }

    @PostMapping("/bootstrap-tokens")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createBootstrapToken(@RequestParam UUID tenantId,
            @RequestHeader(value = "X-OCPI-Admin-Key", required = false) String suppliedAdminKey,
            @RequestBody JsonNode body) {
        if (bootstrapAdminKey.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "OCPI bootstrap token creation is not enabled");
        if (suppliedAdminKey == null || !MessageDigest.isEqual(
                bootstrapAdminKey.getBytes(StandardCharsets.UTF_8),
                suppliedAdminKey.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid OCPI administration key");
        if (configuration(tenantId).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Save the local OCPI party configuration before creating a bootstrap token");

        String partnerName = required(body, "partnerName");
        if (partnerName.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "partnerName is too long");
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("INSERT INTO ocpi_partner_tokens " +
                        "(tenant_id,partner_name,token_sha256,enabled,connection_status) " +
                        "VALUES(?,?,?,TRUE,'BOOTSTRAP')",
                tenantId.toString(), partnerName, OcpiProtocol.sha256(token));
        return Map.of("partnerName", partnerName, "token", token, "oneTime", true);
    }

    @DeleteMapping("/partners/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(@RequestParam UUID tenantId, @PathVariable long id) {
        int changed = jdbc.update("UPDATE ocpi_partner_tokens SET enabled=FALSE,connection_status='UNREGISTERED',issued_token=NULL,outbound_token=NULL,updated_at=? WHERE id=? AND tenant_id=?",
                Timestamp.from(Instant.now()), id, tenantId.toString());
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "OCPI partner not found");
    }

    @GetMapping("/summary")
    public Map<String, Integer> summary(@RequestParam UUID tenantId) {
        String id = tenantId.toString();
        return Map.of(
                "partners", count("SELECT COUNT(*) FROM ocpi_partner_tokens WHERE tenant_id=? AND partner_country_code IS NOT NULL AND enabled=TRUE", id),
                "tokens", count("SELECT COUNT(*) FROM ocpi_tokens WHERE tenant_id=?", id),
                "cdrs", count("SELECT COUNT(*) FROM ocpi_cdrs WHERE tenant_id=?", id),
                "commands", count("SELECT COUNT(*) FROM ocpi_commands WHERE tenant_id=?", id));
    }

    private int count(String sql, String tenantId) {
        Integer value = jdbc.queryForObject(sql, Integer.class, tenantId);
        return value == null ? 0 : value;
    }

    private static String required(JsonNode body, String name) {
        String value = body.path(name).asText().trim();
        if (value.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, name + " is required");
        return value;
    }

    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) result.put(String.valueOf(values[index]), values[index + 1]);
        return result;
    }

    public record PartnerView(long id, String partnerName, String countryCode, String partyId,
                              String versionsUrl, String rolesJson, String status, boolean enabled,
                              Instant createdAt, Instant updatedAt) {}
}
