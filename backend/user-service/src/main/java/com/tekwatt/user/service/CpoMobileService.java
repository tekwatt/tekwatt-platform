package com.tekwatt.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tekwatt.user.entity.Partner;
import com.tekwatt.user.repository.PartnerRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Mobile-facing CPO reads are scoped to the authenticated partner, not a caller-supplied tenant. */
@Service
public class CpoMobileService {
    private final PartnerRepository partners;
    private final RestClient auth;
    private final RestClient chargers;
    private final RestClient sessions;

    public CpoMobileService(PartnerRepository partners, RestClient.Builder builder,
            @Value("${tekwatt.services.auth:http://localhost:8081}") String authUrl,
            @Value("${tekwatt.services.charger:http://localhost:8083}") String chargerUrl,
            @Value("${tekwatt.services.charging-session:http://localhost:8084}") String sessionUrl) {
        this.partners = partners;
        this.auth = builder.clone().baseUrl(authUrl).build();
        this.chargers = builder.clone().baseUrl(chargerUrl).build();
        this.sessions = builder.clone().baseUrl(sessionUrl).build();
    }

    public CpoIdentity me(String authorization) {
        AuthIdentity account;
        try {
            account = auth.get().uri("/api/v1/auth/identity")
                    .header(HttpHeaders.AUTHORIZATION, authorization).retrieve().body(AuthIdentity.class);
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 401 || error.getStatusCode().value() == 403)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again", error);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity service is unavailable", error);
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity service is unavailable", error);
        }
        if (account == null || account.userId() == null)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity service returned no account");
        Partner partner = partners.findByAuthUserId(account.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No CPO account is linked to this login"));
        if (!"ACTIVE".equalsIgnoreCase(partner.getStatus()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CPO account is not active");
        return new CpoIdentity(partner.getId(), partner.getTenantId(), partner.getCompanyName(),
                partner.getContactName(), account.email());
    }

    public CpoOverview overview(String authorization) {
        CpoIdentity identity = me(authorization);
        JsonNode chargerData = fetch(chargers, "/api/v1/chargers?tenantId={tenantId}", identity.tenantId());
        List<CpoCharger> owned = new ArrayList<>();
        Set<String> chargerIds = new HashSet<>();
        for (JsonNode item : chargerData) {
            if (!identity.tenantId().toString().equals(item.path("tenantId").asText())
                    || !identity.partnerId().toString().equals(item.path("organizationId").asText())) continue;
            String id = item.path("id").asText();
            chargerIds.add(id);
            owned.add(new CpoCharger(id, item.path("stationId").asText(), item.path("stationName").asText(),
                    item.path("city").asText(), item.path("status").asText(),
                    item.path("powerKw").asDouble(), item.path("lastHeartbeat").asText(null)));
        }
        JsonNode sessionData = fetch(sessions, "/api/v1/charging-sessions?tenantId={tenantId}", identity.tenantId());
        List<CpoSession> related = new ArrayList<>();
        for (JsonNode item : sessionData) {
            if (!identity.tenantId().toString().equals(item.path("tenantId").asText())
                    || !chargerIds.contains(item.path("chargerId").asText())) continue;
            related.add(new CpoSession(item.path("id").asText(), item.path("chargerId").asText(),
                    item.path("transactionId").asText(), item.path("status").asText(),
                    item.path("energyKwh").asDouble(), item.path("startedAt").asText(null),
                    item.path("stoppedAt").asText(null)));
        }
        return new CpoOverview(identity, owned, related);
    }

    private JsonNode fetch(RestClient client, String path, UUID tenantId) {
        try {
            JsonNode body = client.get().uri(path, tenantId).retrieve().body(JsonNode.class);
            if (body == null || !body.isArray())
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Operator data is unavailable");
            return body;
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Operator data is unavailable", error);
        }
    }

    private record AuthIdentity(UUID userId, String email, String role) {}
    public record CpoIdentity(UUID partnerId, UUID tenantId, String companyName, String contactName, String email) {}
    public record CpoCharger(String id, String stationId, String stationName, String city, String status,
            double powerKw, String lastHeartbeat) {}
    public record CpoSession(String id, String chargerId, String transactionId, String status,
            double energyKwh, String startedAt, String stoppedAt) {}
    public record CpoOverview(CpoIdentity partner, List<CpoCharger> chargers, List<CpoSession> sessions) {}
}
