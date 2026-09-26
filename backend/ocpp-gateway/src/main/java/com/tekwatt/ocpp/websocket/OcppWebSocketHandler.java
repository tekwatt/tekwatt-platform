package com.tekwatt.ocpp.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tekwatt.ocpp.service.ConnectionRegistry;
import com.tekwatt.ocpp.service.FirmwareStatusNotifier;
import com.tekwatt.ocpp.service.OcppAuditService;
import com.tekwatt.ocpp.service.OcppPlatformBridge;
import com.tekwatt.ocpp.service.OcppCommandTracker;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class OcppWebSocketHandler extends TextWebSocketHandler implements SubProtocolCapable {
    private static final Logger log = LoggerFactory.getLogger(OcppWebSocketHandler.class);

    private final ObjectMapper json;
    private final ConnectionRegistry registry;
    private final OcppAuditService audit;
    private final FirmwareStatusNotifier firmware;
    private final OcppPlatformBridge platform;
    private final OcppCommandTracker commands;

    public OcppWebSocketHandler(ObjectMapper json, ConnectionRegistry registry, OcppAuditService audit,
                                FirmwareStatusNotifier firmware, OcppPlatformBridge platform, OcppCommandTracker commands) {
        this.json = json;
        this.registry = registry;
        this.audit = audit;
        this.firmware = firmware;
        this.platform = platform;
        this.commands = commands;
    }

    @Override public List<String> getSubProtocols() {
        // Some OCPP 2.0.1 simulators still advertise the early "ocpp2.0" token.
        // Accept it as a wire-level alias while exposing 2.0.1 throughout TekWatt.
        return List.of("ocpp2.0.1", "ocpp2.0", "ocpp1.6");
    }

    @Override public void afterConnectionEstablished(WebSocketSession session) {
        String stationId = station(session);
        registry.add(stationId, session);
        log.info("OCPP WebSocket connected: station={}, protocol={}, remote={}, session={}",
                stationId, protocol(session), remoteAddress(session), session.getId());
    }

    @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String stationId = station(session);
        JsonNode root;
        try {
            root = json.readTree(message.getPayload());
        } catch (Exception exception) {
            error(session, stationId, "unknown", "FormationViolation", "Message is not valid JSON");
            return;
        }
        if (!root.isArray() || root.size() < 2) {
            error(session, stationId, "unknown", "FormationViolation", "Expected an OCPP array frame");
            return;
        }
        int messageType = root.get(0).asInt();
        String uniqueId = root.get(1).asText("unknown");
        if (messageType == 3 || messageType == 4) {
            audit.record(stationId, "IN", messageType, uniqueId, null, message.getPayload());
            commands.complete(uniqueId, messageType, root);
            return;
        }
        if (messageType != 2 || root.size() < 3) {
            error(session, stationId, uniqueId, "FormationViolation", "Expected an OCPP CALL, CALLRESULT or CALLERROR frame");
            return;
        }
        String action = root.get(2).asText();
        JsonNode payload = root.size() > 3 ? root.get(3) : json.createObjectNode();
        audit.record(stationId, "IN", 2, uniqueId, action, message.getPayload());
        try {
            ObjectNode response = handle(stationId, session.getAcceptedProtocol(), action, payload);
            ArrayNode frame = json.createArrayNode().add(3).add(uniqueId).add(response);
            send(session, stationId, 3, uniqueId, action, frame);
        } catch (Exception exception) {
            error(session, stationId, uniqueId, "InternalError", safeMessage(exception));
        }
    }

    private ObjectNode handle(String stationId, String protocol, String action, JsonNode payload) {
        boolean v201 = "ocpp2.0.1".equals(protocol) || "ocpp2.0".equals(protocol);
        ObjectNode response = json.createObjectNode();
        switch (action) {
            case "BootNotification" -> {
                boolean accepted = platform.boot(stationId);
                response.put("status", accepted ? "Accepted" : "Rejected")
                        .put("currentTime", Instant.now().toString()).put("interval", 60);
            }
            case "Heartbeat" -> {
                platform.heartbeat(stationId);
                response.put("currentTime", Instant.now().toString());
            }
            case "Authorize" -> {
                String field = v201 ? "idTokenInfo" : "idTagInfo";
                response.set(field, json.valueToTree(Map.of("status", platform.authorize(payload))));
            }
            case "StartTransaction" -> {
                if (v201) throw new IllegalArgumentException("Use TransactionEvent with OCPP 2.0.1");
                int transactionId = platform.startLegacy(stationId, payload);
                response.put("transactionId", transactionId)
                        .set("idTagInfo", json.valueToTree(Map.of("status", "Accepted")));
            }
            case "TransactionEvent" -> {
                if (!v201) throw new IllegalArgumentException("TransactionEvent requires OCPP 2.0.1");
                platform.transactionEvent(stationId, payload);
            }
            case "StatusNotification" -> platform.status(stationId, protocol, payload);
            case "MeterValues" -> platform.meterValues(stationId, protocol, payload);
            case "StopTransaction" -> {
                if (v201) throw new IllegalArgumentException("Use TransactionEvent with OCPP 2.0.1");
                platform.stopLegacy(stationId, payload);
            }
            case "FirmwareStatusNotification" -> firmware.notify(stationId, payload);
            case "NotifyEvent" -> { /* The audited event is exposed immediately to the web OCPP log. */ }
            default -> throw new IllegalArgumentException("Unsupported action: " + action);
        }
        return response;
    }

    private void send(WebSocketSession session, String stationId, int type, String uniqueId,
                      String action, JsonNode frame) throws Exception {
        String text = json.writeValueAsString(frame);
        session.sendMessage(new TextMessage(text));
        audit.record(stationId, "OUT", type, uniqueId, action, text);
    }

    private void error(WebSocketSession session, String stationId, String uniqueId,
                       String code, String description) throws Exception {
        ArrayNode frame = json.createArrayNode().add(4).add(uniqueId).add(code)
                .add(description == null ? "" : description);
        frame.addObject();
        send(session, stationId, 4, uniqueId, null, frame);
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "The platform could not apply this OCPP event" : message;
    }

    @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String stationId = station(session);
        registry.remove(stationId, session);
        log.info("OCPP WebSocket disconnected: station={}, protocol={}, remote={}, session={}, code={}, reason={}",
                stationId, protocol(session), remoteAddress(session), session.getId(),
                status.getCode(), status.getReason() == null || status.getReason().isBlank() ? "-" : status.getReason());
    }

    @Override public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("OCPP WebSocket transport error: station={}, protocol={}, remote={}, session={}, error={}",
                station(session), protocol(session), remoteAddress(session), session.getId(),
                exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage(), exception);
    }

    private String protocol(WebSocketSession session) {
        String value = session.getAcceptedProtocol();
        return value == null || value.isBlank() ? "none" : value;
    }

    private String remoteAddress(WebSocketSession session) {
        return session.getRemoteAddress() == null ? "unknown" : session.getRemoteAddress().toString();
    }

    private String station(WebSocketSession session) { return (String) session.getAttributes().get("stationId"); }
}
