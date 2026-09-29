package com.tekwatt.session.service;

import com.tekwatt.session.dto.SessionResponse;
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

/** Session reads for a driver are keyed by the server-verified customer profile. */
@Service
public class CurrentCustomerSessionService {
    private final ChargingSessionService sessions;
    private final RestClient users;

    public CurrentCustomerSessionService(ChargingSessionService sessions, RestClient.Builder builder,
            @Value("${tekwatt.services.user:http://localhost:8082}") String userUrl) {
        this.sessions = sessions;
        this.users = builder.baseUrl(userUrl).build();
    }

    public List<SessionResponse> mySessions(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again");
        Customer customer;
        try {
            customer = users.get().uri("/api/v1/users/me")
                    .header(HttpHeaders.AUTHORIZATION, authorization).retrieve().body(Customer.class);
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 401 || error.getStatusCode().value() == 403 || error.getStatusCode().value() == 404)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "An active customer profile is required", error);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer service is unavailable", error);
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer service is unavailable", error);
        }
        if (customer == null || customer.id() == null || customer.tenantId() == null || !"ACTIVE".equals(customer.status()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "An active customer profile is required");
        return sessions.listForCustomer(customer.tenantId(), customer.id());
    }

    private record Customer(UUID id, UUID tenantId, String status) { }
}
