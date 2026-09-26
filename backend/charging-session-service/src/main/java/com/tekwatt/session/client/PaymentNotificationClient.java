package com.tekwatt.session.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.tekwatt.session.entity.ChargingSession;
import java.net.URI;
import java.util.LinkedHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Sends only newly issued, unpaid invoice requests; never charges the customer. */
@Component
public class PaymentNotificationClient {
    private final RestClient client;
    private final boolean enabled;
    private final String portal;
    @Autowired
    public PaymentNotificationClient(RestClient.Builder builder,
            @Value("${tekwatt.services.notification:http://localhost:8093}") String url,
            @Value("${tekwatt.billing.sms-enabled:false}") boolean enabled,
            @Value("${tekwatt.billing.portal-url:}") String portal) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000); factory.setReadTimeout(30000);
        this.client = builder.clone().baseUrl(url).requestFactory(factory).build();
        this.enabled = enabled; this.portal = portal;
    }
    PaymentNotificationClient(RestClient client, boolean enabled, String portal) {
        this.client = client; this.enabled = enabled; this.portal = portal;
    }
    public boolean enabled() { return enabled; }
    public void send(ChargingSession session, JsonNode invoice, JsonNode customer) {
        if (!enabled || !java.util.Set.of("DRAFT", "ISSUED", "OVERDUE").contains(invoice.path("status").asText())) return;
        if (!session.getTenantId().toString().equals(customer.path("tenantId").asText())) throw new IllegalStateException("Customer workspace mismatch");
        String phone = customer.path("phone").asText("").trim();
        if (phone.isBlank()) throw new IllegalStateException("Payment SMS needs a customer phone number");
        URI base = URI.create(portal);
        if (!"https".equals(base.getScheme()) || base.getHost() == null || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null)
            throw new IllegalStateException("Configure an HTTPS customer portal URL without credentials, query or fragment");
        String invoiceId = java.util.UUID.fromString(invoice.path("id").asText()).toString();
        String link = portal.replaceAll("/+$", "") + "/?invoice=" + invoiceId + "&workspace=" + session.getTenantId() + "#/payments/invoices";
        String body = "TekWatt: Charging complete. Invoice " + invoice.path("invoiceNumber").asText()
                + ", " + invoice.path("currency").asText() + " " + invoice.path("totalAmount").asText()
                + " (tax " + invoice.path("taxAmount").asText() + "). Energy " + session.getEnergyKwh()
                + " kWh. Due " + invoice.path("dueDate").asText() + ". View invoice and pay (sign in): " + link;
        var request = new LinkedHashMap<String,Object>();
        request.put("tenantId", session.getTenantId()); request.put("userId", session.getUserId());
        request.put("idempotencyKey", "pay-sms:" + invoiceId); request.put("channel", "SMS");
        request.put("recipient", phone); request.put("subject", "Charging payment due"); request.put("body", body);
        request.put("templateKey", "charging-payment-due"); request.put("maxAttempts", 3);
        JsonNode notification = client.post().uri("/api/v1/notifications").body(request).retrieve().body(JsonNode.class);
        if (notification == null) throw new IllegalStateException("Missing notification result");
        String status = notification.path("status").asText();
        if ("SENT".equals(status)) return;
        if ("FAILED".equals(status)) {
            // Keep billing queued for operations to inspect; do not silently declare delivery successful.
            if (notification.path("attemptCount").asInt() >= notification.path("maxAttempts").asInt(3))
                throw new IllegalStateException("Payment SMS attempts exhausted; inspect notification delivery error");
            notification = client.post().uri("/api/v1/notifications/{id}/retry", notification.path("id").asText()).retrieve().body(JsonNode.class);
        }
        if (notification == null || !"QUEUED".equals(notification.path("status").asText())) throw new IllegalStateException("Payment SMS is not ready");
        var sent = client.post().uri("/api/v1/notifications/{id}/send", notification.path("id").asText()).retrieve().body(JsonNode.class);
        if (sent == null || !"SENT".equals(sent.path("status").asText())) throw new IllegalStateException("Payment SMS was not accepted by provider");
    }
}
