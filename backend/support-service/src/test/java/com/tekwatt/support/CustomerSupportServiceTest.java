package com.tekwatt.support;

import com.tekwatt.support.dto.AddCommentRequest;
import com.tekwatt.support.dto.CreateTicketRequest;
import com.tekwatt.support.dto.CustomerTicketRequest;
import com.tekwatt.support.dto.TicketDetailResponse;
import com.tekwatt.support.entity.SupportTicket;
import com.tekwatt.support.entity.TicketComment;
import com.tekwatt.support.entity.TicketPriority;
import com.tekwatt.support.service.CustomerSupportService;
import com.tekwatt.support.service.SupportTicketService;
import java.util.List;
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

class CustomerSupportServiceTest {
    @Test
    void createDerivesRequesterFromSignedInProfile() {
        UUID tenant = UUID.randomUUID(), user = UUID.randomUUID();
        SupportTicketService tickets = mock(SupportTicketService.class);
        var builder = RestClient.builder();
        var server = profileServer(builder, tenant, user);
        var service = new CustomerSupportService(tickets, builder, "http://users");

        service.create("Bearer customer", new CustomerTicketRequest("Help", "Charging stopped", "CHARGING", TicketPriority.HIGH));

        var captured = ArgumentCaptor.forClass(CreateTicketRequest.class);
        verify(tickets).create(captured.capture());
        assertThat(captured.getValue().tenantId()).isEqualTo(tenant);
        assertThat(captured.getValue().requesterId()).isEqualTo(user);
        assertThat(captured.getValue().requesterName()).isEqualTo("Amul Kumar");
        assertThat(captured.getValue().requesterEmail()).isEqualTo("amul@tekwatt.in");
        server.verify();
    }

    @Test
    void detailHidesInternalCommentsAndRejectsForeignTicket() {
        UUID tenant = UUID.randomUUID(), user = UUID.randomUUID(), id = UUID.randomUUID();
        SupportTicketService tickets = mock(SupportTicketService.class);
        var own = ticket(tenant, user);
        var publicComment = new TicketComment(id, user, "Amul", "Please help", false);
        var internalComment = new TicketComment(id, UUID.randomUUID(), "Agent", "Internal note", true);
        when(tickets.detail(id)).thenReturn(new TicketDetailResponse(own,
                List.of(publicComment, internalComment), List.of()));
        var builder = RestClient.builder();
        var server = profileServer(builder, tenant, user);
        var service = new CustomerSupportService(tickets, builder, "http://users");
        assertThat(service.detail("Bearer customer", id).comments()).containsExactly(publicComment);
        server.verify();

        var foreign = ticket(tenant, UUID.randomUUID());
        when(tickets.detail(id)).thenReturn(new TicketDetailResponse(foreign, List.of(), List.of()));
        var secondBuilder = RestClient.builder();
        var secondServer = profileServer(secondBuilder, tenant, user);
        var secondService = new CustomerSupportService(tickets, secondBuilder, "http://users");
        assertThatThrownBy(() -> secondService.detail("Bearer customer", id))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        secondServer.verify();
    }

    @Test
    void replyCannotBeMarkedInternal() {
        UUID tenant = UUID.randomUUID(), user = UUID.randomUUID(), id = UUID.randomUUID();
        SupportTicketService tickets = mock(SupportTicketService.class);
        when(tickets.detail(id)).thenReturn(new TicketDetailResponse(ticket(tenant, user), List.of(), List.of()));
        var builder = RestClient.builder();
        var server = profileServer(builder, tenant, user);
        var service = new CustomerSupportService(tickets, builder, "http://users");

        service.comment("Bearer customer", id, "  Please update me  ");

        var captured = ArgumentCaptor.forClass(AddCommentRequest.class);
        verify(tickets).comment(eq(id), captured.capture());
        assertThat(captured.getValue().authorId()).isEqualTo(user);
        assertThat(captured.getValue().body()).isEqualTo("Please update me");
        assertThat(captured.getValue().internalNote()).isFalse();
        server.verify();
    }

    private static SupportTicket ticket(UUID tenant, UUID requester) {
        return new SupportTicket(tenant, requester, "Amul Kumar", "amul@tekwatt.in",
                "Help", "Charging stopped", "CHARGING", TicketPriority.HIGH, null, null);
    }

    private static MockRestServiceServer profileServer(RestClient.Builder builder, UUID tenant, UUID user) {
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://users/api/v1/users/me"))
                .andRespond(withSuccess("{\"id\":\"" + user + "\",\"tenantId\":\"" + tenant
                        + "\",\"status\":\"ACTIVE\",\"fullName\":\"Amul Kumar\",\"email\":\"amul@tekwatt.in\"}",
                        MediaType.APPLICATION_JSON));
        return server;
    }
}
