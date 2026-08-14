package com.tekwatt.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthServiceApplicationTests {
    @Autowired TestRestTemplate http;

    @Test void contextLoads() { }

    @Test
    void exposesOpenApiWithBearerSecurity() {
        ResponseEntity<JsonNode> response = http.getForEntity("/v3/api-docs", JsonNode.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode document = response.getBody();
        assertThat(document).isNotNull();
        assertThat(document.path("info").path("title").asText()).isEqualTo("Auth Service API");
        assertThat(document.path("paths").has("/api/v1/auth/login")).isTrue();
        assertThat(document.path("components").path("securitySchemes").path("bearerAuth")
                .path("scheme").asText()).isEqualTo("bearer");
    }
}
