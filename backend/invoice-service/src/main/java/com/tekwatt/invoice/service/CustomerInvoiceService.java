package com.tekwatt.invoice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tekwatt.invoice.dto.InvoiceResponse;
import com.tekwatt.invoice.repository.InvoiceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerInvoiceService {
    private final InvoiceRepository invoices;
    private final RestClient users;

    public CustomerInvoiceService(InvoiceRepository invoices, RestClient.Builder builder,
            @Value("${tekwatt.services.user:http://localhost:8082}") String userUrl) {
        this.invoices = invoices;
        this.users = builder.baseUrl(userUrl).build();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> mine(String authorization) {
        Customer customer = customer(authorization);
        return invoices.findAllByTenantIdAndUserIdOrderByCreatedAtDesc(customer.tenantId(), customer.userId())
                .stream().map(InvoiceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse mine(String authorization, UUID invoiceId) {
        Customer customer = customer(authorization);
        return invoices.findById(invoiceId)
                .filter(invoice -> customer.tenantId().equals(invoice.getTenantId())
                        && customer.userId().equals(invoice.getUserId()))
                .map(InvoiceResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found in your account"));
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
            throw unavailable(error);
        } catch (RestClientException error) { throw unavailable(error); }
        try {
            if (profile == null || !"ACTIVE".equalsIgnoreCase(profile.path("status").asText()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Active customer account is required");
            return new Customer(UUID.fromString(profile.path("tenantId").asText()),
                    UUID.fromString(profile.path("id").asText()));
        } catch (IllegalArgumentException error) { throw unavailable(error); }
    }

    private ResponseStatusException unavailable(Exception error) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer identity is unavailable", error);
    }

    private record Customer(UUID tenantId, UUID userId) { }
}
