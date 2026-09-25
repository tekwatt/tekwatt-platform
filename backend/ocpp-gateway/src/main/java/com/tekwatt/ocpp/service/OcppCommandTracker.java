package com.tekwatt.ocpp.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class OcppCommandTracker {
    private final Map<String, Result> results = new ConcurrentHashMap<>();

    public void register(String messageId) {
        purgeExpired();
        results.put(messageId, new Result("PENDING", null, Instant.now()));
    }

    public void fail(String messageId, String message) {
        results.put(messageId, new Result("FAILED", message, Instant.now()));
    }

    public void complete(String messageId, int messageType, JsonNode frame) {
        if (!results.containsKey(messageId)) return;
        if (messageType == 4) {
            String message = frame.size() > 3 ? frame.get(3).asText("Charge point rejected the command") : "Charge point rejected the command";
            results.put(messageId, new Result("FAILED", message, Instant.now()));
            return;
        }
        JsonNode payload = frame.size() > 2 ? frame.get(2) : frame;
        String status = payload.path("status").asText("Accepted");
        boolean accepted = "Accepted".equalsIgnoreCase(status) || "Unlocked".equalsIgnoreCase(status);
        results.put(messageId, new Result(accepted ? "ACCEPTED" : "REJECTED", accepted ? null : status, Instant.now()));
    }

    public Result result(String messageId) {
        purgeExpired();
        return results.getOrDefault(messageId, new Result("UNKNOWN", "Unknown command", Instant.now()));
    }

    private void purgeExpired() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        results.entrySet().removeIf(entry -> entry.getValue().updatedAt().isBefore(cutoff));
    }

    public record Result(String result, String message, Instant updatedAt) {}
}
