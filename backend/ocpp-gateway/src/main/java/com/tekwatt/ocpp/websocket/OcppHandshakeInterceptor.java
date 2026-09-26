package com.tekwatt.ocpp.websocket;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriTemplate;

@Component
public class OcppHandshakeInterceptor implements HandshakeInterceptor {
    private static final Logger log = LoggerFactory.getLogger(OcppHandshakeInterceptor.class);
    private static final UriTemplate OCPP_PATH = new UriTemplate("/ocpp/{stationId}");

    private final String sharedKey;

    public OcppHandshakeInterceptor(@Value("${tekwatt.ocpp.shared-key:}") String sharedKey) {
        this.sharedKey = sharedKey;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        BasicCredentials basicCredentials = basicCredentials(
                request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        String stationId = OCPP_PATH.match(request.getURI().getPath()).get("stationId");
        if ((stationId == null || stationId.isBlank()) && basicCredentials != null) {
            stationId = basicCredentials.username();
        }
        if (stationId == null || stationId.isBlank()) {
            log.warn("Rejected OCPP WebSocket handshake from {}: station ID is missing", remoteAddress(request));
            return false;
        }

        if (!sharedKey.isBlank()
                && !matches(request.getHeaders().getFirst("X-OCPP-Key"), sharedKey)
                && !matchesBasicAuth(basicCredentials, stationId)) {
            log.warn("Rejected OCPP WebSocket handshake for station {} from {}: credentials are invalid",
                    stationId, remoteAddress(request));
            return false;
        }

        attributes.put("stationId", stationId);
        log.info("Accepted OCPP WebSocket handshake for station {} from {}; origin={}; requestedProtocols={}",
                stationId,
                remoteAddress(request),
                valueOrDash(request.getHeaders().getOrigin()),
                valueOrDash(request.getHeaders().getFirst("Sec-WebSocket-Protocol")));
        return true;
    }

    private String remoteAddress(ServerHttpRequest request) {
        return request.getRemoteAddress() == null ? "unknown" : request.getRemoteAddress().toString();
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private BasicCredentials basicCredentials(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Basic ", 0, 6)) return null;
        try {
            String credentials = new String(
                    Base64.getDecoder().decode(authorization.substring(6).trim()), StandardCharsets.UTF_8);
            int separator = credentials.indexOf(':');
            if (separator < 0) return null;
            return new BasicCredentials(
                    credentials.substring(0, separator), credentials.substring(separator + 1));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean matchesBasicAuth(BasicCredentials credentials, String stationId) {
        return credentials != null
                && (credentials.username().isBlank() || stationId.equals(credentials.username()))
                && matches(credentials.password(), sharedKey);
    }

    private boolean matches(String candidate, String expected) {
        if (candidate == null) return false;
        return MessageDigest.isEqual(
                candidate.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception exception) {
        // No post-handshake action is required.
    }

    private record BasicCredentials(String username, String password) {}
}
