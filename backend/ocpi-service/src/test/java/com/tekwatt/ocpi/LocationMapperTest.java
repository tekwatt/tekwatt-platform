package com.tekwatt.ocpi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LocationMapperTest {
    private final ObjectMapper json = new ObjectMapper();
    private final LocationMapper mapper = new LocationMapper();
    private final OcpiController.Party party = new OcpiController.Party("IN", "TKW", "TekWatt", "IND", "Asia/Kolkata");

    @Test
    void mapsValidStationAndConnector() throws Exception {
        var charger = json.readTree("""
                {"id":"11111111-1111-1111-1111-111111111111","tenantId":"tenant-a","address":"Main Street",
                "city":"Chennai","latitude":13.0827,"longitude":80.2707,"stationName":"City Hub",
                "status":"AVAILABLE","stationStatus":"ACTIVE","updatedAt":"2026-01-01T00:00:00Z"}
                """);
        var connectors = json.readTree("""
                [{"tenantId":"tenant-a","connectorNumber":1,"type":"CCS2","maxVoltage":400,
                "maxCurrent":125,"status":"AVAILABLE","updatedAt":"2026-01-02T00:00:00Z"}]
                """);
        Map<String, Object> location = mapper.map(charger, connectors, party);
        assertThat(location).containsEntry("country_code", "IN").containsEntry("country", "IND")
                .containsEntry("last_updated", "2026-01-02T00:00:00Z");
        var evses = (List<Map<String, Object>>) location.get("evses");
        assertThat(evses.getFirst()).containsEntry("status", "AVAILABLE");
        var mappedConnectors = (List<Map<String, Object>>) evses.getFirst().get("connectors");
        assertThat(mappedConnectors.getFirst()).containsEntry("standard", "IEC_62196_T2_COMBO")
                .containsEntry("power_type", "DC");
    }

    @Test
    void doesNotPublishIncompleteStations() throws Exception {
        var charger = json.readTree("""
                {"id":"11111111-1111-1111-1111-111111111111","address":"Main Street","city":"Chennai"}
                """);
        assertThat(mapper.map(charger, json.readTree("[]"), party)).isNull();
    }

    @Test
    void hashesTokensWithoutExposingThem() {
        String hash = OcpiProtocol.sha256("partner-secret");
        assertThat(OcpiProtocol.matches("partner-secret", hash)).isTrue();
        assertThat(OcpiProtocol.matches("wrong", hash)).isFalse();
    }
}
