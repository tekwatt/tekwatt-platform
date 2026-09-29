package com.tekwatt.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.tekwatt.gateway.config.ApiSessionAuthenticationFilter;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class ApiSessionAuthenticationFilterTest {
    private ApiSessionAuthenticationFilter filter(HttpStatus identityStatus) {
        return filter(identityStatus, "ADMIN");
    }

    private ApiSessionAuthenticationFilter filter(HttpStatus identityStatus, String role) {
        return new ApiSessionAuthenticationFilter(WebClient.builder().exchangeFunction(request -> {
            assertThat(request.url().getPath()).isEqualTo("/api/v1/auth/identity");
            assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer session-token");
            return Mono.just(ClientResponse.create(identityStatus)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body("{\"role\":\"" + role + "\"}").build());
        }), "http://auth-service");
    }

    @Test
    void anonymousApiRequestsNeverReachServices() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/chargers?tenantId=demo"));
        var forwarded = new AtomicBoolean();
        filter(HttpStatus.OK).filter(exchange, request -> { forwarded.set(true); return Mono.empty(); }).block();
        assertThat(forwarded).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void revokedSessionCannotReachServices() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/chargers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer session-token"));
        var forwarded = new AtomicBoolean();
        filter(HttpStatus.UNAUTHORIZED).filter(exchange, request -> { forwarded.set(true); return Mono.empty(); }).block();
        assertThat(forwarded).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validSessionReachesService() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/users/cpo/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer session-token"));
        var forwarded = new AtomicBoolean();
        filter(HttpStatus.OK).filter(exchange, request -> { forwarded.set(true); return Mono.empty(); }).block();
        assertThat(forwarded).isTrue();
    }

    @Test
    void driverCannotUseLegacyFinancialEndpoints() {
        for (String path : new String[] { "/api/v1/invoices?tenantId=demo", "/api/v1/invoices/id/pay",
                "/api/v1/payments/operations/wallets", "/api/v1/payments/id/result", "/api/v1/bills/id",
                "/api/v1/chargers?tenantId=demo", "/api/v1/chargers/id/status",
                "/api/v1/reservations?tenantId=demo", "/api/v1/reservations/id/cancel",
                "/api/v1/connectors?chargerId=demo", "/api/v1/connectors/id/status",
                "/api/v1/users/directory/rfid-cards?tenantId=demo", "/api/v1/ocpp/commands/id/result",
                "/api/v1/ocpp/connections/station-1",
                "/api/v1/charging-sessions?tenantId=demo", "/api/v1/charging-sessions/id/stop",
                "/api/v1/support/tickets?tenantId=demo", "/api/v1/support/tickets/id" }) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer session-token"));
            var forwarded = new AtomicBoolean();
            filter(HttpStatus.OK, "DRIVER").filter(exchange, request -> {
                forwarded.set(true); return Mono.empty();
            }).block();
            assertThat(forwarded).as(path).isFalse();
            assertThat(exchange.getResponse().getStatusCode()).as(path).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void driverCanUseServerScopedFinancialEndpoints() {
        for (String path : new String[] { "/api/v1/invoices/my", "/api/v1/payments/my/wallet",
                "/api/v1/payments/my/invoices/id/settle", "/api/v1/bills/my/id", "/api/v1/chargers/my",
                "/api/v1/reservations/my", "/api/v1/reservations/my/id/cancel", "/api/v1/connectors/my",
                "/api/v1/users/me/rfid-cards", "/api/v1/ocpp/customer-commands/id/result",
                "/api/v1/charging-sessions/my", "/api/v1/support/tickets/my/id" }) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer session-token"));
            var forwarded = new AtomicBoolean();
            filter(HttpStatus.OK, "DRIVER").filter(exchange, request -> {
                forwarded.set(true); return Mono.empty();
            }).block();
            assertThat(forwarded).as(path).isTrue();
        }
    }

    @Test
    void publicAuthenticationAndCorsPreflightStayAccessible() {
        for (var request : new MockServerHttpRequest[] {
                MockServerHttpRequest.post("/api/v1/auth/login").build(),
                MockServerHttpRequest.get("/api/v1/auth/otp/msg91/config").build(),
                MockServerHttpRequest.post("/api/v1/auth/otp/msg91/login").build(),
                MockServerHttpRequest.post("/api/v1/auth/otp/msg91/phone/login").build(),
                MockServerHttpRequest.post("/api/v1/auth/register").build(),
                MockServerHttpRequest.options("/api/v1/chargers").build()}) {
            var forwarded = new AtomicBoolean();
            filter(HttpStatus.UNAUTHORIZED).filter(MockServerWebExchange.from(request), exchange -> {
                forwarded.set(true); return Mono.empty();
            }).block();
            assertThat(forwarded).isTrue();
        }
    }

    @Test
    void linkingPhoneRequiresAnActiveSession() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/otp/msg91/phone"));
        var forwarded = new AtomicBoolean();
        filter(HttpStatus.OK).filter(exchange, request -> { forwarded.set(true); return Mono.empty(); }).block();
        assertThat(forwarded).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
