package com.tekwatt.gateway.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
public class GatewayCorsConfiguration {

    @Bean
    CorsWebFilter corsWebFilter(@Value("${tekwatt.gateway.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration ocppWebSocketConfiguration = new CorsConfiguration();
        ocppWebSocketConfiguration.setAllowedOrigins(List.of("https://evcharger-simulator.com"));
        ocppWebSocketConfiguration.setAllowedMethods(List.of(
                HttpMethod.GET.name(), HttpMethod.OPTIONS.name()));
        ocppWebSocketConfiguration.setAllowedHeaders(List.of("*"));
        ocppWebSocketConfiguration.setAllowCredentials(true);
        ocppWebSocketConfiguration.setMaxAge(3600L);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(), HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name()));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE,
                "X-Request-Id", "X-Tenant-Id"));
        configuration.setExposedHeaders(List.of("X-Request-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        // Keep the REST APIs restricted to the configured frontend while allowing the
        // approved browser-based OCPP simulator to perform its WebSocket handshake.
        CorsConfigurationSource source = exchange -> {
            String path = exchange.getRequest().getPath().pathWithinApplication().value();
            return path.startsWith("/ocpp/") ? ocppWebSocketConfiguration : configuration;
        };
        return new CorsWebFilter(source);
    }
}
