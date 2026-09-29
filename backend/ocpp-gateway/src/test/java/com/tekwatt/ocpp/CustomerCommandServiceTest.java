package com.tekwatt.ocpp;

import com.tekwatt.ocpp.dto.RemoteStartRequest;
import com.tekwatt.ocpp.dto.RemoteStopRequest;
import com.tekwatt.ocpp.service.ConnectionRegistry;
import com.tekwatt.ocpp.service.CustomerCommandService;
import com.tekwatt.ocpp.service.OcppCommandService;
import com.tekwatt.ocpp.service.OcppCommandTracker;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CustomerCommandServiceTest {
    private final UUID userId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();
    private final UUID chargerId = UUID.randomUUID();
    private final UUID connectorId = UUID.randomUUID();
    private final OcppCommandService commands = mock(OcppCommandService.class);
    private final OcppCommandTracker tracker = mock(OcppCommandTracker.class);
    private final ConnectionRegistry connections = mock(ConnectionRegistry.class);
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final CustomerCommandService service = new CustomerCommandService(commands, connections, tracker, builder,
            "https://users.test", "https://chargers.test", "https://connectors.test", "https://sessions.test");

    private void customer(boolean assigned) {
        server.expect(once(), requestTo("https://users.test/api/v1/users/me"))
                .andExpect(header("Authorization", "Bearer test-session"))
                .andRespond(withSuccess("{\"id\":\"" + userId + "\",\"tenantId\":\"" + tenantId
                        + "\",\"status\":\"ACTIVE\",\"assignedChargerIds\":"
                        + (assigned ? "[\"" + chargerId + "\"]" : "[]") + "}", MediaType.APPLICATION_JSON));
    }

    @Test
    void refusesUnassignedChargerBeforeSendingRemoteStart() {
        customer(false);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.start("Bearer test-session", chargerId, connectorId));
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(commands, connections);
        server.verify();
    }

    @Test
    void startUsesServerVerifiedConnectorAndRfidRatherThanClientSuppliedValues() {
        customer(true);
        server.expect(once(), requestTo("https://chargers.test/api/v1/chargers/" + chargerId))
                .andRespond(withSuccess("{\"tenantId\":\"" + tenantId + "\",\"stationId\":\"STATION-1\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://connectors.test/api/v1/connectors/" + connectorId))
                .andRespond(withSuccess("{\"chargerId\":\"" + chargerId + "\",\"tenantId\":\"" + tenantId
                        + "\",\"connectorNumber\":2,\"status\":\"AVAILABLE\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://users.test/api/v1/users/directory/rfid-cards?tenantId=" + tenantId))
                .andRespond(withSuccess("[{\"tenantId\":\"" + tenantId + "\",\"userId\":\"" + userId
                        + "\",\"status\":\"ACTIVE\",\"cardUid\":\"CARD-1\"}]", MediaType.APPLICATION_JSON));
        when(connections.protocol("STATION-1")).thenReturn("ocpp2.0");
        when(commands.remoteStart(any(RemoteStartRequest.class))).thenReturn("command-id");

        assertThat(service.start("Bearer test-session", chargerId, connectorId)).isEqualTo("command-id");
        verify(commands).remoteStart(new RemoteStartRequest("STATION-1", "ocpp2.0", 2, "CARD-1"));
        verify(tracker).claimForCustomer("command-id", userId);
        server.verify();
    }

    @Test
    void refusesAnotherCustomersSessionBeforeSendingRemoteStop() {
        customer(true);
        UUID sessionId = UUID.randomUUID();
        server.expect(once(), requestTo("https://sessions.test/api/v1/charging-sessions/" + sessionId))
                .andRespond(withSuccess("{\"tenantId\":\"" + tenantId + "\",\"userId\":\"" + UUID.randomUUID()
                        + "\",\"chargerId\":\"" + chargerId + "\",\"status\":\"ACTIVE\"}", MediaType.APPLICATION_JSON));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.stop("Bearer test-session", sessionId));
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(commands, connections);
        server.verify();
    }

    @Test
    void stopUsesTheOwnedSessionTransaction() {
        customer(true);
        UUID sessionId = UUID.randomUUID();
        server.expect(once(), requestTo("https://sessions.test/api/v1/charging-sessions/" + sessionId))
                .andRespond(withSuccess("{\"tenantId\":\"" + tenantId + "\",\"userId\":\"" + userId
                        + "\",\"chargerId\":\"" + chargerId + "\",\"transactionId\":\"TX-1\",\"status\":\"ACTIVE\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://chargers.test/api/v1/chargers/" + chargerId))
                .andRespond(withSuccess("{\"tenantId\":\"" + tenantId + "\",\"stationId\":\"STATION-1\"}", MediaType.APPLICATION_JSON));
        when(connections.protocol("STATION-1")).thenReturn("ocpp2.0");
        when(commands.remoteStop(any(RemoteStopRequest.class))).thenReturn("stop-command");

        assertThat(service.stop("Bearer test-session", sessionId)).isEqualTo("stop-command");
        verify(commands).remoteStop(new RemoteStopRequest("STATION-1", "ocpp2.0", "TX-1"));
        server.verify();
    }
}
