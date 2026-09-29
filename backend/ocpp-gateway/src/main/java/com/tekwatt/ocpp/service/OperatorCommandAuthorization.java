package com.tekwatt.ocpp.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Authorizes operator commands against the live login and the charger's workspace. */
@Service
public class OperatorCommandAuthorization {
    private final RestClient auth;
    private final RestClient chargers;
    private final RestClient admins;
    private final String internalKey;

    public OperatorCommandAuthorization(RestClient.Builder builder,
            @Value("${tekwatt.services.auth:http://localhost:8081}") String authUrl,
            @Value("${tekwatt.services.charger:http://localhost:8083}") String chargerUrl,
            @Value("${tekwatt.services.admin:http://localhost:8100}") String adminUrl,
            @Value("${tekwatt.ocpp.internal-command-key:}") String internalKey) {
        auth = builder.clone().baseUrl(authUrl).build();
        chargers = builder.clone().baseUrl(chargerUrl).build();
        admins = builder.clone().baseUrl(adminUrl).build();
        this.internalKey = internalKey;
    }

    public void requireAdministrator(String authorization, String stationId, String serviceKey) {
        if (internalKey != null && !internalKey.isBlank() && serviceKey != null
                && MessageDigest.isEqual(internalKey.getBytes(StandardCharsets.UTF_8),
                        serviceKey.getBytes(StandardCharsets.UTF_8))) return;
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again");
        if (stationId == null || stationId.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Station identifier is required");
        JsonNode identity = fetchIdentity(authorization);
        String userId = identity.path("userId").asText();
        if (userId.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Login identity is unavailable");
        String role = identity.path("role").asText();
        if (!"ADMIN".equalsIgnoreCase(role) && !"ROLE_ADMIN".equalsIgnoreCase(role))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator login is required");
        JsonNode charger = fetch(chargers, "/api/v1/chargers/by-station/{stationId}", stationId);
        UUID tenantId;
        try { tenantId = UUID.fromString(charger.path("tenantId").asText()); }
        catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Charger workspace is unavailable", error);
        }
        JsonNode records = fetch(admins, "/api/v1/admin/governance/administrators?tenantId={tenantId}", tenantId);
        if (!records.isArray())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Administrator directory is unavailable");
        for (JsonNode record : records) {
            if (!tenantId.toString().equals(record.path("tenantId").asText())
                    || !"ACTIVE".equalsIgnoreCase(record.path("status").asText())) continue;
            if (userId.equals(record.path("authUserId").asText())) return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "An active administrator for this charger's workspace is required");
    }

    private JsonNode fetch(RestClient client, String path, Object... variables) {
        try {
            JsonNode body = client.get().uri(path, variables).retrieve().body(JsonNode.class);
            if (body == null || body.isNull())
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Authorization data is unavailable");
            return body;
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 401 || error.getStatusCode().value() == 403)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again", error);
            if (error.getStatusCode().value() == 404)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Charger was not found", error);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Authorization service is unavailable", error);
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Authorization service is unavailable", error);
        }
    }

    private JsonNode fetchIdentity(String authorization) {
        try {
            JsonNode body = auth.get().uri("/api/v1/auth/identity").header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve().body(JsonNode.class);
            if (body == null || body.isNull())
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Login identity is unavailable");
            return body;
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 401 || error.getStatusCode().value() == 403)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again", error);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Login verification is unavailable", error);
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Login verification is unavailable", error);
        }
    }
}
