package com.tekwatt.charger;

import com.tekwatt.charger.dto.ChargerResponse;
import com.tekwatt.charger.service.ChargerService;
import com.tekwatt.charger.service.CustomerChargerService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CustomerChargerServiceTest {
    @Test
    void onlyAssignedChargersInCustomersWorkspaceAreReturned() {
        UUID tenant = UUID.randomUUID(), assignedId = UUID.randomUUID(), otherId = UUID.randomUUID();
        ChargerService chargers = mock(ChargerService.class);
        ChargerResponse assigned = mock(ChargerResponse.class), other = mock(ChargerResponse.class);
        when(assigned.id()).thenReturn(assignedId);
        when(other.id()).thenReturn(otherId);
        when(chargers.list(tenant)).thenReturn(List.of(assigned, other));
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://users/api/v1/users/me"))
                .andRespond(withSuccess("{\"id\":\"" + UUID.randomUUID() + "\",\"tenantId\":\"" + tenant
                        + "\",\"status\":\"ACTIVE\",\"assignedChargerIds\":[\"" + assignedId + "\"]}", MediaType.APPLICATION_JSON));
        var service = new CustomerChargerService(chargers, builder, "http://users");
        assertThat(service.mine("Bearer customer")).containsExactly(assigned);
        server.verify();
    }
}
