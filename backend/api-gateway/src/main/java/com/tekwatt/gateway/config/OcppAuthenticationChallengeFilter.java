package com.tekwatt.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Challenges browsers before the proxy commits its client-side WebSocket upgrade. */
@Component
public class OcppAuthenticationChallengeFilter implements GlobalFilter, Ordered {
    private final boolean requireCredentials;

    public OcppAuthenticationChallengeFilter(
            @Value("${OCPP_REQUIRE_CREDENTIALS:false}") boolean requireCredentials) {
        this.requireCredentials = requireCredentials;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        var headers = request.getHeaders();
        boolean ocppUpgrade = request.getMethod() == HttpMethod.GET
                && request.getPath().pathWithinApplication().value().startsWith("/ocpp/")
                && "websocket".equalsIgnoreCase(headers.getUpgrade());
        if (requireCredentials && ocppUpgrade
                && !StringUtils.hasText(headers.getFirst(HttpHeaders.AUTHORIZATION))
                && !StringUtils.hasText(headers.getFirst("X-OCPP-Key"))) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE,
                    "Basic realm=\"TekWatt OCPP\", charset=\"UTF-8\"");
            exchange.getResponse().getHeaders().setCacheControl("no-store");
            return exchange.getResponse().setComplete();
        }
        // Credential validity remains the responsibility of the OCPP gateway.
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
