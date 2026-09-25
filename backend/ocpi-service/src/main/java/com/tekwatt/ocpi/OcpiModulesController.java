package com.tekwatt.ocpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/ocpi/{tenantId}/2.2.1")
@Tag(name = "OCPI 2.2.1 Modules", description = "Tokens, Tariffs, Sessions and CDR interfaces")
public class OcpiModulesController {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final OcpiDataService data;

    public OcpiModulesController(JdbcTemplate jdbc, ObjectMapper json, OcpiDataService data) {
        this.jdbc = jdbc;
        this.json = json;
        this.data = data;
    }

    @GetMapping("/tokens")
    @Operation(summary = "List cached eMSP tokens")
    public ResponseEntity<Map<String, Object>> tokens(@PathVariable UUID tenantId,
            @RequestParam(name = "date_from", required = false) String dateFrom,
            @RequestParam(name = "date_to", required = false) String dateTo,
            @RequestParam(defaultValue = "100") int limit, @RequestParam(defaultValue = "0") int offset) {
        List<Map<String, Object>> values = jdbc.query("SELECT raw_json FROM ocpi_tokens WHERE tenant_id = ? ORDER BY last_updated, token_uid",
                (rs, row) -> readMap(rs.getString(1)), tenantId.toString());
        return page(tenantId, "tokens", filter(values, dateFrom, dateTo), dateFrom, dateTo, limit, offset);
    }

    @GetMapping("/tokens/{countryCode}/{partyId}/{tokenUid}")
    @Operation(summary = "Get a cached eMSP token")
    public Map<String, Object> token(@PathVariable UUID tenantId, @PathVariable String countryCode,
            @PathVariable String partyId, @PathVariable String tokenUid,
            @RequestParam(defaultValue = "RFID") String type) {
        return OcpiProtocol.envelope(loadToken(tenantId, countryCode, partyId, tokenUid, type));
    }

    @PutMapping("/tokens/{countryCode}/{partyId}/{tokenUid}")
    @Operation(summary = "Create or replace an eMSP token")
    public Map<String, Object> putToken(@PathVariable UUID tenantId, @PathVariable String countryCode,
            @PathVariable String partyId, @PathVariable String tokenUid,
            @RequestParam(defaultValue = "RFID") String type, @RequestBody JsonNode body) {
        validateToken(countryCode, partyId, tokenUid, type, body, false);
        storeToken(tenantId, countryCode, partyId, tokenUid, type, body);
        return OcpiProtocol.envelope(Map.of());
    }

    @PatchMapping("/tokens/{countryCode}/{partyId}/{tokenUid}")
    @Operation(summary = "Partially update an eMSP token")
    public Map<String, Object> patchToken(@PathVariable UUID tenantId, @PathVariable String countryCode,
            @PathVariable String partyId, @PathVariable String tokenUid,
            @RequestParam(defaultValue = "RFID") String type, @RequestBody JsonNode patch) {
        if (!patch.hasNonNull("last_updated")) throw new IllegalArgumentException("last_updated is required");
        ObjectNode merged = json.valueToTree(loadToken(tenantId, countryCode, partyId, tokenUid, type));
        merge(merged, patch);
        validateToken(countryCode, partyId, tokenUid, type, merged, false);
        storeToken(tenantId, countryCode, partyId, tokenUid, type, merged);
        return OcpiProtocol.envelope(Map.of());
    }

    @PostMapping("/tokens/{tokenUid}/authorize")
    @Operation(summary = "Authorize a cached token in real time")
    public ResponseEntity<Map<String, Object>> authorize(@PathVariable UUID tenantId, @PathVariable String tokenUid,
            @RequestParam(defaultValue = "RFID") String type, @RequestBody(required = false) JsonNode location) {
        List<Map<String, Object>> matches = jdbc.query("SELECT raw_json FROM ocpi_tokens WHERE tenant_id = ? AND token_uid = ? AND token_type = ? ORDER BY last_updated DESC LIMIT 1",
                (rs, row) -> readMap(rs.getString(1)), tenantId.toString(), tokenUid, type.toUpperCase());
        if (matches.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(OcpiProtocol.error(2004, "Unknown Token"));
        Map<String, Object> token = matches.getFirst();
        String allowed = Boolean.TRUE.equals(token.get("valid")) ? "ALLOWED" : "BLOCKED";
        Map<String, Object> result = OcpiDataService.map("allowed", allowed, "token", token,
                "location", location == null || location.isNull() ? null : json.convertValue(location, Map.class),
                "authorization_reference", UUID.randomUUID().toString());
        return ResponseEntity.ok(OcpiProtocol.envelope(result));
    }

    @GetMapping("/tariffs")
    @Operation(summary = "List CPO tariffs")
    public ResponseEntity<Map<String, Object>> tariffs(@PathVariable UUID tenantId,
            @RequestParam(name = "date_from", required = false) String dateFrom,
            @RequestParam(name = "date_to", required = false) String dateTo,
            @RequestParam(defaultValue = "100") int limit, @RequestParam(defaultValue = "0") int offset) {
        return page(tenantId, "tariffs", filter(data.tariffs(tenantId), dateFrom, dateTo), dateFrom, dateTo, limit, offset);
    }

    @GetMapping("/tariffs/{tariffId}")
    @Operation(summary = "Get one CPO tariff")
    public Map<String, Object> tariff(@PathVariable UUID tenantId, @PathVariable String tariffId) {
        return OcpiProtocol.envelope(data.tariff(tenantId, tariffId));
    }

    @GetMapping("/sessions")
    @Operation(summary = "List charging sessions")
    public ResponseEntity<Map<String, Object>> sessions(@PathVariable UUID tenantId,
            @RequestParam(name = "date_from", required = false) String dateFrom,
            @RequestParam(name = "date_to", required = false) String dateTo,
            @RequestParam(defaultValue = "100") int limit, @RequestParam(defaultValue = "0") int offset) {
        return page(tenantId, "sessions", filter(data.sessions(tenantId), dateFrom, dateTo), dateFrom, dateTo, limit, offset);
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get one charging session")
    public Map<String, Object> session(@PathVariable UUID tenantId, @PathVariable String sessionId) {
        return OcpiProtocol.envelope(data.session(tenantId, sessionId));
    }

    @PutMapping("/sessions/{sessionId}/charging_preferences")
    @Operation(summary = "Set driver charging preferences")
    public Map<String, Object> chargingPreferences(@PathVariable UUID tenantId, @PathVariable String sessionId,
            @RequestBody JsonNode preferences) {
        data.saveChargingPreferences(tenantId, sessionId, preferences);
        return OcpiProtocol.envelope(Map.of("result", "NOT_POSSIBLE"));
    }

    @GetMapping("/cdrs")
    @Operation(summary = "List immutable charge detail records")
    public ResponseEntity<Map<String, Object>> cdrs(@PathVariable UUID tenantId,
            @RequestParam(name = "date_from", required = false) String dateFrom,
            @RequestParam(name = "date_to", required = false) String dateTo,
            @RequestParam(defaultValue = "100") int limit, @RequestParam(defaultValue = "0") int offset) {
        return page(tenantId, "cdrs", filter(data.cdrs(tenantId), dateFrom, dateTo), dateFrom, dateTo, limit, offset);
    }

    @GetMapping("/cdrs/{cdrId}")
    @Operation(summary = "Get one immutable charge detail record")
    public Map<String, Object> cdr(@PathVariable UUID tenantId, @PathVariable String cdrId) {
        return OcpiProtocol.envelope(data.cdr(tenantId, cdrId));
    }

    private Map<String, Object> loadToken(UUID tenantId, String countryCode, String partyId, String uid, String type) {
        return jdbc.query("SELECT raw_json FROM ocpi_tokens WHERE tenant_id = ? AND country_code = ? AND party_id = ? AND token_uid = ? AND token_type = ?",
                (rs, row) -> readMap(rs.getString(1)), tenantId.toString(), countryCode.toUpperCase(), partyId.toUpperCase(), uid, type.toUpperCase())
                .stream().findFirst().orElseThrow(OcpiController.MissingLocation::new);
    }

    private void storeToken(UUID tenantId, String countryCode, String partyId, String uid, String type, JsonNode body) {
        String contractId = body.path("contract_id").asText();
        String whitelist = body.path("whitelist").asText();
        String localUserId = body.hasNonNull("local_user_id") ? body.path("local_user_id").asText() : null;
        Instant updated = OffsetDateTime.parse(body.path("last_updated").asText()).toInstant();
        jdbc.update("INSERT INTO ocpi_tokens (tenant_id, country_code, party_id, token_uid, token_type, contract_id, local_user_id, valid, whitelist, raw_json, last_updated) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE contract_id=VALUES(contract_id), local_user_id=VALUES(local_user_id), valid=VALUES(valid), whitelist=VALUES(whitelist), raw_json=VALUES(raw_json), last_updated=VALUES(last_updated)",
                tenantId.toString(), countryCode.toUpperCase(), partyId.toUpperCase(), uid, type.toUpperCase(), contractId,
                localUserId, body.path("valid").asBoolean(), whitelist, body.toString(), Timestamp.from(updated));
    }

    private static void validateToken(String countryCode, String partyId, String uid, String type, JsonNode body, boolean partial) {
        if (!partial && (!body.hasNonNull("country_code") || !body.hasNonNull("party_id") || !body.hasNonNull("uid")
                || !body.hasNonNull("type") || !body.hasNonNull("contract_id") || !body.hasNonNull("issuer")
                || !body.has("valid") || !body.hasNonNull("whitelist") || !body.hasNonNull("last_updated")))
            throw new IllegalArgumentException("Token is missing required OCPI fields");
        if (!countryCode.equalsIgnoreCase(body.path("country_code").asText()) || !partyId.equalsIgnoreCase(body.path("party_id").asText())
                || !uid.equals(body.path("uid").asText()) || !type.equalsIgnoreCase(body.path("type").asText()))
            throw new IllegalArgumentException("Token path and body identifiers must match");
        OffsetDateTime.parse(body.path("last_updated").asText());
    }

    private static void merge(ObjectNode target, JsonNode patch) {
        patch.fields().forEachRemaining(entry -> {
            JsonNode existing = target.get(entry.getKey());
            if (existing != null && existing.isObject() && entry.getValue().isObject()) merge((ObjectNode) existing, entry.getValue());
            else target.set(entry.getKey(), entry.getValue());
        });
    }

    private List<Map<String, Object>> filter(List<Map<String, Object>> values, String dateFrom, String dateTo) {
        Instant from = dateFrom == null ? Instant.MIN : OffsetDateTime.parse(dateFrom).toInstant();
        Instant to = dateTo == null ? Instant.MAX : OffsetDateTime.parse(dateTo).toInstant();
        return values.stream().filter(value -> {
            Object lastUpdated = value.get("last_updated");
            if (lastUpdated == null) return dateFrom == null && dateTo == null;
            Instant updated = OffsetDateTime.parse(lastUpdated.toString()).toInstant();
            return !updated.isBefore(from) && updated.isBefore(to);
        }).toList();
    }

    private ResponseEntity<Map<String, Object>> page(UUID tenantId, String module, List<Map<String, Object>> all,
            String dateFrom, String dateTo, int limit, int offset) {
        if (limit < 1 || limit > 1000 || offset < 0) throw new IllegalArgumentException("Invalid pagination parameters");
        int end = Math.min(offset + limit, all.size());
        List<Map<String, Object>> values = offset >= all.size() ? List.of() : new ArrayList<>(all.subList(offset, end));
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Integer.toString(all.size()));
        headers.add("X-Limit", Integer.toString(limit));
        if (end < all.size()) {
            String next = "/ocpi/" + tenantId + "/2.2.1/" + module + "?limit=" + limit + "&offset=" + end;
            if (dateFrom != null) next += "&date_from=" + URLEncoder.encode(dateFrom, StandardCharsets.UTF_8);
            if (dateTo != null) next += "&date_to=" + URLEncoder.encode(dateTo, StandardCharsets.UTF_8);
            headers.add(HttpHeaders.LINK, "<" + next + ">; rel=next");
        }
        return ResponseEntity.ok().headers(headers).body(OcpiProtocol.envelope(values));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String value) {
        try { return json.readValue(value, LinkedHashMap.class); }
        catch (Exception exception) { throw new IllegalStateException("Stored OCPI token JSON is invalid", exception); }
    }
}
