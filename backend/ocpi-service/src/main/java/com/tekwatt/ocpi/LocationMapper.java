package com.tekwatt.ocpi;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LocationMapper {
    public Map<String, Object> map(JsonNode charger, JsonNode connectors, OcpiController.Party party) {
        if (blank(charger, "address") || blank(charger, "city") || charger.path("latitude").isNull()
                || charger.path("longitude").isNull() || !charger.hasNonNull("latitude")
                || !charger.hasNonNull("longitude")) return null;
        if (charger.path("address").asText().length() > 45 || charger.path("city").asText().length() > 45) return null;
        if (!"ACTIVE".equalsIgnoreCase(charger.path("stationStatus").asText())) return null;
        BigDecimal latitude = charger.path("latitude").decimalValue();
        BigDecimal longitude = charger.path("longitude").decimalValue();
        if (latitude.abs().compareTo(BigDecimal.valueOf(90)) > 0 || longitude.abs().compareTo(BigDecimal.valueOf(180)) > 0) return null;
        List<Map<String, Object>> ocpiConnectors = new ArrayList<>();
        Instant updated = timestamp(charger.path("updatedAt"));
        if (connectors != null && connectors.isArray()) {
            for (JsonNode connector : connectors) {
                Map<String, Object> mapped = connector(connector);
                if (mapped != null) {
                    ocpiConnectors.add(mapped);
                    Instant connectorUpdated = timestamp(connector.path("updatedAt"));
                    if (connectorUpdated.isAfter(updated)) updated = connectorUpdated;
                }
            }
        }
        if (ocpiConnectors.isEmpty()) return null;
        String evseStatus = switch (charger.path("status").asText()) {
            case "OFFLINE" -> "UNKNOWN";
            case "UNAVAILABLE", "REGISTERED" -> "INOPERATIVE";
            case "FAULTED" -> "OUTOFORDER";
            default -> {
                if (ocpiConnectors.stream().anyMatch(c -> "AVAILABLE".equals(c.get("_status")))) yield "AVAILABLE";
                if (ocpiConnectors.stream().anyMatch(c -> "CHARGING".equals(c.get("_status")))) yield "CHARGING";
                if (ocpiConnectors.stream().anyMatch(c -> "RESERVED".equals(c.get("_status")))) yield "RESERVED";
                if (ocpiConnectors.stream().anyMatch(c -> "FAULTED".equals(c.get("_status")))) yield "OUTOFORDER";
                yield "INOPERATIVE";
            }
        };
        ocpiConnectors.forEach(c -> c.remove("_status"));
        String locationId = charger.path("id").asText();
        Map<String, Object> evse = new LinkedHashMap<>();
        evse.put("uid", locationId);
        evse.put("status", evseStatus);
        evse.put("connectors", ocpiConnectors);
        evse.put("last_updated", updated.toString());
        Map<String, Object> location = new LinkedHashMap<>();
        location.put("country_code", party.countryCode());
        location.put("party_id", party.partyId());
        location.put("id", locationId);
        location.put("publish", true);
        if (!blank(charger, "stationName")) location.put("name", charger.path("stationName").asText());
        location.put("address", charger.path("address").asText());
        location.put("city", charger.path("city").asText());
        if (!blank(charger, "state")) location.put("state", charger.path("state").asText());
        location.put("country", party.country());
        location.put("coordinates", Map.of("latitude", latitude.setScale(6, RoundingMode.HALF_UP).toPlainString(),
                "longitude", longitude.setScale(6, RoundingMode.HALF_UP).toPlainString()));
        location.put("evses", List.of(evse));
        location.put("time_zone", party.timeZone());
        location.put("last_updated", updated.toString());
        return location;
    }

    private Map<String, Object> connector(JsonNode node) {
        String standard = switch (node.path("type").asText()) {
            case "TYPE_1" -> "IEC_62196_T1";
            case "TYPE_2" -> "IEC_62196_T2";
            case "CCS1" -> "IEC_62196_T1_COMBO";
            case "CCS2" -> "IEC_62196_T2_COMBO";
            case "CHADEMO" -> "CHADEMO";
            case "GB_T_AC" -> "GBT_AC";
            case "GB_T_DC" -> "GBT_DC";
            default -> null;
        };
        if (standard == null || !node.hasNonNull("maxVoltage") || !node.hasNonNull("maxCurrent")
                || node.path("maxVoltage").asInt() <= 0 || node.path("maxCurrent").asInt() <= 0) return null;
        Map<String, Object> connector = new LinkedHashMap<>();
        connector.put("id", node.path("connectorNumber").asText());
        connector.put("standard", standard);
        connector.put("format", switch (node.path("type").asText()) {
            case "TYPE_2", "GB_T_AC" -> "SOCKET";
            default -> "CABLE";
        });
        connector.put("power_type", switch (node.path("type").asText()) {
            case "TYPE_1", "GB_T_AC" -> "AC_1_PHASE";
            case "TYPE_2" -> "AC_3_PHASE";
            default -> "DC";
        });
        connector.put("max_voltage", node.path("maxVoltage").asInt());
        connector.put("max_amperage", node.path("maxCurrent").asInt());
        connector.put("last_updated", timestamp(node.path("updatedAt")).toString());
        connector.put("_status", node.path("status").asText());
        return connector;
    }

    private boolean blank(JsonNode node, String field) {
        return node.path(field).asText("").isBlank();
    }

    private Instant timestamp(JsonNode node) {
        try { return Instant.parse(node.asText()); }
        catch (Exception ignored) { return Instant.EPOCH; }
    }
}
