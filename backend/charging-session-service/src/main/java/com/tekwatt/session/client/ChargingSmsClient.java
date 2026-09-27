package com.tekwatt.session.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.tekwatt.session.entity.ChargingSession;
import com.tekwatt.session.entity.SessionStatus;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Customer-facing charging lifecycle SMS. Delivery is retried by the session worker. */
@Component
public class ChargingSmsClient {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a z")
            .withZone(ZoneId.of("Asia/Kolkata"));
    private final RestClient users, chargers, notifications;

    @Autowired
    public ChargingSmsClient(RestClient.Builder builder,
            @Value("${tekwatt.services.user:http://localhost:8082}") String userUrl,
            @Value("${tekwatt.services.charger:http://localhost:8083}") String chargerUrl,
            @Value("${tekwatt.services.notification:http://localhost:8093}") String notificationUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        this.users = builder.clone().baseUrl(userUrl).requestFactory(factory).build();
        this.chargers = builder.clone().baseUrl(chargerUrl).requestFactory(factory).build();
        this.notifications = builder.clone().baseUrl(notificationUrl).requestFactory(factory).build();
    }

    ChargingSmsClient(RestClient users, RestClient chargers, RestClient notifications) {
        this.users = users; this.chargers = chargers; this.notifications = notifications;
    }

    public void sendStarted(ChargingSession session) { send(session, true); }
    public void sendStopped(ChargingSession session) { send(session, false); }

    private void send(ChargingSession session, boolean started) {
        if (!started && session.getStoppedAt() == null) throw new IllegalStateException("Session has not stopped");
        JsonNode customer = users.get().uri("/api/v1/users/{id}", session.getUserId()).retrieve().body(JsonNode.class);
        if (customer == null || !session.getTenantId().toString().equals(customer.path("tenantId").asText()))
            throw new IllegalStateException("Charging customer workspace mismatch");
        String phone = customer.path("phone").asText("").trim();
        if (phone.isBlank()) throw new IllegalStateException("Charging SMS needs a customer phone number");
        JsonNode charger = chargers.get().uri("/api/v1/chargers/{id}", session.getChargerId()).retrieve().body(JsonNode.class);
        if (charger == null || !session.getTenantId().toString().equals(charger.path("tenantId").asText()))
            throw new IllegalStateException("Charging station workspace mismatch");
        String station = charger.path("stationName").asText("").trim();
        if (station.isBlank()) station = charger.path("stationId").asText("your station");
        String body = started
                ? "TekWatt: Charging started at " + station + " on " + TIME.format(session.getStartedAt())
                    + ". Charger " + charger.path("stationId").asText() + ". Transaction " + session.getTransactionId() + "."
                : "TekWatt: Charging " + (session.getStatus() == SessionStatus.INTERRUPTED ? "interrupted" : "stopped")
                    + " at " + station + " on " + TIME.format(session.getStoppedAt())
                    + ". Energy " + session.getEnergyKwh() + " kWh. Amount " + session.getCurrency()
                    + " " + session.getTotalCost() + ". Transaction " + session.getTransactionId() + ".";
        var request = new LinkedHashMap<String, Object>();
        request.put("tenantId", session.getTenantId());
        request.put("userId", session.getUserId());
        request.put("idempotencyKey", (started ? "charging-started:" : "charging-stopped:") + session.getId());
        request.put("channel", "SMS");
        request.put("recipient", phone);
        request.put("subject", started ? "Charging started" : "Charging stopped");
        request.put("body", body);
        request.put("templateKey", started ? "charging-started" : "charging-completed");
        request.put("maxAttempts", 3);
        JsonNode notification = notifications.post().uri("/api/v1/notifications").body(request).retrieve().body(JsonNode.class);
        if (notification == null) throw new IllegalStateException("Missing charging SMS result");
        if ("SENT".equals(notification.path("status").asText())) return;
        if ("FAILED".equals(notification.path("status").asText())) {
            if (notification.path("attemptCount").asInt() >= notification.path("maxAttempts").asInt(3))
                throw new IllegalStateException("Charging SMS attempts exhausted");
            notification = notifications.post().uri("/api/v1/notifications/{id}/retry", notification.path("id").asText())
                    .retrieve().body(JsonNode.class);
        }
        if (notification == null || !"QUEUED".equals(notification.path("status").asText()))
            throw new IllegalStateException("Charging SMS is not ready");
        JsonNode sent = notifications.post().uri("/api/v1/notifications/{id}/send", notification.path("id").asText())
                .retrieve().body(JsonNode.class);
        if (sent == null || !"SENT".equals(sent.path("status").asText()))
            throw new IllegalStateException("Charging SMS was not accepted by provider");
    }
}
