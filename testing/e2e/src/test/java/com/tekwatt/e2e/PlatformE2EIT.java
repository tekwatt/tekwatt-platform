package com.tekwatt.e2e;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PlatformE2EIT {
  private static final ObjectMapper JSON = new ObjectMapper();
  private final String baseUrl = env("E2E_BASE_URL", "http://localhost:8080");
  private final String wsUrl = env("E2E_WS_URL", baseUrl.replaceFirst("^http", "ws"));

  @Test
  void completesChargingToPdfReportingJourney() throws Exception {
    String run = Long.toString(System.currentTimeMillis());
    String email = "e2e-" + run + "@tekwatt.test";
    Response auth = post(givenBase(), "/api/v1/auth/register", Map.of(
        "email", email, "password", "TekWatt-E2E-" + run), 201);
    String token = auth.jsonPath().getString("accessToken");
    UUID authUserId = UUID.fromString(jwtSubject(token));
    RequestSpecification api = givenBase().header("Authorization", "Bearer " + token);

    UUID tenantId = id(post(api, "/api/v1/tenants", Map.of(
        "name", "E2E Tenant " + run, "slug", "e2e-" + run, "contactEmail", email), 201));
    UUID userId = id(post(api, "/api/v1/users", Map.of(
        "authUserId", authUserId, "tenantId", tenantId, "firstName", "End", "lastName", "ToEnd",
        "email", email, "phone", "+910000000000"), 201));
    String stationId = "E2E-" + run;
    UUID chargerId = id(post(api, "/api/v1/chargers", Map.of(
        "tenantId", tenantId, "stationId", stationId, "serialNumber", "SER-" + run,
        "vendor", "TekWatt", "model", "E2E", "protocolVersion", "OCPP_2_0_1"), 201));
    UUID connectorId = id(post(api, "/api/v1/connectors", Map.of(
        "tenantId", tenantId, "chargerId", chargerId, "connectorNumber", 1, "type", "CCS2",
        "maxPowerKw", 60, "maxVoltage", 800, "maxCurrent", 125), 201));

    verifyOcpp201(stationId);

    String transactionId = "TX-" + run;
    Response started = post(api, "/api/v1/charging-sessions", Map.of(
        "tenantId", tenantId, "userId", userId, "chargerId", chargerId, "connectorId", connectorId,
        "transactionId", transactionId, "meterStartWh", 1000, "pricePerKwh", 10, "currency", "INR"), 201);
    UUID sessionId = id(started);
    post(api, "/api/v1/charging-sessions/" + sessionId + "/meter-values", Map.of(
        "meterWh", 2500, "recordedAt", Instant.now().toString()), 200);
    Response stopped = post(api, "/api/v1/charging-sessions/" + sessionId + "/stop", Map.of(
        "meterStopWh", 2500, "status", "COMPLETED"), 200);
    assertThat(stopped.jsonPath().getString("energyKwh")).isEqualTo("1.5000");

    Response bill = post(api, "/api/v1/bills", Map.of(
        "tenantId", tenantId, "userId", userId, "sessionId", sessionId, "energyKwh", 1.5,
        "durationMinutes", 30, "energyPricePerKwh", 10, "timePricePerMinute", 1,
        "sessionFee", 2, "taxPercent", 18, "currency", "INR"), 201);
    assertThat(bill.jsonPath().getString("status")).isEqualTo("PENDING");

    Instant occurredAt = Instant.now();
    post(api, "/api/v1/analytics/events", Map.of(
        "externalEventId", "session-" + sessionId, "tenantId", tenantId, "chargerId", chargerId,
        "sessionId", sessionId, "energyKwh", 1.5, "revenue", bill.jsonPath().getDouble("totalAmount"),
        "currency", "INR", "durationSeconds", 1800, "occurredAt", occurredAt.toString()), 201);
    Instant from = occurredAt.minus(Duration.ofHours(1));
    Instant to = occurredAt.plus(Duration.ofHours(1));
    Response overview = api.queryParam("tenantId", tenantId).queryParam("from", from.toString())
        .queryParam("to", to.toString()).get(baseUrl + "/api/v1/analytics/overview");
    overview.then().statusCode(200);
    assertThat(overview.jsonPath().getInt("sessions")).isGreaterThanOrEqualTo(1);

    Response report = post(api, "/api/v1/reports", Map.of(
        "tenantId", tenantId, "reportType", "OVERVIEW", "format", "PDF",
        "from", from.toString(), "to", to.toString()), 201);
    assertThat(report.jsonPath().getString("status")).isEqualTo("COMPLETED");
    UUID reportId = id(report);
    byte[] pdf = api.get(baseUrl + "/api/v1/reports/" + reportId + "/download")
        .then().statusCode(200).contentType("application/pdf").extract().asByteArray();
    assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
  }

  private void verifyOcpp201(String stationId) throws Exception {
    SocketListener listener = new SocketListener();
    WebSocket.Builder builder = HttpClient.newHttpClient().newWebSocketBuilder()
        .subprotocols("ocpp2.0.1").connectTimeout(Duration.ofSeconds(10));
    String key = System.getenv("E2E_OCPP_KEY");
    if (key != null && !key.isBlank()) builder.header("X-OCPP-Key", key);
    WebSocket socket = builder.buildAsync(URI.create(wsUrl + "/ocpp/" + stationId), listener)
        .get(15, TimeUnit.SECONDS);
    socket.sendText("[2,\"boot-1\",\"BootNotification\",{\"reason\":\"PowerUp\",\"chargingStation\":{\"model\":\"E2E\",\"vendorName\":\"TekWatt\"}}]", true).join();
    assertThat(listener.messages.poll(10, TimeUnit.SECONDS)).contains("[3,\"boot-1\"").contains("Accepted");
    socket.sendText("[2,\"tx-1\",\"TransactionEvent\",{\"eventType\":\"Started\",\"triggerReason\":\"Authorized\",\"seqNo\":0,\"transactionInfo\":{\"transactionId\":\"e2e\"}}]", true).join();
    assertThat(listener.messages.poll(10, TimeUnit.SECONDS)).contains("[3,\"tx-1\"");
    socket.sendClose(WebSocket.NORMAL_CLOSURE, "complete").join();
  }

  private RequestSpecification givenBase() {
    return given().baseUri(baseUrl).contentType(ContentType.JSON).accept(ContentType.JSON);
  }

  private Response post(RequestSpecification specification, String path, Object body, int status) {
    return specification.body(body).post(baseUrl + path).then().statusCode(status).extract().response();
  }

  private UUID id(Response response) {
    return UUID.fromString(response.jsonPath().getString("id"));
  }

  private String jwtSubject(String token) throws Exception {
    byte[] payload = Base64.getUrlDecoder().decode(token.split("\\.")[1]);
    return JSON.readTree(payload).path("sub").asText();
  }

  private static String env(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? fallback : value;
  }

  private static final class SocketListener implements WebSocket.Listener {
    private final LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();
    private final StringBuilder current = new StringBuilder();
    @Override public void onOpen(WebSocket webSocket) { webSocket.request(1); }
    @Override public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
      current.append(data);
      if (last) { messages.add(current.toString()); current.setLength(0); }
      webSocket.request(1);
      return null;
    }
  }
}
