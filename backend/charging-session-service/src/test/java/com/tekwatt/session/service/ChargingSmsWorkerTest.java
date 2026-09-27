package com.tekwatt.session.service;

import com.tekwatt.session.client.ChargingSmsClient;
import com.tekwatt.session.entity.ChargingSession;
import com.tekwatt.session.repository.ChargingSessionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChargingSmsWorkerTest {
    @Test void failedDeliveryStaysPendingAndRetryCompletesWithoutBlockingStop() {
        var repo = mock(ChargingSessionRepository.class);
        var sms = mock(ChargingSmsClient.class);
        var session = mock(ChargingSession.class);
        var id = UUID.randomUUID();
        when(repo.pendingStartedSms(any(), any())).thenReturn(List.of(id));
        when(repo.pendingStoppedSms(any(), any())).thenReturn(List.of(id));
        when(repo.claimStartedSms(eq(id), any(), any())).thenReturn(1);
        when(repo.claimStoppedSms(eq(id), any(), any())).thenReturn(1);
        when(repo.findById(id)).thenReturn(Optional.of(session));
        doThrow(new IllegalStateException("provider unavailable")).doNothing().when(sms).sendStarted(session);
        var worker = new ChargingSmsWorker(repo, sms);
        worker.process();
        verify(repo, never()).completeStartedSms(id);
        verify(repo).completeStoppedSms(id);
        worker.process();
        verify(repo).completeStartedSms(id);
    }
}
