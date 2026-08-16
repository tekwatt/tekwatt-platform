package com.tekwatt.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ApiGatewayApplicationTests {
    @Autowired WebTestClient webClient;

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
                .jsonPath("$.urls.length()").isEqualTo(21)
                .jsonPath("$.urls[?(@.name == 'Admin Service')].url")
                .isEqualTo("/openapi/admin/v3/api-docs")
                .jsonPath("$.urls[?(@.name == 'Support Service')].url")
                .isEqualTo("/openapi/support/v3/api-docs");
    }
}
