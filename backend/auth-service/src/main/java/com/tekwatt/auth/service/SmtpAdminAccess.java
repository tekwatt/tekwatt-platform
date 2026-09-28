package com.tekwatt.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tekwatt.auth.repository.AppUserRepository;
import com.tekwatt.auth.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SmtpAdminAccess {
    private final JwtService jwt;
    private final RefreshTokenRepository sessions;
    private final AppUserRepository users;
    private final RestClient admins;
    private final Set<String> bootstrapAdminEmails;

    public SmtpAdminAccess(JwtService jwt, RefreshTokenRepository sessions, AppUserRepository users,
            RestClient.Builder builder,
            @Value("${tekwatt.services.admin:http://localhost:8100}") String adminUrl,
            @Value("${tekwatt.auth.smtp-admin-emails:}") String allowedEmails) {
        this.jwt = jwt; this.sessions = sessions; this.users = users;
        this.admins = builder.baseUrl(adminUrl).build();
        this.bootstrapAdminEmails = Arrays.stream(allowedEmails.split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank()).collect(Collectors.toUnmodifiableSet());
    }

    public String requireAdmin(String authorization, UUID tenantId) {
        JwtService.AccessIdentity identity = jwt.parse(authorization);
        sessions.findByIdAndUser_Id(identity.sessionId(), identity.userId())
                .filter(token -> !token.isRevoked() && token.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again before changing email settings."));
        var user = users.findById(identity.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is no longer available."));
        if (!user.isEnabled()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is disabled.");
        if ("ADMIN".equalsIgnoreCase(user.getRole())) return user.getEmail();
        String email = user.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!bootstrapAdminEmails.contains(email))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a platform administrator can manage email delivery.");
        try {
            JsonNode records = admins.get().uri(uri -> uri.path("/api/v1/admin/governance/administrators")
                    .queryParam("tenantId", tenantId).build()).retrieve().body(JsonNode.class);
            if (records != null && records.isArray()) for (JsonNode record : records) {
                if ("ACTIVE".equalsIgnoreCase(record.path("status").asText())
                        && (identity.userId().toString().equals(record.path("authUserId").asText())
                        || email.equalsIgnoreCase(record.path("email").asText()))) return user.getEmail();
            }
        } catch (Exception failure) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Administrator verification is temporarily unavailable.");
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is not an active administrator for the workspace.");
    }
}
