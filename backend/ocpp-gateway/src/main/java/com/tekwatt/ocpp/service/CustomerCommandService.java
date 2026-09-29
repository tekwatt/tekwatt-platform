package com.tekwatt.ocpp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tekwatt.ocpp.dto.RemoteStartRequest;
import com.tekwatt.ocpp.dto.RemoteStopRequest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Customer OCPP commands resolve charger, RFID and session ownership on the server. */
@Service
public class CustomerCommandService {
    private final OcppCommandService commands;
    private final ConnectionRegistry connections;
    private final OcppCommandTracker tracker;
    private final RestClient users;
    private final RestClient chargers;
    private final RestClient connectors;
    private final RestClient sessions;

    public CustomerCommandService(OcppCommandService commands, ConnectionRegistry connections, OcppCommandTracker tracker,
            RestClient.Builder builder,
            @Value("${tekwatt.services.user:http://localhost:8082}") String userUrl,
            @Value("${tekwatt.services.charger:http://localhost:8083}") String chargerUrl,
            @Value("${tekwatt.services.connector:http://localhost:8087}") String connectorUrl,
            @Value("${tekwatt.services.session:http://localhost:8084}") String sessionUrl) {
        this.commands = commands;
        this.connections = connections;
        this.tracker = tracker;
        users = builder.clone().baseUrl(userUrl).build();
        chargers = builder.clone().baseUrl(chargerUrl).build();
        connectors = builder.clone().baseUrl(connectorUrl).build();
        sessions = builder.clone().baseUrl(sessionUrl).build();
    }

    public String start(String authorization, UUID chargerId, UUID connectorId) {
        JsonNode customer = customer(authorization);
        JsonNode charger = charger(chargerId, customer);
        JsonNode connector = fetch(connectors, "/api/v1/connectors/{id}", connectorId);
        if (!chargerId.toString().equals(connector.path("chargerId").asText())
                || !customer.path("tenantId").asText().equals(connector.path("tenantId").asText()))
            throw forbidden("This connector is not assigned to your charger");
        if (!"AVAILABLE".equalsIgnoreCase(connector.path("status").asText()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This connector is not available");
        int connectorNumber = connector.path("connectorNumber").asInt(0);
        if (connectorNumber < 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Connector number is unavailable");
        String cardUid = activeCard(customer);
        String stationId = charger.path("stationId").asText();
        String messageId = commands.remoteStart(new RemoteStartRequest(stationId, protocol(stationId), connectorNumber, cardUid));
        tracker.claimForCustomer(messageId, UUID.fromString(customer.path("id").asText()));
        return messageId;
    }

    public String stop(String authorization, UUID sessionId) {
        JsonNode customer = customer(authorization);
        JsonNode session = fetch(sessions, "/api/v1/charging-sessions/{id}", sessionId);
        if (!customer.path("id").asText().equals(session.path("userId").asText())
                || !customer.path("tenantId").asText().equals(session.path("tenantId").asText()))
            throw forbidden("This charging session does not belong to your account");
        if (!"ACTIVE".equalsIgnoreCase(session.path("status").asText()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This charging session is not active");
        UUID chargerId = UUID.fromString(session.path("chargerId").asText());
        JsonNode charger = charger(chargerId, customer);
        String stationId = charger.path("stationId").asText();
        String messageId = commands.remoteStop(new RemoteStopRequest(stationId, protocol(stationId), session.path("transactionId").asText()));
        tracker.claimForCustomer(messageId, UUID.fromString(customer.path("id").asText()));
        return messageId;
    }

    public OcppCommandTracker.Result result(String authorization, String messageId) {
        JsonNode customer = customer(authorization);
        return tracker.resultForCustomer(messageId, UUID.fromString(customer.path("id").asText()));
    }

    private JsonNode customer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again");
        JsonNode customer;
        try {
            customer = users.get().uri("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve().body(JsonNode.class);
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 401) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again", error);
            if (error.getStatusCode().value() == 403 || error.getStatusCode().value() == 404)
                throw forbidden("An active customer account is required");
            throw unavailable(error);
        } catch (RestClientException error) { throw unavailable(error); }
        if (customer == null || !"ACTIVE".equalsIgnoreCase(customer.path("status").asText())
                || customer.path("id").asText().isBlank() || customer.path("tenantId").asText().isBlank())
            throw forbidden("An active customer account is required");
        return customer;
    }

    private JsonNode charger(UUID chargerId, JsonNode customer) {
        boolean assigned = false;
        for (JsonNode id : customer.path("assignedChargerIds")) if (chargerId.toString().equals(id.asText())) assigned = true;
        if (!assigned) throw forbidden("This charger is not assigned to your account");
        JsonNode charger = fetch(chargers, "/api/v1/chargers/{id}", chargerId);
        if (!customer.path("tenantId").asText().equals(charger.path("tenantId").asText()))
            throw forbidden("This charger belongs to another workspace");
        if (charger.path("stationId").asText().isBlank())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Charger has no station identifier");
        return charger;
    }

    private String activeCard(JsonNode customer) {
        UUID tenantId = UUID.fromString(customer.path("tenantId").asText());
        JsonNode cards = fetch(users, "/api/v1/users/directory/rfid-cards?tenantId={id}", tenantId);
        for (JsonNode card : cards) {
            if (customer.path("id").asText().equals(card.path("userId").asText())
                    && tenantId.toString().equals(card.path("tenantId").asText())
                    && "ACTIVE".equalsIgnoreCase(card.path("status").asText())
                    && !card.path("cardUid").asText().isBlank()) return card.path("cardUid").asText();
        }
        throw forbidden("An active charging card is required for your account");
    }

    private String protocol(String stationId) {
        String protocol = connections.protocol(stationId);
        if (protocol == null || protocol.isBlank())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Charger is offline");
        return protocol;
    }

    private JsonNode fetch(RestClient client, String path, UUID id) {
        try {
            JsonNode body = client.get().uri(path, id).retrieve().body(JsonNode.class);
            if (body == null || body.isNull()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Charger data is unavailable");
            return body;
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 404) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Related charging record was not found", error);
            throw unavailable(error);
        } catch (RestClientException error) { throw unavailable(error); }
    }

    private ResponseStatusException forbidden(String message) { return new ResponseStatusException(HttpStatus.FORBIDDEN, message); }
    private ResponseStatusException unavailable(Exception error) { return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Charging service is unavailable", error); }
}
