package com.tekwatt.session.service;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CurrentCustomerSessionServiceTest {
    private final UUID userId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();
    private final ChargingSessionService sessions = mock(ChargingSessionService.class);
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final CurrentCustomerSessionService current = new CurrentCustomerSessionService(sessions, builder, "https://users.test");

    @Test
    void listsOnlyTheCustomerAndWorkspaceResolvedFromTheSession() {
        server.expect(once(), requestTo("https://users.test/api/v1/users/me"))
                .andExpect(header("Authorization", "Bearer valid-session"))
                .andRespond(withSuccess("{\"id\":\"" + userId + "\",\"tenantId\":\"" + tenantId + "\",\"status\":\"ACTIVE\"}", MediaType.APPLICATION_JSON));
        when(sessions.listForCustomer(tenantId, userId)).thenReturn(List.of());

        assertThat(current.mySessions("Bearer valid-session")).isEmpty();
        verify(sessions).listForCustomer(tenantId, userId);
        verifyNoMoreInteractions(sessions);
        server.verify();
    }

    @Test
    void rejectsInactiveProfileBeforeQueryingSessions() {
        server.expect(once(), requestTo("https://users.test/api/v1/users/me"))
                .andRespond(withSuccess("{\"id\":\"" + userId + "\",\"tenantId\":\"" + tenantId + "\",\"status\":\"INACTIVE\"}", MediaType.APPLICATION_JSON));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> current.mySessions("Bearer valid-session"));
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(sessions);
        server.verify();
    }

    @Test
    void rejectsInvalidLoginBeforeQueryingSessions() {
        server.expect(once(), requestTo("https://users.test/api/v1/users/me"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> current.mySessions("Bearer expired"));
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(sessions);
        server.verify();
    }
}
