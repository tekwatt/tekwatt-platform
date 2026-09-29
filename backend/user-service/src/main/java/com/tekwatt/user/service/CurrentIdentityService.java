package com.tekwatt.user.service;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentIdentityService {
    private final RestClient auth;

    public CurrentIdentityService(RestClient.Builder builder,
            @Value("${tekwatt.services.auth:http://localhost:8081}") String authUrl) {
        auth = builder.baseUrl(authUrl).build();
    }

    public Identity require(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again");
        try {
            Identity identity = auth.get().uri("/api/v1/auth/identity")
                    .header(HttpHeaders.AUTHORIZATION, authorization).retrieve().body(Identity.class);
            if (identity == null || identity.userId() == null)
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity service returned no account");
            return identity;
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 401 || error.getStatusCode().value() == 403)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again", error);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity service is unavailable", error);
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity service is unavailable", error);
        }
    }

    public record Identity(UUID userId, String email, String role) { }
}
