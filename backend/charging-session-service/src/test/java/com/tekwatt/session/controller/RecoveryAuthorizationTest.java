package com.tekwatt.session.controller;
import com.tekwatt.session.service.ChargingSessionService;
import com.tekwatt.session.dto.ReconcileAvailableRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecoveryAuthorizationTest {
    @Test void deniesPublicRecoveryAndRequiresInternalKey() {
        var service=mock(ChargingSessionService.class);var controller=new ChargingSessionController(service,mock(com.tekwatt.session.service.CurrentCustomerSessionService.class));
        var id=UUID.randomUUID();var request=new ReconcileAvailableRequest(UUID.randomUUID(),UUID.randomUUID(),Instant.now());
        assertThrows(ResponseStatusException.class,()->controller.reconcileAvailable(id,"",request));
        ReflectionTestUtils.setField(controller,"recoveryKey","internal-test-key");
        assertThrows(ResponseStatusException.class,()->controller.reconcileAvailable(id,"wrong",request));
        verifyNoInteractions(service);
        controller.reconcileAvailable(id,"internal-test-key",request);
        verify(service).reconcileAvailable(id,request);
    }
}
