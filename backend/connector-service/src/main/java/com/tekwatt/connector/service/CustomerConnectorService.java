package com.tekwatt.connector.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tekwatt.connector.dto.ConnectorResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Driver connector discovery only returns connectors on an assigned charger in the workspace. */
@Service
public class CustomerConnectorService {
    private final ConnectorService connectors;
    private final RestClient users;

    public CustomerConnectorService(ConnectorService connectors, RestClient.Builder builder,
            @Value("${tekwatt.services.user:http://localhost:8082}") String userUrl) {
        this.connectors = connectors;
        this.users = builder.baseUrl(userUrl).build();
    }

    public List<ConnectorResponse> mine(String authorization, UUID chargerId) {
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
            boolean assigned = false;
            JsonNode assignments = profile.path("assignedChargerIds");
            if (assignments.isArray()) for (JsonNode id : assignments)
                if (chargerId.equals(UUID.fromString(id.asText()))) assigned = true;
            if (!assigned) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Charger is not assigned to your account");
            return connectors.list(chargerId).stream().filter(item -> tenantId.equals(item.tenantId())).toList();
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer profile is invalid", error);
        }
    }
}
