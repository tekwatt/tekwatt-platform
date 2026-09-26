package com.tekwatt.session.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.tekwatt.session.entity.ChargingSession;
import com.tekwatt.session.entity.SessionStatus;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SessionBillingClient {
    private final RestClient bills, invoices, users;
    private final PaymentNotificationClient notifications;
    @Autowired
    public SessionBillingClient(RestClient.Builder builder,
            @Value("${tekwatt.services.billing:http://localhost:8090}") String billingUrl,
            @Value("${tekwatt.services.invoice:http://localhost:8091}") String invoiceUrl,
            @Value("${tekwatt.services.user:http://localhost:8082}") String userUrl,
            PaymentNotificationClient notifications) {
        bills = client(builder, billingUrl); invoices = client(builder, invoiceUrl); users = client(builder, userUrl);
        this.notifications = notifications;
    }
    SessionBillingClient(RestClient bills, RestClient invoices, RestClient users) {
        this.bills = bills; this.invoices = invoices; this.users = users;
        this.notifications = null;
    }
    private RestClient client(RestClient.Builder builder, String url) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000); factory.setReadTimeout(3000);
        return builder.clone().baseUrl(url).requestFactory(factory).build();
    }
    public void issue(ChargingSession session) {
        if (session.getStatus() != SessionStatus.COMPLETED) throw new IllegalArgumentException("Session is not completed");
        var bill = find(bills, "/api/v1/bills", session.getTenantId().toString(), "sessionId", session.getId().toString());
        if (bill == null) {
            var body = new LinkedHashMap<String, Object>();
            body.put("tenantId", session.getTenantId()); body.put("userId", session.getUserId());
            body.put("sessionId", session.getId()); body.put("tariffId", session.getTariffId());
            body.put("energyKwh", session.getEnergyKwh());
            long seconds = Math.max(0, Duration.between(session.getStartedAt(), session.getStoppedAt()).getSeconds());
            body.put("durationMinutes", (seconds + 59) / 60);
            body.put("energyPricePerKwh", session.getPricePerKwh()); body.put("timePricePerMinute", session.getTimePricePerMinute());
            body.put("sessionFee", session.getSessionFee()); body.put("taxPercent", session.getTaxPercent()); body.put("currency", session.getCurrency());
            bill = bills.post().uri("/api/v1/bills").body(body).retrieve().body(JsonNode.class);
        }
        if (bill == null) throw new IllegalStateException("Missing bill response");
        if (!bill.path("id").isTextual() || !bill.path("totalAmount").isNumber()) throw new IllegalStateException("Invalid bill response");
        if ("VOID".equals(bill.path("status").asText()) || "PAID".equals(bill.path("status").asText())) return;
        // Free sessions do not need a gateway payment request (providers reject zero-value orders).
        if (bill.path("totalAmount").decimalValue().signum() == 0) return;
        var invoice = find(invoices, "/api/v1/invoices", session.getTenantId().toString(), "billId", bill.path("id").asText());
        if (invoice == null) {
            var user = users.get().uri("/api/v1/users/{id}", session.getUserId()).retrieve().body(JsonNode.class);
            if (user == null || !session.getTenantId().toString().equals(user.path("tenantId").asText()))
                throw new IllegalStateException("Billing customer workspace mismatch");
            var body = new LinkedHashMap<String, Object>();
            body.put("tenantId", session.getTenantId()); body.put("userId", session.getUserId()); body.put("billId", bill.path("id").asText());
            String name = user.path("fullName").asText("");
            body.put("customerName", name.isBlank() ? user.path("email").asText() : name);
            body.put("customerEmail", user.path("email").asText());
            for (String field : new String[]{"subtotal", "taxAmount", "totalAmount"}) body.put(field, bill.path(field).decimalValue());
            body.put("currency", bill.path("currency").asText());
            var date = LocalDate.now(ZoneOffset.UTC);
            body.put("issueDate", date.toString()); body.put("dueDate", date.toString());
            invoice = invoices.post().uri("/api/v1/invoices").body(body).retrieve().body(JsonNode.class);
        }
        if (invoice == null) throw new IllegalStateException("Missing invoice response");
        if (!invoice.path("id").isTextual() || !java.util.Set.of("DRAFT", "ISSUED", "OVERDUE", "PAID", "VOID").contains(invoice.path("status").asText()))
            throw new IllegalStateException("Invalid invoice response");
        if ("DRAFT".equals(invoice.path("status").asText())) {
            invoices.post().uri("/api/v1/invoices/{id}/issue", invoice.path("id").asText()).retrieve().toBodilessEntity();
        }
        // Existing ISSUED/OVERDUE/PAID/VOID invoices are never reissued or charged.
        if (notifications != null && notifications.enabled() && !java.util.Set.of("PAID", "VOID").contains(invoice.path("status").asText())) {
            var customer = users.get().uri("/api/v1/users/{id}", session.getUserId()).retrieve().body(JsonNode.class);
            if (customer == null) throw new IllegalStateException("Missing billing customer");
            notifications.send(session, invoice, customer);
        }
    }
    private JsonNode find(RestClient client, String path, String tenant, String field, String value) {
        var rows = client.get().uri(path + "?tenantId={tenant}", tenant).retrieve().body(JsonNode.class);
        if (rows == null || !rows.isArray()) throw new IllegalStateException("Invalid billing list response");
        for (var row : rows) if (value.equals(row.path(field).asText())) return row;
        return null;
    }
}
