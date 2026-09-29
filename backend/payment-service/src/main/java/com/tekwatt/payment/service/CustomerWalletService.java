package com.tekwatt.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tekwatt.payment.entity.Wallet;
import com.tekwatt.payment.entity.WalletEntry;
import com.tekwatt.payment.repository.WalletEntryRepository;
import com.tekwatt.payment.repository.WalletRepository;
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

/** Wallet reads and creation are derived from the signed-in customer, never caller-supplied IDs. */
@Service
public class CustomerWalletService {
    private final WalletRepository wallets;
    private final WalletEntryRepository entries;
    private final RestClient users;

    public CustomerWalletService(WalletRepository wallets, WalletEntryRepository entries, RestClient.Builder builder,
            @Value("${tekwatt.services.user:http://localhost:8082}") String userUrl) {
        this.wallets = wallets;
        this.entries = entries;
        this.users = builder.baseUrl(userUrl).build();
    }

    @Transactional(readOnly = true)
    public Wallet mine(String authorization) {
        Customer customer = customer(authorization);
        return wallets.findByTenantIdAndUserId(customer.tenantId(), customer.userId()).orElse(null);
    }

    @Transactional
    public Wallet create(String authorization) {
        Customer customer = customer(authorization);
        return wallets.findByTenantIdAndUserId(customer.tenantId(), customer.userId())
                .orElseGet(() -> wallets.save(new Wallet(customer.tenantId(), customer.userId(), "INR")));
    }

    @Transactional(readOnly = true)
    public List<WalletEntry> entries(String authorization) {
        Wallet wallet = mine(authorization);
        return wallet == null ? List.of() : entries.findAllByWalletIdOrderByCreatedAtDesc(wallet.getId());
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
            return new Customer(UUID.fromString(profile.path("tenantId").asText()),
                    UUID.fromString(profile.path("id").asText()));
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer profile is invalid", error);
        }
    }

    private record Customer(UUID tenantId, UUID userId) { }
}
