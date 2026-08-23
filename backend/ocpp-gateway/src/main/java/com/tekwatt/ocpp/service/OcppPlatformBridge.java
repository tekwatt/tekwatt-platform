package com.tekwatt.ocpp.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Applies accepted OCPP events to the services that own operational platform state. */
@Service
public class OcppPlatformBridge {
    private static final Logger log = LoggerFactory.getLogger(OcppPlatformBridge.class);
    private final RestClient chargers;
    private final RestClient connectors;
    private final RestClient users;
    private final RestClient sessions;
    private final RestClient telemetry;

    public OcppPlatformBridge(
            RestClient.Builder builder,
            @Value("${tekwatt.services.charger:http://localhost:8083}") String chargerUrl,
            @Value("${tekwatt.services.connector:http://localhost:8087}") String connectorUrl,
            @Value("${tekwatt.services.user:http://localhost:8082}") String userUrl,
            @Value("${tekwatt.services.session:http://localhost:8084}") String sessionUrl,
            @Value("${tekwatt.services.telemetry:http://localhost:8095}") String telemetryUrl) {
        chargers = builder.clone().baseUrl(chargerUrl).build();
        connectors = builder.clone().baseUrl(connectorUrl).build();
        users = builder.clone().baseUrl(userUrl).build();
        sessions = builder.clone().baseUrl(sessionUrl).build();
        telemetry = builder.clone().baseUrl(telemetryUrl).build();
    }

    public boolean boot(String stationId) {
        try {
            ChargerRef charger = charger(stationId);
            chargers.patch().uri("/api/v1/chargers/{id}/status", charger.id())
                    .body(Map.of("status", "AVAILABLE")).retrieve().toBodilessEntity();
            heartbeat(charger);
            return true;
        } catch (HttpClientErrorException.NotFound exception) {
            log.warn("Rejected BootNotification from unregistered station {}", stationId);
            return false;
        }
    }

    public void heartbeat(String stationId) { heartbeat(charger(stationId)); }

    public String authorize(JsonNode payload) {
        String token = text(payload.path("idToken"), "idToken").orElseGet(() -> text(payload, "idTag").orElse(""));
        if (token.isBlank()) return "Invalid";
        try {
            CardRef card = users.get().uri("/api/v1/users/directory/rfid-cards/by-uid/{uid}", token)
                    .retrieve().body(CardRef.class);
            return card != null && card.userId() != null && "ACTIVE".equalsIgnoreCase(card.status()) ? "Accepted" : "Blocked";
        } catch (HttpClientErrorException.NotFound exception) {
            return "Invalid";
        }
    }

    public void status(String stationId, String protocol, JsonNode payload) {
        StationContext context = context(stationId, connectorNumber(protocol, payload));
        String connectorStatus = connectorStatus(text(payload, "status").orElse("Unavailable"));
        connectors.patch().uri("/api/v1/connectors/{id}/status", context.connector().id())
                .body(Map.of("status", connectorStatus)).retrieve().toBodilessEntity();
        String chargerStatus = switch (connectorStatus) {
            case "CHARGING" -> "CHARGING";
            case "FAULTED" -> "FAULTED";
            case "UNAVAILABLE" -> "UNAVAILABLE";
            default -> "AVAILABLE";
        };
        chargers.patch().uri("/api/v1/chargers/{id}/status", context.charger().id())
                .body(Map.of("status", chargerStatus)).retrieve().toBodilessEntity();
    }

    public int startLegacy(String stationId, JsonNode payload) {
        String transactionId = Integer.toString(Math.abs(UUID.randomUUID().hashCode()));
        start(stationId, payload.path("connectorId").asInt(1), transactionId,
                text(payload, "idTag").orElse(""), decimal(payload, "meterStart").orElse(BigDecimal.ZERO));
        ingestTelemetry(stationId, payload.path("connectorId").asInt(1), transactionId, payload);
        return Integer.parseInt(transactionId);
    }

    public void transactionEvent(String stationId, JsonNode payload) {
        String eventType = text(payload, "eventType").orElse("");
        String transactionId = text(payload.path("transactionInfo"), "transactionId")
                .orElseThrow(() -> new IllegalArgumentException("TransactionEvent transactionId is required"));
        int connectorNumber = connectorNumber("ocpp2.0.1", payload);
        Optional<BigDecimal> meterWh = energyWh(payload);
        if ("Started".equalsIgnoreCase(eventType)) {
            String token = text(payload.path("idToken"), "idToken").orElse("");
            start(stationId, connectorNumber, transactionId, token, meterWh.orElse(BigDecimal.ZERO));
        } else if ("Updated".equalsIgnoreCase(eventType)) {
            meterWh.ifPresent(reading -> applyMeter(transactionId, reading, timestamp(payload)));
        } else if ("Ended".equalsIgnoreCase(eventType)) {
            SessionRef session = session(transactionId);
            stop(transactionId, meterWh.orElseGet(() -> currentMeter(session)));
        } else {
            throw new IllegalArgumentException("Unsupported TransactionEvent eventType: " + eventType);
        }
        ingestTelemetry(stationId, connectorNumber, transactionId, payload);
    }

    public void meterValues(String stationId, String protocol, JsonNode payload) {
        String transactionId = text(payload, "transactionId").orElse("");
        int number = connectorNumber(protocol, payload);
        Optional<BigDecimal> meterWh = energyWh(payload);
        if (!transactionId.isBlank() && meterWh.isPresent()) applyMeter(transactionId, meterWh.get(), timestamp(payload));
        ingestTelemetry(stationId, number, transactionId, payload);
    }

    public void stopLegacy(String stationId, JsonNode payload) {
        String transactionId = text(payload, "transactionId")
                .orElseThrow(() -> new IllegalArgumentException("StopTransaction transactionId is required"));
        BigDecimal meterWh = decimal(payload, "meterStop").orElseGet(() -> energyWh(payload).orElse(BigDecimal.ZERO));
        stop(transactionId, meterWh);
        ingestTelemetry(stationId, payload.path("connectorId").asInt(1), transactionId, payload);
    }

    private void start(String stationId, int connectorNumber, String transactionId, String token, BigDecimal meterWh) {
        StationContext context = context(stationId, connectorNumber);
        CardRef card = card(token);
        if (!context.charger().tenantId().equals(card.tenantId())) throw new IllegalArgumentException("RFID card belongs to another workspace");
        sessions.post().uri("/api/v1/charging-sessions").body(Map.of(
                "tenantId", context.charger().tenantId(),
                "userId", card.userId(),
                "chargerId", context.charger().id(),
                "connectorId", context.connector().id(),
                "transactionId", transactionId,
                "meterStartWh", meterWh)).retrieve().toBodilessEntity();
        connectors.patch().uri("/api/v1/connectors/{id}/status", context.connector().id())
                .body(Map.of("status", "CHARGING")).retrieve().toBodilessEntity();
        chargers.patch().uri("/api/v1/chargers/{id}/status", context.charger().id())
                .body(Map.of("status", "CHARGING")).retrieve().toBodilessEntity();
    }

    private void applyMeter(String transactionId, BigDecimal meterWh, Instant sampledAt) {
        SessionRef session = session(transactionId);
        sessions.post().uri("/api/v1/charging-sessions/{id}/meter-values", session.id())
                .body(Map.of("meterWh", meterWh, "recordedAt", sampledAt.toString())).retrieve().toBodilessEntity();
    }

    private void stop(String transactionId, BigDecimal meterWh) {
        SessionRef session = session(transactionId);
        sessions.post().uri("/api/v1/charging-sessions/{id}/stop", session.id())
                .body(Map.of("meterStopWh", meterWh, "status", "COMPLETED")).retrieve().toBodilessEntity();
        connectors.patch().uri("/api/v1/connectors/{id}/status", session.connectorId())
                .body(Map.of("status", "AVAILABLE")).retrieve().toBodilessEntity();
        chargers.patch().uri("/api/v1/chargers/{id}/status", session.chargerId())
                .body(Map.of("status", "AVAILABLE")).retrieve().toBodilessEntity();
    }

    private BigDecimal currentMeter(SessionRef session) {
        return session.meterStopWh() == null ? session.meterStartWh() : session.meterStopWh();
    }

    private void ingestTelemetry(String stationId, int connectorNumber, String transactionId, JsonNode payload) {
        List<JsonNode> meterValues = meterValues(payload);
        if (meterValues.isEmpty()) return;
        StationContext context = context(stationId, connectorNumber);
        UUID sessionId = transactionId.isBlank() ? null : findSession(transactionId).map(SessionRef::id).orElse(null);
        List<Map<String, Object>> readings = new ArrayList<>();
        for (JsonNode meterValue : meterValues) {
            Instant sampledAt = parseInstant(text(meterValue, "timestamp").orElse(null));
            JsonNode samples = meterValue.path("sampledValue");
            if (!samples.isArray()) continue;
            for (JsonNode sample : samples) {
                BigDecimal value = decimal(sample, "value").orElse(null);
                if (value == null) continue;
                String measurand = text(sample, "measurand").orElse("Energy.Active.Import.Register");
                String unit = text(sample.path("unitOfMeasure"), "unit").orElseGet(() -> text(sample, "unit").orElse("Wh"));
                var reading = new java.util.LinkedHashMap<String, Object>();
                reading.put("tenantId", context.charger().tenantId());
                reading.put("chargerId", context.charger().id());
                reading.put("connectorId", context.connector().id());
                if (sessionId != null) reading.put("sessionId", sessionId);
                if (!transactionId.isBlank()) reading.put("transactionId", transactionId);
                reading.put("measurand", measurand);
                reading.put("value", value);
                reading.put("unit", unit);
                text(sample, "phase").ifPresent(valueText -> reading.put("phase", valueText));
                text(sample, "context").ifPresent(valueText -> reading.put("context", valueText));
                text(sample, "location").ifPresent(valueText -> reading.put("location", valueText));
                reading.put("sampledAt", sampledAt.toString());
                readings.add(reading);
            }
        }
        if (!readings.isEmpty()) telemetry.post().uri("/api/v1/telemetry/readings/batch")
                .body(Map.of("readings", readings)).retrieve().toBodilessEntity();
    }

    private ChargerRef charger(String stationId) {
        ChargerRef result = chargers.get().uri("/api/v1/chargers/by-station/{stationId}", stationId).retrieve().body(ChargerRef.class);
        if (result == null) throw new IllegalStateException("Charger lookup returned no data");
        return result;
    }

    private void heartbeat(ChargerRef charger) {
        chargers.post().uri("/api/v1/chargers/{id}/heartbeat", charger.id()).retrieve().toBodilessEntity();
    }

    private StationContext context(String stationId, int connectorNumber) {
        ChargerRef charger = charger(stationId);
        ConnectorRef[] configured = connectors.get().uri(uri -> uri.path("/api/v1/connectors")
                .queryParam("chargerId", charger.id()).build()).retrieve().body(ConnectorRef[].class);
        if (configured == null) throw new IllegalStateException("Connector lookup returned no data");
        for (ConnectorRef connector : configured) if (connector.connectorNumber() == connectorNumber) return new StationContext(charger, connector);
        throw new IllegalArgumentException("Connector " + connectorNumber + " is not configured for station " + stationId);
    }

    private CardRef card(String token) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("An assigned RFID idToken is required to start charging");
        CardRef card = users.get().uri("/api/v1/users/directory/rfid-cards/by-uid/{uid}", token).retrieve().body(CardRef.class);
        if (card == null || card.userId() == null || !"ACTIVE".equalsIgnoreCase(card.status())) throw new IllegalArgumentException("RFID token is not active or assigned");
        return card;
    }

    private SessionRef session(String transactionId) {
        return findSession(transactionId).orElseThrow(() -> new IllegalArgumentException("Unknown transaction: " + transactionId));
    }

    private Optional<SessionRef> findSession(String transactionId) {
        try {
            return Optional.ofNullable(sessions.get().uri("/api/v1/charging-sessions/by-transaction/{transactionId}", transactionId)
                    .retrieve().body(SessionRef.class));
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        }
    }

    private int connectorNumber(String protocol, JsonNode payload) {
        if ("ocpp2.0.1".equals(protocol)) {
            JsonNode evse = payload.path("evse");
            if (evse.has("connectorId")) return evse.path("connectorId").asInt(1);
            if (evse.has("id")) return evse.path("id").asInt(1);
        }
        return payload.path("connectorId").asInt(1);
    }

    private String connectorStatus(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "available" -> "AVAILABLE";
            case "preparing" -> "PREPARING";
            case "charging" -> "CHARGING";
            case "suspendedev", "suspendedevse" -> "SUSPENDED";
            case "finishing" -> "FINISHING";
            case "reserved" -> "RESERVED";
            case "faulted" -> "FAULTED";
            default -> "UNAVAILABLE";
        };
    }

    private Optional<BigDecimal> energyWh(JsonNode payload) {
        for (JsonNode meterValue : meterValues(payload)) {
            JsonNode samples = meterValue.path("sampledValue");
            if (!samples.isArray()) continue;
            for (JsonNode sample : samples) {
                String measurand = text(sample, "measurand").orElse("Energy.Active.Import.Register");
                if (!measurand.toLowerCase(Locale.ROOT).contains("energy")) continue;
                Optional<BigDecimal> reading = decimal(sample, "value");
                if (reading.isEmpty()) continue;
                String unit = text(sample.path("unitOfMeasure"), "unit").orElseGet(() -> text(sample, "unit").orElse("Wh"));
                return Optional.of("kWh".equalsIgnoreCase(unit) ? reading.get().multiply(BigDecimal.valueOf(1000)) : reading.get());
            }
        }
        return Optional.empty();
    }

    private List<JsonNode> meterValues(JsonNode payload) {
        JsonNode values = payload.path("meterValue");
        if (!values.isArray()) return List.of();
        List<JsonNode> result = new ArrayList<>();
        values.forEach(result::add);
        return result;
    }

    private Instant timestamp(JsonNode payload) {
        String direct = text(payload, "timestamp").orElse(null);
        if (direct != null) return parseInstant(direct);
        List<JsonNode> values = meterValues(payload);
        return values.isEmpty() ? Instant.now() : parseInstant(text(values.get(values.size() - 1), "timestamp").orElse(null));
    }

    private Instant parseInstant(String value) {
        try { return value == null || value.isBlank() ? Instant.now() : Instant.parse(value); }
        catch (RuntimeException exception) { return Instant.now(); }
    }

    private Optional<String> text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? Optional.empty() : Optional.of(value.asText());
    }

    private Optional<BigDecimal> decimal(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) return Optional.empty();
        try { return Optional.of(new BigDecimal(value.asText())); }
        catch (NumberFormatException exception) { return Optional.empty(); }
    }

    private record ChargerRef(UUID id, UUID tenantId, String stationId) {}
    private record ConnectorRef(UUID id, UUID tenantId, UUID chargerId, int connectorNumber, String status) {}
    private record CardRef(UUID tenantId, UUID userId, String status) {}
    private record SessionRef(UUID id, UUID tenantId, UUID userId, UUID chargerId, UUID connectorId,
                              String transactionId, String status, BigDecimal meterStartWh, BigDecimal meterStopWh) {}
    private record StationContext(ChargerRef charger, ConnectorRef connector) {}
}
