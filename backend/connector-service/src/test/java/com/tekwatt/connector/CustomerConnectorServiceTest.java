package com.tekwatt.connector;

import com.tekwatt.connector.dto.ConnectorResponse;
import com.tekwatt.connector.service.ConnectorService;
import com.tekwatt.connector.service.CustomerConnectorService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CustomerConnectorServiceTest {
    @Test
    void onlyConnectorsFromAssignedChargerAndWorkspaceAreReturned() {
        UUID tenant = UUID.randomUUID(), assigned = UUID.randomUUID();
        ConnectorService connectors = mock(ConnectorService.class);
        ConnectorResponse own = mock(ConnectorResponse.class), wrongTenant = mock(ConnectorResponse.class);
        when(own.tenantId()).thenReturn(tenant);
        when(wrongTenant.tenantId()).thenReturn(UUID.randomUUID());
        when(connectors.list(assigned)).thenReturn(List.of(own, wrongTenant));
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://users/api/v1/users/me"))
                .andRespond(withSuccess(profile(tenant, assigned), MediaType.APPLICATION_JSON));
        var service = new CustomerConnectorService(connectors, builder, "http://users");
        assertThat(service.mine("Bearer customer", assigned)).containsExactly(own);
        server.verify();
    }

    @Test
    void unassignedChargerIsNotQueried() {
        UUID tenant = UUID.randomUUID(), assigned = UUID.randomUUID(), other = UUID.randomUUID();
        ConnectorService connectors = mock(ConnectorService.class);
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://users/api/v1/users/me"))
                .andRespond(withSuccess(profile(tenant, assigned), MediaType.APPLICATION_JSON));
        var service = new CustomerConnectorService(connectors, builder, "http://users");
        assertThatThrownBy(() -> service.mine("Bearer customer", other))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verifyNoInteractions(connectors);
        server.verify();
    }

    private String profile(UUID tenant, UUID charger) {
        return "{\"id\":\"" + UUID.randomUUID() + "\",\"tenantId\":\"" + tenant
                + "\",\"status\":\"ACTIVE\",\"assignedChargerIds\":[\"" + charger + "\"]}";
    }
}
