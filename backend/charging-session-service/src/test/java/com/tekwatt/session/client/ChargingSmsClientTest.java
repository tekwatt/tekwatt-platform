package com.tekwatt.session.client;

import com.tekwatt.session.entity.ChargingSession;
import com.tekwatt.session.entity.SessionStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ChargingSmsClientTest {
    @Test void sendsStartAndStopToCustomerWithSeparateTemplatesAndAmounts() {
        var builder = RestClient.builder().baseUrl("http://test");
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new ChargingSmsClient(builder.build(), builder.build(), builder.build());
        var session = session();
        for (boolean started : new boolean[]{true, false}) {
            String key = started ? "charging-started" : "charging-completed";
            server.expect(requestTo("http://test/api/v1/users/" + session.getUserId()))
                    .andRespond(withSuccess("{\"tenantId\":\"" + session.getTenantId() + "\",\"phone\":\"9876868696\"}", MediaType.APPLICATION_JSON));
            server.expect(requestTo("http://test/api/v1/chargers/" + session.getChargerId()))
                    .andRespond(withSuccess("{\"tenantId\":\"" + session.getTenantId() + "\",\"stationId\":\"SIM-001\",\"stationName\":\"Chennai Hub\"}", MediaType.APPLICATION_JSON));
            server.expect(requestTo("http://test/api/v1/notifications")).andExpect(method(POST))
                    .andExpect(jsonPath("$.recipient").value("9876868696"))
                    .andExpect(jsonPath("$.templateKey").value(key))
                    .andExpect(jsonPath("$.idempotencyKey").value((started ? "charging-started:" : "charging-stopped:") + session.getId()))
                    .andExpect(jsonPath("$.body", org.hamcrest.Matchers.containsString("Chennai Hub")))
                    .andExpect(jsonPath("$.body", org.hamcrest.Matchers.containsString(started ? "Charging started" : "Energy 1.000 kWh. Amount INR 10.00")))
                    .andRespond(withSuccess("{\"id\":\"" + UUID.randomUUID() + "\",\"status\":\"QUEUED\"}", MediaType.APPLICATION_JSON));
            // Queue response id is unpredictable in this test; match any notification send URL.
            server.expect(requestTo(org.hamcrest.Matchers.matchesPattern("http://test/api/v1/notifications/[0-9a-f-]+/send")))
                    .andExpect(method(POST)).andRespond(withSuccess("{\"status\":\"SENT\"}", MediaType.APPLICATION_JSON));
        }
        client.sendStarted(session);
        client.sendStopped(session);
        server.verify();
    }

    @Test void refusesCustomerFromAnotherWorkspace() {
        var builder = RestClient.builder().baseUrl("http://test");
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new ChargingSmsClient(builder.build(), builder.build(), builder.build());
        var session = session();
        server.expect(requestTo("http://test/api/v1/users/" + session.getUserId()))
                .andRespond(withSuccess("{\"tenantId\":\"" + UUID.randomUUID() + "\",\"phone\":\"9876868696\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.sendStarted(session)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("workspace mismatch");
        server.verify();
    }

    private static ChargingSession session() {
        var session = new ChargingSession(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "TRX-1", BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "INR");
        session.stop(new BigDecimal("1000"), SessionStatus.COMPLETED);
        return session;
    }
}
