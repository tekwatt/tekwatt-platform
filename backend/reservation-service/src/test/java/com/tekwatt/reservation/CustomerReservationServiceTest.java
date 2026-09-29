package com.tekwatt.reservation;

import com.tekwatt.reservation.dto.CustomerReservationRequest;
import com.tekwatt.reservation.dto.ReservationRequest;
import com.tekwatt.reservation.dto.ReservationResponse;
import com.tekwatt.reservation.entity.ReservationStatus;
import com.tekwatt.reservation.service.CustomerReservationService;
import com.tekwatt.reservation.service.ReservationService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CustomerReservationServiceTest {
    @Test
    void createDerivesCustomerAndChecksAssignedConnector() {
        UUID tenant = UUID.randomUUID(), user = UUID.randomUUID(), charger = UUID.randomUUID(), connector = UUID.randomUUID();
        ReservationService reservations = mock(ReservationService.class);
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://users/api/v1/users/me"))
                .andRespond(withSuccess(profile(tenant, user, charger), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://connectors/api/v1/connectors/" + connector))
                .andRespond(withSuccess("{\"tenantId\":\"" + tenant + "\",\"chargerId\":\"" + charger
                        + "\",\"status\":\"AVAILABLE\"}", MediaType.APPLICATION_JSON));
        var service = new CustomerReservationService(reservations, builder, "http://users", "http://connectors");
        Instant starts = Instant.now().plusSeconds(30), expires = starts.plusSeconds(1800);
        service.create("Bearer customer", new CustomerReservationRequest(charger, connector, starts, expires));
        var captured = ArgumentCaptor.forClass(ReservationRequest.class);
        verify(reservations).create(captured.capture());
        assertThat(captured.getValue().tenantId()).isEqualTo(tenant);
        assertThat(captured.getValue().userId()).isEqualTo(user);
        assertThat(captured.getValue().chargerId()).isEqualTo(charger);
        assertThat(captured.getValue().connectorId()).isEqualTo(connector);
        server.verify();
    }

    @Test
    void cannotCancelAnotherCustomersReservation() {
        UUID tenant = UUID.randomUUID(), user = UUID.randomUUID(), charger = UUID.randomUUID(), id = UUID.randomUUID();
        ReservationService reservations = mock(ReservationService.class);
        when(reservations.get(id)).thenReturn(new ReservationResponse(id, tenant, UUID.randomUUID(), charger,
                UUID.randomUUID(), "ref", ReservationStatus.ACTIVE, Instant.now(), Instant.now().plusSeconds(1800),
                Instant.now(), Instant.now()));
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://users/api/v1/users/me"))
                .andRespond(withSuccess(profile(tenant, user, charger), MediaType.APPLICATION_JSON));
        var service = new CustomerReservationService(reservations, builder, "http://users", "http://connectors");
        assertThatThrownBy(() -> service.cancel("Bearer customer", id))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verify(reservations, never()).cancel(id);
        server.verify();
    }

    private String profile(UUID tenant, UUID user, UUID charger) {
        return "{\"id\":\"" + user + "\",\"tenantId\":\"" + tenant
                + "\",\"status\":\"ACTIVE\",\"assignedChargerIds\":[\"" + charger + "\"]}";
    }
}
