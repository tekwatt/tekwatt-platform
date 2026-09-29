package com.tekwatt.billing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tekwatt.billing.dto.BillResponse;
import com.tekwatt.billing.repository.BillRepository;
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
public class CustomerBillService {
    private final BillRepository bills;
    private final RestClient users;

    public CustomerBillService(BillRepository bills, RestClient.Builder builder,
            @Value("${tekwatt.services.user:http://localhost:8082}") String userUrl) {
        this.bills = bills;
        users = builder.baseUrl(userUrl).build();
    }

    @Transactional(readOnly = true)
    public BillResponse mine(String authorization, UUID billId) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again");
        JsonNode customer;
        try {
            customer = users.get().uri("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve().body(JsonNode.class);
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 401 || error.getStatusCode().value() == 403)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again", error);
            if (error.getStatusCode().value() == 404)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customer account is required", error);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer identity is unavailable", error);
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer identity is unavailable", error);
        }
        if (customer == null || !"ACTIVE".equalsIgnoreCase(customer.path("status").asText()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Active customer account is required");
        return bills.findById(billId)
                .filter(bill -> bill.getUserId().toString().equals(customer.path("id").asText())
                        && bill.getTenantId().toString().equals(customer.path("tenantId").asText()))
                .map(BillResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found in your account"));
    }
}
