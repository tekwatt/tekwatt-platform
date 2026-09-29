package com.tekwatt.ocpp.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OcppCommandTracker {
    private final Map<String, Result> results = new ConcurrentHashMap<>();
    private final Map<String, UUID> customerOwners = new ConcurrentHashMap<>();

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

    public void claimForCustomer(String messageId, UUID userId) {
        if (!results.containsKey(messageId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Command result not found");
        customerOwners.put(messageId, userId);
    }

    public Result resultForCustomer(String messageId, UUID userId) {
        purgeExpired();
        if (!userId.equals(customerOwners.get(messageId)))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Command result not found");
        return result(messageId);
    }

    private void purgeExpired() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        results.entrySet().removeIf(entry -> entry.getValue().updatedAt().isBefore(cutoff));
        customerOwners.keySet().removeIf(messageId -> !results.containsKey(messageId));
    }

    public record Result(String result, String message, Instant updatedAt) {}
}
