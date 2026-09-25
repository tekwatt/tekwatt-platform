package com.tekwatt.ocpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/ocpi/{tenantId}/2.2.1/commands")
@Tag(name = "OCPI 2.2.1 Commands", description = "Asynchronous CPO command receiver backed by OCPP")
public class OcpiCommandsController {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final OcpiDataService data;
    private final RestClient client;
    private final String ocppUrl;

    public OcpiCommandsController(JdbcTemplate jdbc, ObjectMapper json, OcpiDataService data,
            @Value("${tekwatt.ocpi.ocpp-gateway-url}") String ocppUrl) {
        this.jdbc = jdbc;
        this.json = json;
        this.data = data;
        this.ocppUrl = ocppUrl.replaceAll("/+$", "");
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    @PostMapping("/{command}")
    @Operation(summary = "Send an asynchronous command to a charging station")
    public Map<String, Object> command(@PathVariable UUID tenantId, @PathVariable String command,
            @RequestBody JsonNode body, HttpServletRequest httpRequest) {
        String type = command.toUpperCase();
        if (!java.util.Set.of("START_SESSION", "STOP_SESSION", "RESERVE_NOW", "CANCEL_RESERVATION", "UNLOCK_CONNECTOR").contains(type))
            return OcpiProtocol.envelope(Map.of("result", "NOT_SUPPORTED"));
        String responseUrl = required(body, "response_url");
        validateCallback(responseUrl);
        PartnerConnection partner = partnerConnection(tenantId, httpRequest);
        UUID commandId = UUID.randomUUID();
        try {
            String messageId = send(tenantId, type, body, partner);
            jdbc.update("INSERT INTO ocpi_commands (id, tenant_id, command_type, response_url, request_json, result, result_message, created_at) VALUES (?, ?, ?, ?, ?, 'PENDING', ?, ?)",
                    commandId.toString(), tenantId.toString(), type, responseUrl, body.toString(), messageId, Timestamp.from(Instant.now()));
            CompletableFuture.runAsync(() -> deliverResult(commandId, messageId, responseUrl, partner.outboundToken()));
            return OcpiProtocol.envelope(Map.of("result", "ACCEPTED", "timeout", 30));
        } catch (Exception exception) {
            jdbc.update("INSERT INTO ocpi_commands (id, tenant_id, command_type, response_url, request_json, result, result_message, created_at, completed_at) VALUES (?, ?, ?, ?, ?, 'REJECTED', ?, ?, ?)",
                    commandId.toString(), tenantId.toString(), type, responseUrl, body.toString(), safe(exception),
                    Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
            return OcpiProtocol.envelope(Map.of("result", "REJECTED"));
        }
    }

    private String send(UUID tenantId, String type, JsonNode body, PartnerConnection partner) throws Exception {
        JsonNode charger;
        JsonNode connector;
        Map<String, Object> request;
        switch (type) {
            case "START_SESSION" -> {
                charger = data.charger(tenantId, required(body, "location_id"));
                validateEvse(body, charger, false);
                connector = data.connectorFor(tenantId, charger, text(body, "connector_id"));
                validateCommandToken(body.path("token"), partner);
                request = Map.of("stationId", charger.path("stationId").asText(), "ocppVersion", protocol(charger),
                        "connectorId", connector.path("connectorNumber").asInt(), "idToken", required(body.path("token"), "uid"));
                return post("/api/v1/ocpp/commands/remote-start", request);
            }
            case "STOP_SESSION" -> {
                JsonNode session = data.rawSession(tenantId, required(body, "session_id"));
                charger = data.charger(tenantId, session.path("chargerId").asText());
                request = Map.of("stationId", charger.path("stationId").asText(), "ocppVersion", protocol(charger),
                        "transactionId", session.path("transactionId").asText());
                return post("/api/v1/ocpp/commands/remote-stop", request);
            }
            case "RESERVE_NOW" -> {
                charger = data.charger(tenantId, required(body, "location_id"));
                validateEvse(body, charger, false);
                connector = data.connectorFor(tenantId, charger, text(body, "connector_id"));
                validateCommandToken(body.path("token"), partner);
                request = Map.of("stationId", charger.path("stationId").asText(), "ocppVersion", protocol(charger),
                        "connectorId", connector.path("connectorNumber").asInt(),
                        "reservationId", numericReservation(tenantId, body.path("token"), required(body, "reservation_id")),
                        "idToken", required(body.path("token"), "uid"), "expiryDate", OffsetDateTime.parse(required(body, "expiry_date")).toInstant().toString());
                return post("/api/v1/ocpp/commands/reserve-now", request);
            }
            case "CANCEL_RESERVATION" -> {
                String reservationId = required(body, "reservation_id");
                JsonNode reserve = previousReservation(tenantId, reservationId, partner);
                charger = data.charger(tenantId, required(reserve, "location_id"));
                request = Map.of("stationId", charger.path("stationId").asText(), "ocppVersion", protocol(charger),
                        "reservationId", numericReservation(tenantId, reserve.path("token"), reservationId));
                return post("/api/v1/ocpp/commands/cancel-reservation", request);
            }
            case "UNLOCK_CONNECTOR" -> {
                charger = data.charger(tenantId, required(body, "location_id"));
                validateEvse(body, charger, true);
                connector = data.connectorFor(tenantId, charger, required(body, "connector_id"));
                request = Map.of("stationId", charger.path("stationId").asText(), "ocppVersion", protocol(charger),
                        "connectorId", connector.path("connectorNumber").asInt());
                return post("/api/v1/ocpp/commands/unlock-connector", request);
            }
            default -> throw new IllegalArgumentException("Unsupported command");
        }
    }

    private String post(String path, Object request) {
        JsonNode response = client.post().uri(ocppUrl + path).body(request).retrieve().body(JsonNode.class);
        if (response == null || !response.hasNonNull("messageId")) throw new IllegalStateException("OCPP command was not accepted");
        return response.path("messageId").asText();
    }

    private void deliverResult(UUID commandId, String messageId, String responseUrl, String callbackToken) {
        String result = "TIMEOUT";
        String message = null;
        try {
            for (int attempt = 0; attempt < 30; attempt++) {
                JsonNode status = client.get().uri(ocppUrl + "/api/v1/ocpp/commands/" + messageId + "/result").retrieve().body(JsonNode.class);
                if (status != null && !"PENDING".equals(status.path("result").asText())) {
                    result = status.path("result").asText("FAILED");
                    message = status.path("message").isNull() ? null : status.path("message").asText(null);
                    break;
                }
                Thread.sleep(1000);
            }
            String authorization = "Token " + Base64.getEncoder().encodeToString(callbackToken.getBytes(StandardCharsets.UTF_8));
            Map<String, Object> callback = message == null ? Map.of("result", result) : Map.of("result", result,
                    "message", Map.of("language", "en", "text", message));
            client.post().uri(responseUrl).header(HttpHeaders.AUTHORIZATION, authorization)
                    .header("X-Request-ID", UUID.randomUUID().toString()).header("X-Correlation-ID", commandId.toString())
                    .body(callback).retrieve().toBodilessEntity();
            jdbc.update("UPDATE ocpi_commands SET result=?, result_message=?, completed_at=?, callback_sent_at=? WHERE id=?",
                    result, message, Timestamp.from(Instant.now()), Timestamp.from(Instant.now()), commandId.toString());
        } catch (Exception exception) {
            jdbc.update("UPDATE ocpi_commands SET result=?, result_message=?, completed_at=? WHERE id=?",
                    result, safe(exception), Timestamp.from(Instant.now()), commandId.toString());
        }
    }

    private JsonNode previousReservation(UUID tenantId, String reservationId, PartnerConnection partner) throws Exception {
        for (String raw : jdbc.query("SELECT request_json FROM ocpi_commands WHERE tenant_id=? AND command_type='RESERVE_NOW' ORDER BY created_at DESC",
                (rs, row) -> rs.getString(1), tenantId.toString())) {
            JsonNode request = json.readTree(raw);
            JsonNode token = request.path("token");
            if (reservationId.equals(request.path("reservation_id").asText())
                    && tokenBelongsToPartner(token, partner)) return request;
        }
        throw new IllegalArgumentException("Unknown reservation");
    }

    private PartnerConnection partnerConnection(UUID tenantId, HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = header != null && header.startsWith("Token ") ? header.substring(6).trim() : "";
        java.util.List<String> hashes = new java.util.ArrayList<>();
        if (!token.isBlank()) hashes.add(OcpiProtocol.sha256(token));
        try {
            hashes.add(OcpiProtocol.sha256(new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8)));
        } catch (IllegalArgumentException ignored) {
            // Compatibility with OCPI 2.1.1/early 2.2 peers that send a direct token.
        }
        for (String hash : hashes) {
            var values = jdbc.query("SELECT outbound_token, roles_json FROM ocpi_partner_tokens WHERE tenant_id=? AND token_sha256=? AND enabled=TRUE AND connection_status='REGISTERED'",
                    (rs, row) -> new PartnerConnection(rs.getString(1), rs.getString(2)),
                    tenantId.toString(), hash);
            if (!values.isEmpty() && values.getFirst().outboundToken() != null) return values.getFirst();
        }
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Registered OCPI credentials are required");
    }

    private static void validateEvse(JsonNode body, JsonNode charger, boolean required) {
        String evseUid = text(body, "evse_uid");
        if (required && evseUid == null) throw new IllegalArgumentException("evse_uid is required");
        if (evseUid != null && !evseUid.equals(charger.path("id").asText()))
            throw new OcpiController.MissingLocation();
    }

    private void validateCommandToken(JsonNode token, PartnerConnection partner) {
        if (token == null || !token.hasNonNull("uid") || !token.hasNonNull("type")
                || !token.hasNonNull("country_code") || !token.hasNonNull("party_id"))
            throw new IllegalArgumentException("token is missing required OCPI fields");
        if (!tokenBelongsToPartner(token, partner))
            throw new IllegalArgumentException("Command token does not belong to the authenticated OCPI party");
    }

    private boolean tokenBelongsToPartner(JsonNode token, PartnerConnection partner) {
        try {
            JsonNode roles = json.readTree(partner.rolesJson());
            for (JsonNode role : roles) {
                if (role.path("country_code").asText().equalsIgnoreCase(token.path("country_code").asText())
                        && role.path("party_id").asText().equalsIgnoreCase(token.path("party_id").asText())) return true;
            }
            return false;
        } catch (Exception exception) {
            throw new IllegalStateException("Stored OCPI partner roles are invalid", exception);
        }
    }

    private static String protocol(JsonNode charger) { return "OCPP_1_6J".equalsIgnoreCase(charger.path("protocolVersion").asText()) ? "ocpp1.6" : "ocpp2.0.1"; }
    private static int numericReservation(UUID tenantId, JsonNode token, String value) {
        String hash = OcpiProtocol.sha256(tenantId + "|" + token.path("country_code").asText().toUpperCase()
                + "|" + token.path("party_id").asText().toUpperCase() + "|" + value);
        int result = (int) (Long.parseUnsignedLong(hash.substring(0, 8), 16) & 0x7fffffffL);
        return result == 0 ? 1 : result;
    }
    private static String required(JsonNode node, String field) { String value = text(node, field); if (value == null) throw new IllegalArgumentException(field + " is required"); return value; }
    private static String text(JsonNode node, String field) { return node != null && node.hasNonNull(field) && !node.path(field).asText().isBlank() ? node.path(field).asText() : null; }
    private static String safe(Exception exception) { String value = exception.getMessage(); return value == null || value.isBlank() ? "Command failed" : value.substring(0, Math.min(500, value.length())); }

    private static void validateCallback(String value) {
        URI uri = URI.create(value);
        if (!("https".equalsIgnoreCase(uri.getScheme()) || (("http".equalsIgnoreCase(uri.getScheme())) && ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost())))))
            throw new IllegalArgumentException("response_url must use HTTPS");
    }

    private record PartnerConnection(String outboundToken, String rolesJson) {}
}
