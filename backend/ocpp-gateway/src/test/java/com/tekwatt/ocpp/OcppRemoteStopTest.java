package com.tekwatt.ocpp;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tekwatt.ocpp.dto.RemoteStopRequest;
import com.tekwatt.ocpp.service.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class OcppRemoteStopTest {
    @Test void version16SendsNumericTransaction() throws Exception { check("ocpp1.6","123",true); }
    @Test void version20SendsStringTransaction() throws Exception { check("ocpp2.0","TRX-123",false); }
    void check(String protocol,String transaction,boolean numeric) throws Exception {
        var registry=mock(ConnectionRegistry.class);when(registry.protocol("S1")).thenReturn(protocol);
        var mapper=new ObjectMapper();var service=new OcppCommandService(registry,mock(OcppAuditService.class),mapper,new OcppCommandTracker());
        service.remoteStop(new RemoteStopRequest("S1",protocol,transaction));
        var text=ArgumentCaptor.forClass(String.class);verify(registry).send(eq("S1"),text.capture());
        var frame=mapper.readTree(text.getValue());assertEquals(numeric?"RemoteStopTransaction":"RequestStopTransaction",frame.get(2).asText());
        assertEquals(numeric,frame.get(3).path("transactionId").isIntegralNumber());assertEquals(transaction,frame.get(3).path("transactionId").asText());
    }
    @Test void invalid16TransactionIsNotSent() throws Exception {
        var registry=mock(ConnectionRegistry.class);when(registry.protocol("S1")).thenReturn("ocpp1.6");
        var service=new OcppCommandService(registry,mock(OcppAuditService.class),new ObjectMapper(),new OcppCommandTracker());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.remoteStop(new RemoteStopRequest("S1","ocpp1.6","TRX-bad")));
        verify(registry,never()).send(anyString(),anyString());
    }
}
