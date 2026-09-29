package com.tekwatt.gateway.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Rejects anonymous and revoked sessions before any application API is proxied. */
@Component
public class ApiSessionAuthenticationFilter implements GlobalFilter, Ordered {
    private final WebClient authClient;

    public ApiSessionAuthenticationFilter(WebClient.Builder builder,
            @Value("${AUTH_SERVICE_URL:http://localhost:8081}") String authServiceUrl) {
        this.authClient = builder.baseUrl(authServiceUrl).build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        var path = request.getPath().pathWithinApplication().value();
        if (request.getMethod() == HttpMethod.OPTIONS || !path.startsWith("/api/v1/") || isPublicAuth(path, request.getMethod())) {
            return chain.filter(exchange);
        }
        var authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank()) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }
        return authClient.get().uri("/api/v1/auth/identity")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .exchangeToMono(response -> {
                    if (response.statusCode().value() == 401 || response.statusCode().value() == 403)
                        return Mono.just(new IdentityCheck(HttpStatus.UNAUTHORIZED, null));
                    if (!response.statusCode().is2xxSuccessful())
                        return Mono.just(new IdentityCheck(HttpStatus.SERVICE_UNAVAILABLE, null));
                    return response.bodyToMono(JsonNode.class)
                            .map(body -> new IdentityCheck(HttpStatus.OK, body.path("role").asText("")))
                            .defaultIfEmpty(new IdentityCheck(HttpStatus.SERVICE_UNAVAILABLE, null));
                })
                .onErrorReturn(new IdentityCheck(HttpStatus.SERVICE_UNAVAILABLE, null))
                .flatMap(identity -> {
                    if (identity.status() != HttpStatus.OK) return reject(exchange, identity.status());
                    if (identity.role() == null || identity.role().isBlank())
                        return reject(exchange, HttpStatus.SERVICE_UNAVAILABLE);
                    if (isDriver(identity.role()) && isUnscopedDriverPath(path))
                        return reject(exchange, HttpStatus.FORBIDDEN);
                    return chain.filter(exchange);
                });
    }

    private static boolean isDriver(String role) {
        return "DRIVER".equalsIgnoreCase(role) || "CUSTOMER".equalsIgnoreCase(role);
    }

    private static boolean isUnscopedDriverPath(String path) {
        return (within(path, "/api/v1/payments") && !within(path, "/api/v1/payments/my"))
                || (within(path, "/api/v1/invoices") && !within(path, "/api/v1/invoices/my"))
                || (within(path, "/api/v1/bills") && !within(path, "/api/v1/bills/my"))
                || (within(path, "/api/v1/chargers") && !within(path, "/api/v1/chargers/my"))
                || (within(path, "/api/v1/reservations") && !within(path, "/api/v1/reservations/my"))
                || (within(path, "/api/v1/connectors") && !within(path, "/api/v1/connectors/my"))
                || within(path, "/api/v1/users/directory")
                || (within(path, "/api/v1/ocpp") && !within(path, "/api/v1/ocpp/customer-commands"))
                || (within(path, "/api/v1/charging-sessions") && !within(path, "/api/v1/charging-sessions/my"))
                || (within(path, "/api/v1/support/tickets") && !within(path, "/api/v1/support/tickets/my"));
    }

    private static boolean within(String path, String root) {
        return path.equals(root) || path.startsWith(root + "/");
    }

    private record IdentityCheck(HttpStatus status, String role) { }

    private static boolean isPublicAuth(String path, HttpMethod method) {
        if (method != HttpMethod.POST) return false;
        return path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/refresh") || path.equals("/api/v1/auth/password-reset/request")
                || path.equals("/api/v1/auth/password-reset/confirm");
    }

    private static Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setCacheControl("no-store");
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() { return -2; }
}
