package com.tekwatt.support.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tekwatt.support.dto.AddCommentRequest;
import com.tekwatt.support.dto.CreateTicketRequest;
import com.tekwatt.support.dto.CustomerTicketRequest;
import com.tekwatt.support.dto.TicketDetailResponse;
import com.tekwatt.support.entity.SupportTicket;
import com.tekwatt.support.entity.TicketComment;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Customer support actions never accept requester identity or internal-note flags from the phone. */
@Service
public class CustomerSupportService {
    private final SupportTicketService tickets;
    private final RestClient users;

    public CustomerSupportService(SupportTicketService tickets, RestClient.Builder builder,
            @Value("${tekwatt.services.user:http://localhost:8082}") String userUrl) {
        this.tickets = tickets;
        this.users = builder.baseUrl(userUrl).build();
    }

    public List<SupportTicket> mine(String authorization) {
        Customer customer = customer(authorization);
        return tickets.list(customer.tenantId(), null).stream().filter(ticket -> owns(customer, ticket)).toList();
    }

    public SupportTicket create(String authorization, CustomerTicketRequest request) {
        Customer customer = customer(authorization);
        return tickets.create(new CreateTicketRequest(customer.tenantId(), customer.userId(), customer.name(),
                customer.email(), request.subject(), request.description(), request.category(), request.priority(),
                null, null));
    }

    public TicketDetailResponse detail(String authorization, UUID ticketId) {
        Customer customer = customer(authorization);
        TicketDetailResponse detail = tickets.detail(ticketId);
        if (!owns(customer, detail.ticket()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found in your account");
        return new TicketDetailResponse(detail.ticket(), detail.comments().stream()
                .filter(comment -> !comment.isInternalNote()).toList(), List.of());
    }

    public TicketComment comment(String authorization, UUID ticketId, String body) {
        Customer customer = customer(authorization);
        SupportTicket ticket = tickets.detail(ticketId).ticket();
        if (!owns(customer, ticket))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found in your account");
        if (body == null || body.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reply is required");
        return tickets.comment(ticketId, new AddCommentRequest(customer.userId(), customer.name(), body.trim(), false));
    }

    private boolean owns(Customer customer, SupportTicket ticket) {
        return customer.tenantId().equals(ticket.getTenantId())
                && (customer.userId().equals(ticket.getRequesterId())
                        || (ticket.getRequesterId() == null
                                && customer.email().equalsIgnoreCase(ticket.getRequesterEmail())));
    }

    private Customer customer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again");
        JsonNode profile;
        try {
            profile = users.get().uri("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve().body(JsonNode.class);
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 401 || error.getStatusCode().value() == 403)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again", error);
            if (error.getStatusCode().value() == 404)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customer account is required", error);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer service is unavailable", error);
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer service is unavailable", error);
        }
        try {
            if (profile == null || !"ACTIVE".equalsIgnoreCase(profile.path("status").asText()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Active customer account is required");
            String email = profile.path("email").asText();
            if (email.isBlank()) throw new IllegalArgumentException("Missing customer email");
            String name = profile.path("fullName").asText().trim();
            if (name.isBlank()) name = email;
            return new Customer(UUID.fromString(profile.path("tenantId").asText()),
                    UUID.fromString(profile.path("id").asText()), email, name);
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer profile is invalid", error);
        }
    }

    private record Customer(UUID tenantId, UUID userId, String email, String name) { }
}
