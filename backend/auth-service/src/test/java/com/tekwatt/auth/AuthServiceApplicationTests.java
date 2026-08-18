package com.tekwatt.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.*;
import java.util.*;

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

    @Test void listsCurrentSessionAndRevokesItOnLogout(){
        String email="sessions-"+UUID.randomUUID()+"@tekwatt.in";
        ResponseEntity<JsonNode> registered=http.postForEntity("/api/v1/auth/register",Map.of("email",email,"password","Tekwatt@12345"),JsonNode.class);
        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String access=registered.getBody().path("accessToken").asText(),refresh=registered.getBody().path("refreshToken").asText();
        HttpHeaders headers=new HttpHeaders();headers.setBearerAuth(access);
        ResponseEntity<JsonNode> sessions=http.exchange("/api/v1/auth/sessions",HttpMethod.GET,new HttpEntity<>(headers),JsonNode.class);
        assertThat(sessions.getStatusCode()).isEqualTo(HttpStatus.OK);assertThat(sessions.getBody().isArray()).isTrue();assertThat(sessions.getBody().size()).isEqualTo(1);assertThat(sessions.getBody().get(0).path("current").asBoolean()).isTrue();
        assertThat(http.postForEntity("/api/v1/auth/logout",Map.of("refreshToken",refresh),Void.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(http.exchange("/api/v1/auth/sessions",HttpMethod.GET,new HttpEntity<>(headers),JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
