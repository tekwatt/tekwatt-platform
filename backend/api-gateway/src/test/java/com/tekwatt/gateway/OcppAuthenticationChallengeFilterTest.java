package com.tekwatt.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.tekwatt.gateway.config.OcppAuthenticationChallengeFilter;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class OcppAuthenticationChallengeFilterTest {
    @Test
    void missingCredentialsReceiveChallengeBeforeTheUpgradeChainRuns() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/ocpp/STATION-1")
                .header(HttpHeaders.UPGRADE, "websocket"));
        var called = new AtomicBoolean();
        new OcppAuthenticationChallengeFilter(true).filter(exchange, request -> {
            called.set(true);
            return Mono.empty();
        }).block();
        assertThat(called).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE))
                .isEqualTo("Basic realm=\"TekWatt OCPP\", charset=\"UTF-8\"");
        assertThat(exchange.getResponse().getHeaders().getCacheControl()).isEqualTo("no-store");
    }

    @Test
    void credentialsAreForwardedUnchangedForDownstreamValidation() {
        for (String header : new String[] {HttpHeaders.AUTHORIZATION, "X-OCPP-Key"}) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/ocpp/STATION-1")
                    .header(HttpHeaders.UPGRADE, "websocket").header(header, "test-credential"));
            var called = new AtomicBoolean();
            new OcppAuthenticationChallengeFilter(true).filter(exchange, request -> {
                called.set(true);
                assertThat(request.getRequest().getHeaders().getFirst(header)).isEqualTo("test-credential");
                return Mono.empty();
            }).block();
            assertThat(called).isTrue();
        }
    }

    @Test
    void unrelatedRequestsAndExplicitAnonymousDevelopmentRemainUnchanged() {
        for (var request : new MockServerHttpRequest[] {
                MockServerHttpRequest.get("/api/v1/ocpp/connections").build(),
                MockServerHttpRequest.options("/ocpp/STATION-1").build(),
                MockServerHttpRequest.get("/other").header(HttpHeaders.UPGRADE, "websocket").build()}) {
            var called = new AtomicBoolean();
            new OcppAuthenticationChallengeFilter(true).filter(MockServerWebExchange.from(request), ex -> {
                called.set(true);
                return Mono.empty();
            }).block();
            assertThat(called).isTrue();
        }
        var called = new AtomicBoolean();
        new OcppAuthenticationChallengeFilter(false).filter(MockServerWebExchange.from(
                MockServerHttpRequest.get("/ocpp/STATION-1").header(HttpHeaders.UPGRADE, "websocket")), ex -> {
            called.set(true);
            return Mono.empty();
        }).block();
        assertThat(called).isTrue();
    }
}
