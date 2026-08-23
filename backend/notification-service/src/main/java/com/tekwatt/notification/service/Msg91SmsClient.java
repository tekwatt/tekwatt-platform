package com.tekwatt.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Msg91SmsClient {
    private static final URI FLOW_URI = URI.create("https://control.msg91.com/api/v5/flow");
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json;
    private final String authKey;
    private final String templateId;
    private final String messageVariable;

    public Msg91SmsClient(ObjectMapper json,
                          @Value("${tekwatt.sms.msg91.auth-key:}") String authKey,
                          @Value("${tekwatt.sms.msg91.template-id:}") String templateId,
                          @Value("${tekwatt.sms.msg91.message-variable:message}") String messageVariable) {
        this.json = json;
        this.authKey = authKey;
        this.templateId = templateId;
        this.messageVariable = messageVariable;
    }

    public String send(String recipient, String message) {
        if (authKey.isBlank() || templateId.isBlank())
            throw new SmsDeliveryException("MSG91 is not configured. Set MSG91_AUTH_KEY and MSG91_TEMPLATE_ID.");
        String mobile = normalize(recipient);
        if (!mobile.matches("[1-9][0-9]{9,14}"))
            throw new SmsDeliveryException("SMS recipient must be a valid mobile number with country code.");
        try {
            ObjectNode target = json.createObjectNode();
            target.put("mobiles", mobile);
            target.put(messageVariable, message);
            String body = json.writeValueAsString(Map.of(
                    "template_id", templateId,
                    "short_url", "0",
                    "recipients", List.of(target)));
            HttpRequest request = HttpRequest.newBuilder(FLOW_URI)
                    .timeout(Duration.ofSeconds(20))
                    .header("accept", "application/json")
                    .header("authkey", authKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode result = json.readTree(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || "error".equalsIgnoreCase(result.path("type").asText())) {
                String error = result.path("message").asText("MSG91 rejected the SMS request.");
                throw new SmsDeliveryException(error);
            }
            String providerId = result.path("request_id").asText();
            return providerId.isBlank() ? "MSG91-ACCEPTED" : providerId;
        } catch (SmsDeliveryException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SmsDeliveryException("SMS request was interrupted. Please try again.");
        } catch (Exception exception) {
            throw new SmsDeliveryException("MSG91 could not be reached. Please try again.");
        }
    }

    private String normalize(String recipient) {
        String digits = recipient.replaceAll("[^0-9]", "");
        return digits.length() == 10 ? "91" + digits : digits;
    }

    public static class SmsDeliveryException extends RuntimeException {
        public SmsDeliveryException(String message) { super(message); }
    }
}
