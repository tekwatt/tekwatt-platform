package com.tekwatt.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.WebFilter;
import java.util.concurrent.atomic.AtomicBoolean;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ApiGatewayApplicationTests {
    @Autowired WebTestClient webClient;
    @Autowired ApplicationContext context;

    @Test
    void contextLoads() {
        // Confirms the gateway configuration can be started by Spring Boot.
    }

    @Test
    void servesSwaggerUiAndAllServiceDefinitions() {
        webClient.get().uri("/swagger-ui/index.html").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body ->
                        org.assertj.core.api.Assertions.assertThat(body).contains("Swagger UI"));

        webClient.get().uri("/v3/api-docs/swagger-config").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.urls").isArray()
                .jsonPath("$.urls.length()").isEqualTo(22)
                .jsonPath("$.urls[?(@.name == 'Admin Service')].url")
                .isEqualTo("/openapi/admin/v3/api-docs")
                .jsonPath("$.urls[?(@.name == 'Support Service')].url")
                .isEqualTo("/openapi/support/v3/api-docs")
                .jsonPath("$.urls[?(@.name == 'OCPI 2.2.1 Service')].url")
                .isEqualTo("/openapi/ocpi/v3/api-docs");
    }

    @Test
    void permitsApprovedSimulatorOriginOnlyForOcppWebSocketHandshake() {
        String simulatorOrigin = "https://evcharger-simulator.com";
        WebFilter corsFilter = context.getBean("corsWebFilter", WebFilter.class);
        MockServerWebExchange ocppExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("https://gateway.test/ocpp/TKW-AM-SN-01-CH01")
                        .header(HttpHeaders.ORIGIN, simulatorOrigin));
        AtomicBoolean ocppChainCalled = new AtomicBoolean();

        corsFilter.filter(ocppExchange, exchange -> {
            ocppChainCalled.set(true);
            return Mono.empty();
        }).block();

        org.assertj.core.api.Assertions.assertThat(ocppChainCalled).isTrue();
        org.assertj.core.api.Assertions.assertThat(ocppExchange.getResponse().getHeaders()
                .getAccessControlAllowOrigin()).isEqualTo(simulatorOrigin);
        org.assertj.core.api.Assertions.assertThat(ocppExchange.getResponse().getHeaders()
                .getAccessControlAllowCredentials()).isTrue();

        MockServerWebExchange apiExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("https://gateway.test/api/v1/users")
                        .header(HttpHeaders.ORIGIN, simulatorOrigin));
        AtomicBoolean apiChainCalled = new AtomicBoolean();
        corsFilter.filter(apiExchange, exchange -> {
            apiChainCalled.set(true);
            return Mono.empty();
        }).block();

        org.assertj.core.api.Assertions.assertThat(apiChainCalled).isFalse();
        org.assertj.core.api.Assertions.assertThat(apiExchange.getResponse().getStatusCode().value())
                .isEqualTo(403);
    }
}
