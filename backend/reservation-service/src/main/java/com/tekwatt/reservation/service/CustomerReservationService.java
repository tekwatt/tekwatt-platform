package com.tekwatt.reservation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tekwatt.reservation.dto.CustomerReservationRequest;
import com.tekwatt.reservation.dto.ReservationRequest;
import com.tekwatt.reservation.dto.ReservationResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Customer reservation operations derive workspace and customer from the authenticated profile. */
@Service
public class CustomerReservationService {
    private final ReservationService reservations;
    private final RestClient users;
    private final RestClient connectors;

    public CustomerReservationService(ReservationService reservations, RestClient.Builder builder,
            @Value("${tekwatt.services.user:http://localhost:8082}") String userUrl,
            @Value("${tekwatt.services.connector:http://localhost:8087}") String connectorUrl) {
        this.reservations = reservations;
        this.users = builder.clone().baseUrl(userUrl).build();
        this.connectors = builder.clone().baseUrl(connectorUrl).build();
    }

    public List<ReservationResponse> mine(String authorization) {
        Customer customer = customer(authorization);
        return reservations.list(customer.tenantId()).stream()
                .filter(item -> customer.userId().equals(item.userId())).toList();
    }

    public ReservationResponse create(String authorization, CustomerReservationRequest request) {
        Customer customer = customer(authorization);
        if (!customer.assignedChargers().contains(request.chargerId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Charger is not assigned to your account");
        JsonNode connector;
        try {
            connector = connectors.get().uri("/api/v1/connectors/{id}", request.connectorId())
                    .retrieve().body(JsonNode.class);
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 404)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Connector not found", error);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Connector service is unavailable", error);
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Connector service is unavailable", error);
        }
        if (connector == null || !customer.tenantId().toString().equals(connector.path("tenantId").asText())
                || !request.chargerId().toString().equals(connector.path("chargerId").asText()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Connector is not on your assigned charger");
        if (!"AVAILABLE".equals(connector.path("status").asText()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Connector is not available for reservation");
        return reservations.create(new ReservationRequest(customer.tenantId(), customer.userId(), request.chargerId(),
                request.connectorId(), "APP-RES-" + UUID.randomUUID(), request.startsAt(), request.expiresAt()));
    }

    public ReservationResponse cancel(String authorization, UUID id) {
        own(authorization, id);
        return reservations.cancel(id);
    }

    public ReservationResponse complete(String authorization, UUID id) {
        own(authorization, id);
        return reservations.complete(id);
    }

    private void own(String authorization, UUID id) {
        Customer customer = customer(authorization);
        ReservationResponse reservation = reservations.get(id);
        if (!customer.tenantId().equals(reservation.tenantId()) || !customer.userId().equals(reservation.userId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found in your account");
    }

    private Customer customer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again");
        JsonNode profile;
        try {
            profile = users.get().uri("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve().body(JsonNode.class);
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 401 || error.getStatusCode().value() == 403)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again", error);
            if (error.getStatusCode().value() == 404)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customer account is required", error);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer service is unavailable", error);
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer service is unavailable", error);
        }
        try {
            if (profile == null || !"ACTIVE".equalsIgnoreCase(profile.path("status").asText()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Active customer account is required");
            Set<UUID> assigned = new HashSet<>();
            JsonNode assignments = profile.path("assignedChargerIds");
            if (assignments.isArray()) for (JsonNode id : assignments) assigned.add(UUID.fromString(id.asText()));
            return new Customer(UUID.fromString(profile.path("tenantId").asText()),
                    UUID.fromString(profile.path("id").asText()), assigned);
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer profile is invalid", error);
        }
    }

    private record Customer(UUID tenantId, UUID userId, Set<UUID> assignedChargers) { }
}
