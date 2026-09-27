package com.tekwatt.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RazorpayClient {
    private static final URI ORDERS_URI = URI.create("https://api.razorpay.com/v1/orders");
    private final HttpClient http;
    private final ObjectMapper json;

    @Autowired
    public RazorpayClient(ObjectMapper json) {
        this(json, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    RazorpayClient(ObjectMapper json, HttpClient http) {
        this.json = json;
        this.http = http;
    }

    public ConnectionCheck testConnection(String keyId, String secret) {
        if (keyId == null || !keyId.startsWith("rzp_test_")) {
            return new ConnectionCheck(false, "Use a Razorpay Test Mode key. Live credentials cannot be tested here.");
        }
        if (secret == null || secret.isBlank()) {
            return new ConnectionCheck(false, "Save the matching Razorpay test secret before testing.");
        }
        String authorization = Base64.getEncoder().encodeToString(
                (keyId + ":" + secret).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1/orders?count=1"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Basic " + authorization)
                .GET()
                .build();
        try {
            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 200) {
                return new ConnectionCheck(true, "Test credentials accepted by Razorpay. No order or payment was created.");
            }
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                return new ConnectionCheck(false, "Razorpay rejected the saved test key or secret. Check that they are a matching pair.");
            }
            return new ConnectionCheck(false, "Razorpay could not verify this configuration right now (HTTP " + response.statusCode() + ").");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new ConnectionCheck(false, "Razorpay connection check was interrupted. Try again.");
        } catch (IOException exception) {
            return new ConnectionCheck(false, "The payment service could not reach Razorpay. Try again later.");
        }
    }

    public record ConnectionCheck(boolean success, String message) {}

    public String createOrder(String keyId, String secret, long amount, String currency, String receipt, String description) {
        try {
            var payload = json.writeValueAsString(Map.of(
                    "amount", amount,
                    "currency", currency,
                    "receipt", receipt,
                    "notes", Map.of("description", description == null ? "TekWatt charging payment" : description)));
            JsonNode response = send(ORDERS_URI, keyId, secret, payload);
            String orderId = response.path("id").asText();
            if (orderId.isBlank()) throw providerFailure("Razorpay did not return an order ID.");
            return orderId;
        } catch (IOException exception) {
            throw providerFailure("Razorpay order data could not be prepared.");
        }
    }

    public void refund(String keyId, String secret, String razorpayPaymentId) {
        send(URI.create("https://api.razorpay.com/v1/payments/" + razorpayPaymentId + "/refund"), keyId, secret, "{}");
    }

    private JsonNode send(URI uri, String keyId, String secret, String payload) {
        String authorization = Base64.getEncoder().encodeToString((keyId + ":" + secret).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Basic " + authorization)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = extractError(response.body());
                throw providerFailure(message.isBlank() ? "Razorpay rejected the request." : message);
            }
            return json.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw providerFailure("Razorpay request was interrupted. Please try again.");
        } catch (IOException exception) {
            throw providerFailure("Razorpay could not be reached. Please try again.");
        }
    }

    private String extractError(String body) {
        try {
            return json.readTree(body).path("error").path("description").asText();
        } catch (Exception ignored) {
            return "";
        }
    }

    private ResponseStatusException providerFailure(String message) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
    }
}
