package com.tekwatt.ocpi;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Component
public class OcpiProtocol extends OncePerRequestFilter {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public OcpiProtocol(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/ocpi/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String[] parts = request.getRequestURI().split("/");
        if (parts.length < 4) {
            error(request, response, 404, 2003, "Unknown OCPI endpoint");
            return;
        }
        try {
            UUID.fromString(parts[2]);
        } catch (IllegalArgumentException invalidTenant) {
            error(request, response, 401, 2000, "Invalid OCPI credentials");
            return;
        }
        var hashes = jdbc.queryForList(
                "SELECT t.token_sha256 FROM ocpi_partner_tokens t JOIN ocpi_parties p ON p.tenant_id = t.tenant_id WHERE t.tenant_id = ? AND t.enabled = TRUE AND p.enabled = TRUE",
                String.class, parts[2]);
        String authorization = request.getHeader("Authorization");
        String token = authorization != null && authorization.startsWith("Token ")
                ? authorization.substring(6).trim() : "";
        boolean valid = false;
        if (!token.isEmpty() && token.length() <= 512 && !hashes.isEmpty()) {
            for (String hash : hashes) valid |= matches(token, hash);
            try {
                String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
                for (String hash : hashes) valid |= matches(decoded, hash);
            } catch (IllegalArgumentException ignored) {
                // Accept a direct token as well as the OCPI base64 wire representation.
            }
        }
        if (!valid) {
            error(request, response, 401, 2000, "Invalid OCPI credentials");
            return;
        }
        String requestId = request.getHeader("X-Request-ID");
        String correlationId = request.getHeader("X-Correlation-ID");
        response.setHeader("X-Request-ID", requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId);
        response.setHeader("X-Correlation-ID", correlationId == null || correlationId.isBlank() ? UUID.randomUUID().toString() : correlationId);
        chain.doFilter(request, response);
    }

    static boolean matches(String token, String hash) {
        if (hash == null) return false;
        return MessageDigest.isEqual(sha256(token).getBytes(StandardCharsets.US_ASCII), hash.getBytes(StandardCharsets.US_ASCII));
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    static Map<String, Object> envelope(Object data) {
        return Map.of("data", data, "status_code", 1000, "timestamp", Instant.now().toString());
    }

    private void error(HttpServletRequest request, HttpServletResponse response, int httpStatus, int ocpiStatus, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json");
        String requestId = request.getHeader("X-Request-ID");
        String correlationId = request.getHeader("X-Correlation-ID");
        response.setHeader("X-Request-ID", requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId);
        response.setHeader("X-Correlation-ID", correlationId == null || correlationId.isBlank() ? UUID.randomUUID().toString() : correlationId);
        json.writeValue(response.getOutputStream(), Map.of("status_code", ocpiStatus,
                "status_message", message, "timestamp", Instant.now().toString()));
    }
}
