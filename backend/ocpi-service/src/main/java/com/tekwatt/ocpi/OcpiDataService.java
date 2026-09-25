package com.tekwatt.ocpi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OcpiDataService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final RestClient client;
    private final String chargerUrl;
    private final String connectorUrl;
    private final String tariffUrl;
    private final String sessionUrl;

    public OcpiDataService(JdbcTemplate jdbc, ObjectMapper json,
            @Value("${tekwatt.ocpi.charger-service-url}") String chargerUrl,
            @Value("${tekwatt.ocpi.connector-service-url}") String connectorUrl,
            @Value("${tekwatt.ocpi.tariff-service-url}") String tariffUrl,
            @Value("${tekwatt.ocpi.session-service-url}") String sessionUrl) {
        this.jdbc = jdbc;
        this.json = json;
        this.chargerUrl = trim(chargerUrl);
        this.connectorUrl = trim(connectorUrl);
        this.tariffUrl = trim(tariffUrl);
        this.sessionUrl = trim(sessionUrl);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    public OcpiController.Party party(UUID tenantId) {
        return jdbc.query("SELECT country_code, party_id, business_name, country, time_zone FROM ocpi_parties WHERE tenant_id = ? AND enabled = TRUE",
                (rs, row) -> new OcpiController.Party(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)),
                tenantId.toString()).stream().findFirst().orElseThrow(OcpiController.MissingLocation::new);
    }

    public List<Map<String, Object>> tariffs(UUID tenantId) {
        var party = party(tenantId);
        return array(tariffUrl + "/api/v1/tariffs?tenantId=" + tenantId).stream()
                .map(node -> tariff(node, party)).toList();
    }

    public Map<String, Object> tariff(UUID tenantId, String tariffId) {
        var party = party(tenantId);
        JsonNode tariff = get(tariffUrl + "/api/v1/tariffs/" + tariffId);
        if (!tenantId.toString().equals(tariff.path("tenantId").asText())) throw new OcpiController.MissingLocation();
        return tariff(tariff, party);
    }

    public List<Map<String, Object>> sessions(UUID tenantId) {
        var party = party(tenantId);
        return array(sessionUrl + "/api/v1/charging-sessions?tenantId=" + tenantId).stream()
                .map(node -> session(node, party)).toList();
    }

    public Map<String, Object> session(UUID tenantId, String sessionId) {
        var party = party(tenantId);
        JsonNode session = get(sessionUrl + "/api/v1/charging-sessions/" + sessionId);
        if (!tenantId.toString().equals(session.path("tenantId").asText())) throw new OcpiController.MissingLocation();
        return session(session, party);
    }

    public void saveChargingPreferences(UUID tenantId, String sessionId, JsonNode preferences) {
        session(tenantId, sessionId);
        if (!preferences.hasNonNull("profile_type")) throw new IllegalArgumentException("profile_type is required");
        jdbc.update("INSERT INTO ocpi_charging_preferences (tenant_id, session_id, preferences_json, updated_at) VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE preferences_json = VALUES(preferences_json), updated_at = VALUES(updated_at)",
                tenantId.toString(), sessionId, preferences.toString(), java.sql.Timestamp.from(Instant.now()));
    }

    public List<Map<String, Object>> cdrs(UUID tenantId) {
        syncCdrs(tenantId);
        return jdbc.query("SELECT cdr_json FROM ocpi_cdrs WHERE tenant_id = ? ORDER BY created_at, cdr_id",
                (rs, row) -> parseMap(rs.getString(1)), tenantId.toString());
    }

    public Map<String, Object> cdr(UUID tenantId, String cdrId) {
        syncCdrs(tenantId);
        return jdbc.query("SELECT cdr_json FROM ocpi_cdrs WHERE tenant_id = ? AND cdr_id = ?",
                (rs, row) -> parseMap(rs.getString(1)), tenantId.toString(), cdrId)
                .stream().findFirst().orElseThrow(OcpiController.MissingLocation::new);
    }

    public JsonNode charger(UUID tenantId, String chargerId) {
        JsonNode charger = get(chargerUrl + "/api/v1/chargers/" + chargerId);
        if (!tenantId.toString().equals(charger.path("tenantId").asText())) throw new OcpiController.MissingLocation();
        return charger;
    }

    public JsonNode connectorFor(UUID tenantId, JsonNode charger, String connectorId) {
        return array(connectorUrl + "/api/v1/connectors?chargerId=" + charger.path("id").asText()).stream()
                .filter(node -> tenantId.toString().equals(node.path("tenantId").asText()))
                .filter(node -> connectorId == null || connectorId.equals(node.path("id").asText())
                        || connectorId.equals(node.path("connectorNumber").asText()))
                .findFirst().orElseThrow(OcpiController.MissingLocation::new);
    }

    public JsonNode rawSession(UUID tenantId, String sessionId) {
        JsonNode session = get(sessionUrl + "/api/v1/charging-sessions/" + sessionId);
        if (!tenantId.toString().equals(session.path("tenantId").asText())) throw new OcpiController.MissingLocation();
        return session;
    }

    private Map<String, Object> tariff(JsonNode node, OcpiController.Party party) {
        List<Map<String, Object>> components = new ArrayList<>();
        addPrice(components, "ENERGY", decimal(node, "energyPricePerKwh"), decimal(node, "taxPercent"), 1);
        addPrice(components, "TIME", decimal(node, "timePricePerMinute").multiply(BigDecimal.valueOf(60)), decimal(node, "taxPercent"), 60);
        addPrice(components, "FLAT", decimal(node, "sessionFee"), decimal(node, "taxPercent"), 1);
        if (components.isEmpty()) components.add(price("FLAT", BigDecimal.ZERO, decimal(node, "taxPercent"), 1));
        Map<String, Object> result = map(
                "country_code", party.countryCode(), "party_id", party.partyId(), "id", node.path("id").asText(),
                "currency", node.path("currency").asText("INR"), "elements", List.of(Map.of("price_components", components)),
                "start_date_time", text(node, "validFrom"), "end_date_time", text(node, "validTo"),
                "last_updated", instant(node, "updatedAt", "createdAt"));
        return result;
    }

    private Map<String, Object> session(JsonNode node, OcpiController.Party party) {
        JsonNode charger = charger(UUID.fromString(node.path("tenantId").asText()), node.path("chargerId").asText());
        JsonNode connector = connectorFor(UUID.fromString(node.path("tenantId").asText()), charger, node.path("connectorId").asText());
        Map<String, Object> result = map(
                "country_code", party.countryCode(), "party_id", party.partyId(), "id", node.path("id").asText(),
                "start_date_time", instant(node, "startedAt"), "end_date_time", text(node, "stoppedAt"),
                "kwh", decimal(node, "energyKwh"), "cdr_token", cdrToken(node, party), "auth_method", "WHITELIST",
                "location_id", charger.path("id").asText(), "evse_uid", charger.path("id").asText(),
                "connector_id", connector.path("connectorNumber").asText(), "meter_id", text(charger, "meterSerialNumber"),
                "currency", node.path("currency").asText("INR"), "charging_periods", chargingPeriods(node),
                "total_cost", priceFromIncl(decimal(node, "totalCost"), decimal(node, "taxPercent")),
                "status", sessionStatus(node.path("status").asText()), "last_updated", instant(node, "updatedAt", "startedAt"));
        return result;
    }

    private void syncCdrs(UUID tenantId) {
        OcpiController.Party party = party(tenantId);
        for (JsonNode session : array(sessionUrl + "/api/v1/charging-sessions?tenantId=" + tenantId)) {
            if (!isComplete(session) || session.path("stoppedAt").isMissingNode() || session.path("stoppedAt").isNull()) continue;
            String id = session.path("id").asText();
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM ocpi_cdrs WHERE tenant_id = ? AND session_id = ?",
                    Integer.class, tenantId.toString(), id);
            if (count != null && count > 0) continue;
            Map<String, Object> cdr = cdr(session, party);
            Instant created = Instant.parse((String) cdr.get("last_updated"));
            jdbc.update("INSERT INTO ocpi_cdrs (tenant_id, cdr_id, session_id, cdr_json, created_at, last_updated) VALUES (?, ?, ?, ?, ?, ?)",
                    tenantId.toString(), id, id, write(cdr), java.sql.Timestamp.from(created), java.sql.Timestamp.from(created));
        }
    }

    private Map<String, Object> cdr(JsonNode node, OcpiController.Party party) {
        UUID tenantId = UUID.fromString(node.path("tenantId").asText());
        JsonNode charger = charger(tenantId, node.path("chargerId").asText());
        JsonNode connector = connectorFor(tenantId, charger, node.path("connectorId").asText());
        Instant start = Instant.parse(instant(node, "startedAt"));
        Instant end = Instant.parse(instant(node, "stoppedAt", "updatedAt"));
        BigDecimal hours = BigDecimal.valueOf(Duration.between(start, end).toSeconds())
                .divide(BigDecimal.valueOf(3600), 6, RoundingMode.HALF_UP);
        BigDecimal energy = decimal(node, "energyKwh");
        BigDecimal energyCost = energy.multiply(decimal(node, "pricePerKwh"));
        BigDecimal timeCost = BigDecimal.valueOf(Duration.between(start, end).toMinutes())
                .multiply(decimal(node, "timePricePerMinute"));
        Map<String, Object> location = map("id", charger.path("id").asText(), "name", text(charger, "stationName"),
                "address", charger.path("address").asText(), "city", charger.path("city").asText(),
                "state", text(charger, "state"), "country", party.country(),
                "coordinates", Map.of("latitude", charger.path("latitude").asText(), "longitude", charger.path("longitude").asText()),
                "evse_uid", charger.path("id").asText(),
                "connector_id", connector.path("connectorNumber").asText(), "connector_standard", connectorStandard(connector.path("type").asText()),
                "connector_format", connectorFormat(connector.path("type").asText()), "connector_power_type", powerType(connector.path("type").asText()));
        List<Map<String, Object>> tariffs = node.path("tariffId").isTextual() ? List.of(sessionTariff(node, party)) : List.of();
        return map("country_code", party.countryCode(), "party_id", party.partyId(), "id", node.path("id").asText(),
                "start_date_time", start.toString(), "end_date_time", end.toString(), "session_id", node.path("id").asText(),
                "cdr_token", cdrToken(node, party), "auth_method", "WHITELIST", "cdr_location", location,
                "meter_id", text(charger, "meterSerialNumber"), "currency", node.path("currency").asText("INR"),
                "tariffs", tariffs, "charging_periods", chargingPeriods(node),
                "total_cost", priceFromIncl(decimal(node, "totalCost"), decimal(node, "taxPercent")),
                "total_fixed_cost", priceFromExcl(decimal(node, "sessionFee"), decimal(node, "taxPercent")),
                "total_energy", energy, "total_energy_cost", priceFromExcl(energyCost, decimal(node, "taxPercent")),
                "total_time", hours, "total_time_cost", priceFromExcl(timeCost, decimal(node, "taxPercent")),
                "last_updated", end.toString());
    }

    private Map<String, Object> sessionTariff(JsonNode session, OcpiController.Party party) {
        List<Map<String, Object>> components = new ArrayList<>();
        addPrice(components, "ENERGY", decimal(session, "pricePerKwh"), decimal(session, "taxPercent"), 1);
        addPrice(components, "TIME", decimal(session, "timePricePerMinute").multiply(BigDecimal.valueOf(60)),
                decimal(session, "taxPercent"), 60);
        addPrice(components, "FLAT", decimal(session, "sessionFee"), decimal(session, "taxPercent"), 1);
        if (components.isEmpty()) components.add(price("FLAT", BigDecimal.ZERO, decimal(session, "taxPercent"), 1));
        return map("country_code", party.countryCode(), "party_id", party.partyId(),
                "id", session.path("tariffId").asText(), "currency", session.path("currency").asText("INR"),
                "elements", List.of(Map.of("price_components", components)),
                "last_updated", instant(session, "startedAt"));
    }

    private Map<String, Object> cdrToken(JsonNode session, OcpiController.Party party) {
        String userId = session.path("userId").asText();
        List<Map<String, Object>> stored = jdbc.query("SELECT raw_json FROM ocpi_tokens WHERE tenant_id = ? AND local_user_id = ? ORDER BY last_updated DESC LIMIT 1",
                (rs, row) -> parseMap(rs.getString(1)), session.path("tenantId").asText(), userId);
        if (!stored.isEmpty()) {
            Map<String, Object> token = stored.getFirst();
            return map("country_code", token.get("country_code"), "party_id", token.get("party_id"), "uid", token.get("uid"),
                    "type", token.get("type"), "contract_id", token.get("contract_id"));
        }
        return map("country_code", party.countryCode(), "party_id", party.partyId(), "uid", userId,
                "type", "APP_USER", "contract_id", userId);
    }

    private List<Map<String, Object>> chargingPeriods(JsonNode session) {
        BigDecimal energy = decimal(session, "energyKwh");
        List<Map<String, Object>> dimensions = new ArrayList<>();
        dimensions.add(Map.of("type", "ENERGY", "volume", energy));
        String startValue = instant(session, "startedAt");
        String endValue = text(session, "stoppedAt");
        if (endValue != null) {
            BigDecimal hours = BigDecimal.valueOf(Duration.between(Instant.parse(startValue), Instant.parse(endValue)).toSeconds())
                    .divide(BigDecimal.valueOf(3600), 6, RoundingMode.HALF_UP);
            dimensions.add(Map.of("type", "TIME", "volume", hours));
        }
        return List.of(Map.of("start_date_time", startValue, "dimensions", dimensions));
    }

    private static void addPrice(List<Map<String, Object>> target, String type, BigDecimal amount, BigDecimal vat, int step) {
        if (amount.signum() > 0) target.add(price(type, amount, vat, step));
    }

    private static Map<String, Object> price(String type, BigDecimal amount, BigDecimal vat, int step) {
        return map("type", type, "price", amount, "vat", vat, "step_size", step);
    }

    private static Map<String, Object> priceFromIncl(BigDecimal incl, BigDecimal vat) {
        BigDecimal divisor = BigDecimal.ONE.add(vat.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
        return Map.of("excl_vat", incl.divide(divisor, 4, RoundingMode.HALF_UP), "incl_vat", incl);
    }

    private static Map<String, Object> priceFromExcl(BigDecimal excl, BigDecimal vat) {
        BigDecimal multiplier = BigDecimal.ONE.add(vat.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
        return Map.of("excl_vat", excl, "incl_vat", excl.multiply(multiplier).setScale(4, RoundingMode.HALF_UP));
    }

    private List<JsonNode> array(String uri) {
        JsonNode result = get(uri);
        if (!result.isArray()) return List.of();
        List<JsonNode> values = new ArrayList<>();
        result.forEach(values::add);
        return values;
    }

    private JsonNode get(String uri) {
        JsonNode result = client.get().uri(uri).retrieve().body(JsonNode.class);
        if (result == null) throw new IllegalStateException("Required TekWatt service returned no data");
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMap(String value) {
        try { return json.readValue(value, LinkedHashMap.class); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Stored OCPI JSON is invalid", exception); }
    }

    private String write(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("OCPI JSON could not be stored", exception); }
    }

    private static boolean isComplete(JsonNode session) {
        String status = session.path("status").asText();
        return "STOPPED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status);
    }

    private static String sessionStatus(String status) {
        return switch (status.toUpperCase()) {
            case "ACTIVE", "STARTED" -> "ACTIVE";
            case "STOPPED", "COMPLETED", "CANCELLED" -> "COMPLETED";
            case "RESERVED" -> "RESERVED";
            default -> "PENDING";
        };
    }

    private static String connectorStandard(String type) {
        return switch (type.toUpperCase()) {
            case "TYPE_1" -> "IEC_62196_T1";
            case "CCS1" -> "IEC_62196_T1_COMBO";
            case "CCS2" -> "IEC_62196_T2_COMBO";
            case "CHADEMO" -> "CHADEMO";
            case "TYPE_2" -> "IEC_62196_T2";
            case "GB_T_AC" -> "GBT_AC";
            case "GB_T_DC" -> "GBT_DC";
            default -> "DOMESTIC_A";
        };
    }

    private static String connectorFormat(String type) {
        return "TYPE_2".equalsIgnoreCase(type) || "GB_T_AC".equalsIgnoreCase(type) ? "SOCKET" : "CABLE";
    }
    private static String powerType(String type) {
        if ("TYPE_1".equalsIgnoreCase(type) || "GB_T_AC".equalsIgnoreCase(type)) return "AC_1_PHASE";
        return "TYPE_2".equalsIgnoreCase(type) ? "AC_3_PHASE" : "DC";
    }
    private static String trim(String value) { return value.replaceAll("/+$", ""); }
    private static BigDecimal decimal(JsonNode node, String field) { return node.path(field).decimalValue(); }
    private static String text(JsonNode node, String field) { return node.hasNonNull(field) && !node.path(field).asText().isBlank() ? node.path(field).asText() : null; }
    private static String instant(JsonNode node, String... fields) {
        for (String field : fields) if (text(node, field) != null) return Instant.parse(node.path(field).asText()).toString();
        return Instant.now().toString();
    }

    static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) if (values[index + 1] != null) result.put((String) values[index], values[index + 1]);
        return result;
    }
}
