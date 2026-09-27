package com.tekwatt.ocpp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tekwatt.ocpp.service.OcppPlatformBridge;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import java.time.Instant;
import java.util.UUID;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OcppRecoveryTest {
    @Test void lateFinalEventDoesNotResetTheNextCarsConnector() throws Exception {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var bridge=new OcppPlatformBridge(builder,"http://charger","http://connector","http://user","http://session","http://telemetry");
        var id=UUID.randomUUID();
        String session="{\"id\":\""+id+"\",\"status\":\"INTERRUPTED\",\"meterStartWh\":0,\"meterStopWh\":4767}";
        server.expect(requestTo("http://session/api/v1/charging-sessions/by-transaction/OLD-TX")).andRespond(withSuccess(session,MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://session/api/v1/charging-sessions/"+id+"/stop")).andExpect(content().json("{\"meterStopWh\":4800,\"status\":\"COMPLETED\"}")).andRespond(withSuccess());
        bridge.stopLegacy("ST-1",new ObjectMapper().readTree("{\"transactionId\":\"OLD-TX\",\"meterStop\":4800}"));
        server.verify();
    }
    @Test void available201ReconcilesOnlyItsConnectorBeforePublishingAvailability() throws Exception { status(true); }
    @Test void occupied201DoesNotReleaseAnySession() throws Exception { status(false); }
    void status(boolean available) throws Exception {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var bridge=new OcppPlatformBridge(builder,"http://charger","http://connector","http://user","http://session","http://telemetry");
        ReflectionTestUtils.setField(bridge,"recoveryKey","test-recovery");
        var charger=UUID.randomUUID();var connector=UUID.randomUUID();var tenant=UUID.randomUUID();var session=UUID.randomUUID();
        server.expect(requestTo("http://charger/api/v1/chargers/by-station/ST-1")).andRespond(withSuccess("{\"id\":\""+charger+"\",\"tenantId\":\""+tenant+"\"}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://connector/api/v1/connectors?chargerId="+charger)).andRespond(withSuccess("[{\"id\":\""+connector+"\",\"connectorNumber\":2}]",MediaType.APPLICATION_JSON));
        if(available){
            server.expect(requestTo("http://session/api/v1/charging-sessions?tenantId="+tenant)).andRespond(withSuccess("[{\"id\":\""+session+"\",\"connectorId\":\""+connector+"\",\"status\":\"ACTIVE\"}]",MediaType.APPLICATION_JSON));
            server.expect(requestTo("http://session/api/v1/charging-sessions/"+session+"/reconcile-available")).andExpect(method(HttpMethod.POST)).andExpect(header("X-Session-Recovery-Key","test-recovery")).andExpect(jsonPath("$.connectorId").value(connector.toString())).andRespond(withSuccess());
        }
        server.expect(requestTo("http://connector/api/v1/connectors/"+connector+"/status")).andExpect(content().json("{\"status\":\""+(available?"AVAILABLE":"PREPARING")+"\"}")).andRespond(withSuccess());
        server.expect(requestTo("http://charger/api/v1/chargers/"+charger+"/status")).andRespond(withSuccess());
        bridge.status("ST-1","ocpp2.0",new ObjectMapper().readTree("{\"connectorId\":2,\"evseId\":1,\"connectorStatus\":\""+(available?"Available":"Occupied")+"\",\"timestamp\":\""+Instant.now().minusSeconds(1)+"\"}"));
        server.verify();
    }
}
