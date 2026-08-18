package com.tekwatt.session.client;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TariffClient {
    private final RestClient client;
    public TariffClient(RestClient.Builder builder, @Value("${tekwatt.services.tariff:http://localhost:8088}") String baseUrl) { SimpleClientHttpRequestFactory requests=new SimpleClientHttpRequestFactory();requests.setConnectTimeout(2000);requests.setReadTimeout(3000);client = builder.baseUrl(baseUrl).requestFactory(requests).build(); }
    public ResolvedTariff resolve(UUID tenantId, UUID chargerId) {
        try {
            return client.get().uri(uri -> uri.path("/api/v1/tariffs/resolve").queryParam("tenantId", tenantId).queryParam("chargerId", chargerId).build()).retrieve().body(ResolvedTariff.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Assign an active tariff to this charger before starting a session");
        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Tariff service rejected pricing: " + e.getStatusText());
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Tariff service is unavailable");
        }
    }
    public record ResolvedTariff(UUID id, UUID tenantId, String code, String name, BigDecimal energyPricePerKwh, BigDecimal timePricePerMinute, BigDecimal sessionFee, BigDecimal taxPercent, String currency) {}
}
