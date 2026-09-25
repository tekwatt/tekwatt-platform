package com.tekwatt.ocpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:ocpi_docs;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false"
})
class OcpiOpenApiTest {
    @LocalServerPort int port;
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper json;

    @Test
    void publishesOcpiEndpointsWithTokenHeaderSecurity() throws Exception {
        String response = http.getForObject("http://localhost:" + port + "/v3/api-docs", String.class);
        JsonNode document = json.readTree(response);
        assertThat(document.path("paths").has("/ocpi/{tenantId}/versions")).isTrue();
        assertThat(document.path("paths").has("/ocpi/{tenantId}/2.2.1/locations")).isTrue();
        assertThat(document.path("paths").has("/ocpi/{tenantId}/2.2.1/credentials")).isTrue();
        assertThat(document.path("paths").has("/ocpi/{tenantId}/2.2.1/tokens")).isTrue();
        assertThat(document.path("paths").has("/ocpi/{tenantId}/2.2.1/tariffs")).isTrue();
        assertThat(document.path("paths").has("/ocpi/{tenantId}/2.2.1/sessions")).isTrue();
        assertThat(document.path("paths").has("/ocpi/{tenantId}/2.2.1/cdrs")).isTrue();
        assertThat(document.path("paths").has("/ocpi/{tenantId}/2.2.1/commands/{command}")).isTrue();
        assertThat(document.path("servers").get(0).path("url").asText()).isEqualTo("http://localhost:8080");
        JsonNode security = document.path("components").path("securitySchemes").path("ocpiToken");
        assertThat(security.path("type").asText()).isEqualTo("apiKey");
        assertThat(security.path("in").asText()).isEqualTo("header");
        assertThat(security.path("name").asText()).isEqualTo("Authorization");
    }
}
