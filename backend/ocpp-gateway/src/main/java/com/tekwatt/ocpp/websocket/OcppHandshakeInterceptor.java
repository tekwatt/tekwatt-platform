package com.tekwatt.ocpp.websocket;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
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
        if (stationId == null || stationId.isBlank()) return false;

        if (!sharedKey.isBlank()
                && !matches(request.getHeaders().getFirst("X-OCPP-Key"), sharedKey)
                && !matchesBasicAuth(basicCredentials, stationId)) {
            return false;
        }

        attributes.put("stationId", stationId);
        return true;
    }

    private BasicCredentials basicCredentials(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Basic ", 0, 6)) return null;
        try {
            String credentials = new String(
                    Base64.getDecoder().decode(authorization.substring(6).trim()), StandardCharsets.UTF_8);
            int separator = credentials.indexOf(':');
            if (separator < 1) return null;
            return new BasicCredentials(
                    credentials.substring(0, separator), credentials.substring(separator + 1));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean matchesBasicAuth(BasicCredentials credentials, String stationId) {
        return credentials != null
                && stationId.equals(credentials.username())
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
