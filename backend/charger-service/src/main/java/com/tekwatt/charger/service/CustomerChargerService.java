package com.tekwatt.charger.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tekwatt.charger.dto.ChargerResponse;
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

/** Mobile driver station discovery is limited to chargers explicitly assigned to the customer. */
@Service
public class CustomerChargerService {
    private final ChargerService chargers;
    private final RestClient users;

    public CustomerChargerService(ChargerService chargers, RestClient.Builder builder,
            @Value("${tekwatt.services.user:http://localhost:8082}") String userUrl) {
        this.chargers = chargers;
        this.users = builder.baseUrl(userUrl).build();
    }

    public List<ChargerResponse> mine(String authorization) {
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
            UUID tenantId = UUID.fromString(profile.path("tenantId").asText());
            Set<UUID> assigned = new HashSet<>();
            JsonNode assignments = profile.path("assignedChargerIds");
            if (assignments.isArray()) for (JsonNode id : assignments) assigned.add(UUID.fromString(id.asText()));
            if (assigned.isEmpty()) return List.of();
            return chargers.list(tenantId).stream().filter(charger -> assigned.contains(charger.id())).toList();
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer profile is invalid", error);
        }
    }
}
