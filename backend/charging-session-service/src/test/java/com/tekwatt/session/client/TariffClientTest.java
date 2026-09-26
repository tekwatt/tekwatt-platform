package com.tekwatt.session.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class TariffClientTest {
    private final UUID tenantId = UUID.randomUUID();
    private final UUID chargerId = UUID.randomUUID();
    private MockRestServiceServer server;
    private TariffClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://tariff.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TariffClient(builder.build());
    }

    @Test
    void missingAssignmentIsAnActionableConflict() {
        server.expect(requestTo(resolveUrl())).andRespond(withStatus(HttpStatus.NOT_FOUND));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> client.resolve(tenantId, chargerId));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getReason()).isEqualTo("Assign an active tariff to this charger before starting a session");
        server.verify();
    }

    @Test
    void preservesDownstreamConflictDetail() {
        server.expect(requestTo(resolveUrl())).andRespond(withStatus(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body("{\"status\":409,\"detail\":\"Assigned tariff expired at 2026-09-01T00:00:00Z\"}"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> client.resolve(tenantId, chargerId));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getReason()).isEqualTo("Assigned tariff expired at 2026-09-01T00:00:00Z");
        server.verify();
    }

    @Test
    void givesUsefulFallbackWhenDownstreamConflictHasNoDetail() {
        server.expect(requestTo(resolveUrl())).andRespond(withStatus(HttpStatus.CONFLICT));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> client.resolve(tenantId, chargerId));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getReason()).isEqualTo("Assigned tariff is not active for this time");
        server.verify();
    }

    private String resolveUrl() {
        return "http://tariff.test/api/v1/tariffs/resolve?tenantId=" + tenantId + "&chargerId=" + chargerId;
    }
}
