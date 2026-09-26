package com.tekwatt.session.service;

import com.tekwatt.session.client.SessionBillingClient;
import com.tekwatt.session.entity.ChargingSession;
import com.tekwatt.session.repository.ChargingSessionRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SessionBillingWorkerTest {
    @Test void failureRemainsQueuedAndSuccessfulRetryCompletes() {
        var repo = mock(ChargingSessionRepository.class); var client = mock(SessionBillingClient.class);
        var s = mock(ChargingSession.class); var id = UUID.randomUUID();
        when(repo.pendingBilling(any(),any())).thenReturn(List.of(id));
        when(repo.claimBilling(eq(id),any(),any())).thenReturn(1);
        when(repo.findById(id)).thenReturn(Optional.of(s));
        doThrow(new IllegalStateException()).doNothing().when(client).issue(s);
        var worker = new SessionBillingWorker(repo,client);
        worker.process(); verify(repo,never()).completeBilling(id);
        worker.process(); verify(repo).completeBilling(id);
    }
    @Test void anotherWorkerHoldingLeasePreventsProcessing() {
        var repo = mock(ChargingSessionRepository.class); var client = mock(SessionBillingClient.class);
        when(repo.pendingBilling(any(),any())).thenReturn(List.of(UUID.randomUUID()));
        new SessionBillingWorker(repo,client).process(); verifyNoInteractions(client);
    }
}
