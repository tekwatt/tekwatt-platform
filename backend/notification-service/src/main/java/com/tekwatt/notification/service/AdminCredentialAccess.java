package com.tekwatt.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AdminCredentialAccess {
    private final RestClient auth;
    private final RestClient admins;
    private final ObjectMapper json;
    private final Set<String> bootstrapAdminEmails;

    public AdminCredentialAccess(RestClient.Builder builder, ObjectMapper json,
            @Value("${tekwatt.services.auth:http://localhost:8081}") String authUrl,
            @Value("${tekwatt.services.admin:http://localhost:8100}") String adminUrl,
            @Value("${tekwatt.sms.provider-admin-emails:}") String allowedEmails) {
        this.auth = builder.baseUrl(authUrl).build();
        this.admins = builder.baseUrl(adminUrl).build();
        this.json = json;
        this.bootstrapAdminEmails = Arrays.stream(allowedEmails.split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank()).collect(Collectors.toUnmodifiableSet());
    }

    public void requireAdmin(String authorization, UUID tenantId) {
        if (authorization == null || !authorization.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in as an administrator.");
        try {
            // Auth service verifies the JWT signature, expiry, and active login session.
            auth.get().uri("/api/v1/auth/sessions").header("Authorization", authorization)
                    .retrieve().toBodilessEntity();
            String[] parts = authorization.substring(7).split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("Malformed access token");
            JsonNode claims = json.readTree(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode roles = claims.path("roles");
            if (roles.isArray()) {
                for (JsonNode role : roles) {
                    if ("ADMIN".equals(role.asText()) || "ROLE_ADMIN".equals(role.asText())) return;
                }
            }
            // Legacy bootstrap accounts may have a DRIVER token but an active admin
            // directory record. Only operator-allowlisted emails can use this path.
            String email = claims.path("email").asText("").trim().toLowerCase(Locale.ROOT);
            if (bootstrapAdminEmails.contains(email) && activeWorkspaceAdmin(tenantId, claims.path("sub").asText(""), email)) return;
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This login is not authorized to manage provider credentials for this workspace.");
        } catch (ResponseStatusException exception) { throw exception;
        } catch (HttpClientErrorException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again before editing provider credentials.");
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token.");
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Login verification is temporarily unavailable.");
        }
    }

    private boolean activeWorkspaceAdmin(UUID tenantId, String userId, String email) {
        JsonNode records = admins.get().uri(uri -> uri.path("/api/v1/admin/governance/administrators")
                .queryParam("tenantId", tenantId).build()).retrieve().body(JsonNode.class);
        if (records == null || !records.isArray()) return false;
        for (JsonNode record : records) {
            if ("ACTIVE".equalsIgnoreCase(record.path("status").asText())
                    && (userId.equals(record.path("authUserId").asText())
                        || email.equalsIgnoreCase(record.path("email").asText()))) return true;
        }
        return false;
    }
}
