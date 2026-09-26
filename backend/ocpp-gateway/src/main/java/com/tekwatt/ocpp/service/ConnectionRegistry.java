package com.tekwatt.ocpp.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Service
public class ConnectionRegistry {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Instant> connectedAt = new ConcurrentHashMap<>();

    public void add(String station, WebSocketSession session) {
        WebSocketSession old = sessions.put(station, session);
        connectedAt.put(station, Instant.now());
        if (old != null && old.isOpen()) {
            try {
                old.close();
            } catch (Exception ignored) {
                // The new session is already registered and remains authoritative.
            }
        }
    }

    public void remove(String station, WebSocketSession session) {
        if (sessions.remove(station, session)) {
            connectedAt.remove(station);
        }
    }

    public List<ConnectionInfo> all() {
        return sessions.entrySet().stream()
                .map(entry -> new ConnectionInfo(
                        entry.getKey(),
                        entry.getValue().isOpen(),
                        connectedAt.get(entry.getKey()),
                        canonicalProtocol(entry.getValue().getAcceptedProtocol())))
                .toList();
    }

    public Optional<ConnectionInfo> get(String station) {
        WebSocketSession session = sessions.get(station);
        return session == null
                ? Optional.empty()
                : Optional.of(new ConnectionInfo(
                        station,
                        session.isOpen(),
                        connectedAt.get(station),
                        canonicalProtocol(session.getAcceptedProtocol())));
    }

    public void send(String station, String text) throws Exception {
        WebSocketSession session = sessions.get(station);
        if (session == null || !session.isOpen()) {
            throw new IllegalStateException("Charger is not connected");
        }
        session.sendMessage(new TextMessage(text));
    }

    public String protocol(String station) {
        WebSocketSession session = sessions.get(station);
        if (session == null || !session.isOpen()) {
            throw new IllegalStateException("Charger is not connected");
        }
        return canonicalProtocol(session.getAcceptedProtocol());
    }

    private String canonicalProtocol(String protocol) {
        return "ocpp2.0".equals(protocol) ? "ocpp2.0.1" : protocol;
    }

    public record ConnectionInfo(String stationId, boolean connected, Instant connectedAt, String protocol) {}
}
