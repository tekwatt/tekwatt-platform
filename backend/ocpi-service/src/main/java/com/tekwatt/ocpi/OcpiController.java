package com.tekwatt.ocpi;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/ocpi/{tenantId}")
public class OcpiController {
    private final JdbcTemplate jdbc;
    private final LocationMapper locationMapper;
    private final RestClient client;
    private final String baseUrl;
    private final String chargerUrl;
    private final String connectorUrl;

    public OcpiController(JdbcTemplate jdbc, LocationMapper locationMapper,
            @Value("${tekwatt.ocpi.public-base-url}") String baseUrl,
            @Value("${tekwatt.ocpi.charger-service-url}") String chargerUrl,
            @Value("${tekwatt.ocpi.connector-service-url}") String connectorUrl) {
        this.jdbc = jdbc;
        this.locationMapper = locationMapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.chargerUrl = chargerUrl.replaceAll("/+$", "");
        this.connectorUrl = connectorUrl.replaceAll("/+$", "");
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    @GetMapping("/versions")
    public Map<String, Object> versions(@PathVariable UUID tenantId) {
        return OcpiProtocol.envelope(List.of(Map.of("version", "2.2.1",
                "url", baseUrl + "/ocpi/" + tenantId + "/2.2.1")));
    }

    @GetMapping("/2.2.1")
    public Map<String, Object> versionDetails(@PathVariable UUID tenantId) {
        return OcpiProtocol.envelope(Map.of("version", "2.2.1", "endpoints", List.of(
                Map.of("identifier", "locations", "role", "SENDER",
                        "url", baseUrl + "/ocpi/" + tenantId + "/2.2.1/locations"))));
    }

    @GetMapping("/2.2.1/locations")
    public ResponseEntity<Map<String, Object>> locations(@PathVariable UUID tenantId,
            @RequestParam(name = "date_from", required = false) String dateFrom,
            @RequestParam(name = "date_to", required = false) String dateTo,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        if (limit < 1 || limit > 1000 || offset < 0) throw new IllegalArgumentException("Invalid pagination parameters");
        Instant from = dateFrom == null ? Instant.MIN : OffsetDateTime.parse(dateFrom).toInstant();
        Instant to = dateTo == null ? Instant.MAX : OffsetDateTime.parse(dateTo).toInstant();
        Party party = party(tenantId);
        JsonNode chargers = client.get().uri(chargerUrl + "/api/v1/chargers?tenantId=" + tenantId)
                .retrieve().body(JsonNode.class);
        List<Map<String, Object>> all = new ArrayList<>();
        if (chargers != null && chargers.isArray()) {
            for (JsonNode charger : chargers) {
                Map<String, Object> location = mappedLocation(charger, party);
                if (location == null) continue;
                Instant updated = Instant.parse((String) location.get("last_updated"));
                if (!updated.isBefore(from) && updated.isBefore(to)) all.add(location);
            }
        }
        all.sort(Comparator.comparing(l -> (String) l.get("id")));
        int end = (int) Math.min((long) offset + limit, all.size());
        List<Map<String, Object>> page = offset >= all.size() ? List.of() : all.subList(offset, end);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Integer.toString(all.size()));
        headers.add("X-Limit", Integer.toString(limit));
        if (end < all.size()) {
            String next = baseUrl + "/ocpi/" + tenantId + "/2.2.1/locations?limit=" + limit + "&offset=" + end;
            if (dateFrom != null) next += "&date_from=" + URLEncoder.encode(dateFrom, StandardCharsets.UTF_8);
            if (dateTo != null) next += "&date_to=" + URLEncoder.encode(dateTo, StandardCharsets.UTF_8);
            headers.add(HttpHeaders.LINK, "<" + next + ">; rel=next");
        }
        return ResponseEntity.ok().headers(headers).body(OcpiProtocol.envelope(page));
    }

    @GetMapping("/2.2.1/locations/{countryCode}/{partyId}/{locationId}")
    public Map<String, Object> location(@PathVariable UUID tenantId, @PathVariable String countryCode,
            @PathVariable String partyId, @PathVariable UUID locationId) {
        return OcpiProtocol.envelope(loadLocation(tenantId, countryCode, partyId, locationId));
    }

    @GetMapping("/2.2.1/locations/{countryCode}/{partyId}/{locationId}/{evseUid}")
    public Map<String, Object> evse(@PathVariable UUID tenantId, @PathVariable String countryCode,
            @PathVariable String partyId, @PathVariable UUID locationId, @PathVariable String evseUid) {
        Map<String, Object> location = loadLocation(tenantId, countryCode, partyId, locationId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> evses = (List<Map<String, Object>>) location.get("evses");
        return OcpiProtocol.envelope(evses.stream().filter(e -> evseUid.equals(e.get("uid")))
                .findFirst().orElseThrow(MissingLocation::new));
    }

    @GetMapping("/2.2.1/locations/{countryCode}/{partyId}/{locationId}/{evseUid}/{connectorId}")
    public Map<String, Object> connector(@PathVariable UUID tenantId, @PathVariable String countryCode,
            @PathVariable String partyId, @PathVariable UUID locationId, @PathVariable String evseUid,
            @PathVariable String connectorId) {
        Map<String, Object> location = loadLocation(tenantId, countryCode, partyId, locationId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> evses = (List<Map<String, Object>>) location.get("evses");
        Map<String, Object> evse = evses.stream().filter(e -> evseUid.equals(e.get("uid")))
                .findFirst().orElseThrow(MissingLocation::new);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> connectors = (List<Map<String, Object>>) evse.get("connectors");
        return OcpiProtocol.envelope(connectors.stream().filter(c -> connectorId.equals(c.get("id")))
                .findFirst().orElseThrow(MissingLocation::new));
    }

    private Map<String, Object> loadLocation(UUID tenantId, String countryCode, String partyId, UUID locationId) {
        Party party = party(tenantId);
        if (!countryCode.equalsIgnoreCase(party.countryCode()) || !partyId.equalsIgnoreCase(party.partyId()))
            throw new MissingLocation();
        JsonNode charger = client.get().uri(chargerUrl + "/api/v1/chargers/" + locationId)
                .retrieve().onStatus(status -> status.value() == 404, (request, response) -> { throw new MissingLocation(); })
                .body(JsonNode.class);
        if (charger == null || !tenantId.toString().equals(charger.path("tenantId").asText())) throw new MissingLocation();
        Map<String, Object> result = mappedLocation(charger, party);
        if (result == null) throw new MissingLocation();
        return result;
    }

    private Map<String, Object> mappedLocation(JsonNode charger, Party party) {
        String chargerId = charger.path("id").asText();
        if (chargerId.isBlank()) return null;
        JsonNode connectors = client.get().uri(connectorUrl + "/api/v1/connectors?chargerId=" + chargerId)
                .retrieve().body(JsonNode.class);
        if (connectors != null && connectors.isArray()) {
            var sameTenant = new com.fasterxml.jackson.databind.node.ArrayNode(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance);
            connectors.forEach(connector -> {
                if (charger.path("tenantId").asText().equals(connector.path("tenantId").asText())) sameTenant.add(connector);
            });
            connectors = sameTenant;
        }
        return locationMapper.map(charger, connectors, party);
    }

    private Party party(UUID tenantId) {
        return jdbc.query("SELECT country_code, party_id, business_name, country, time_zone FROM ocpi_parties WHERE tenant_id = ? AND enabled = TRUE",
                (rs, row) -> new Party(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)),
                tenantId.toString()).stream().findFirst().orElseThrow(MissingLocation::new);
    }

    record Party(String countryCode, String partyId, String businessName, String country, String timeZone) {}

    static class MissingLocation extends RuntimeException {}

    @ExceptionHandler(MissingLocation.class)
    public ResponseEntity<Map<String, Object>> missing() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status_code", 2003,
                "status_message", "Unknown location", "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest() {
        return ResponseEntity.badRequest().body(Map.of("status_code", 2001,
                "status_message", "Invalid OCPI request", "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unavailable() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status_code", 3000,
                "status_message", "OCPI data is temporarily unavailable", "timestamp", Instant.now().toString()));
    }
}
