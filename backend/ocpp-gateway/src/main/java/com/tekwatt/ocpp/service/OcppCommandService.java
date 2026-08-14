package com.tekwatt.ocpp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tekwatt.ocpp.dto.FirmwareCommandRequest;
import com.tekwatt.ocpp.dto.RemoteStartRequest;
import com.tekwatt.ocpp.dto.RemoteStopRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OcppCommandService {
    private final ConnectionRegistry registry;
    private final OcppAuditService audit;
    private final ObjectMapper json;

    public OcppCommandService(ConnectionRegistry registry, OcppAuditService audit, ObjectMapper json) {
        this.registry = registry;
        this.audit = audit;
        this.json = json;
    }

    public String remoteStart(RemoteStartRequest request) {
        String protocol = requireProtocol(request.stationId(), request.ocppVersion());
        ObjectNode payload = json.createObjectNode();
        if ("ocpp1.6".equals(protocol)) {
            payload.put("connectorId", request.connectorId()).put("idTag", request.idToken());
            return send(request.stationId(), "RemoteStartTransaction", payload);
        }
        payload.put("remoteStartId", Math.abs(UUID.randomUUID().hashCode()));
        payload.put("evseId", request.connectorId());
        payload.putObject("idToken").put("idToken", request.idToken()).put("type", "Central");
        return send(request.stationId(), "RequestStartTransaction", payload);
    }

    public String remoteStop(RemoteStopRequest request) {
        String protocol = requireProtocol(request.stationId(), request.ocppVersion());
        ObjectNode payload = json.createObjectNode().put("transactionId", request.transactionId());
        return send(request.stationId(), "ocpp1.6".equals(protocol) ? "RemoteStopTransaction" : "RequestStopTransaction", payload);
    }

    public String firmware(FirmwareCommandRequest request) {
        String protocol = requireProtocol(request.stationId(), request.ocppVersion());
        ObjectNode payload = json.createObjectNode();
        if ("ocpp1.6".equals(protocol)) {
            payload.put("location", request.location()).put("retrieveDate", request.retrieveDate().toString()).put("retries", 3).put("retryInterval", 60);
        } else {
            payload.put("requestId", Math.abs(request.jobId().hashCode()));
            ObjectNode firmware = payload.putObject("firmware");
            firmware.put("location", request.location()).put("retrieveDateTime", request.retrieveDate().toString());
            if (request.signature() != null) firmware.put("signature", request.signature());
        }
        return send(request.stationId(), "UpdateFirmware", payload);
    }

    private String requireProtocol(String stationId, String requested) {
        String protocol;
        try { protocol = registry.protocol(stationId); }
        catch (IllegalStateException exception) { throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage()); }
        if (!requested.equals(protocol)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Connected charger negotiated " + protocol);
        return protocol;
    }

    private String send(String stationId, String action, ObjectNode payload) {
        String uniqueId = UUID.randomUUID().toString();
        ArrayNode frame = json.createArrayNode().add(2).add(uniqueId).add(action).add(payload);
        String text = frame.toString();
        try {
            registry.send(stationId, text);
            audit.record(stationId, "OUT", 2, uniqueId, action, text);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not send OCPP command");
        }
        return uniqueId;
    }
}
