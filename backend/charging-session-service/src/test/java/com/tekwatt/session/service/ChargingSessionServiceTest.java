package com.tekwatt.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tekwatt.session.client.TariffClient;
import com.tekwatt.session.dto.SessionResponse;
import com.tekwatt.session.dto.StartSessionRequest;
import com.tekwatt.session.entity.ChargingSession;
import com.tekwatt.session.entity.SessionStatus;
import com.tekwatt.session.repository.ChargingSessionRepository;
import com.tekwatt.session.repository.MeterReadingRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ChargingSessionServiceTest {
    @Mock private ChargingSessionRepository sessions;
    @Mock private MeterReadingRepository readings;
    @Mock private TariffClient tariffs;

    private ChargingSessionService service;
    private StartSessionRequest request;

    @BeforeEach
    void setUp() {
        service = new ChargingSessionService(sessions, readings, tariffs);
        request = new StartSessionRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "TX-100", new BigDecimal("100.000"), null, null);
    }

    @Test
    void identicalStopIsIdempotentButChangedReadingIsRejected() {
        var s = new ChargingSession(request.tenantId(),request.userId(),request.chargerId(),request.connectorId(),UUID.randomUUID(),"STOP-1",BigDecimal.ZERO,BigDecimal.TEN,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,"INR");
        when(sessions.findForUpdate(s.getId())).thenReturn(java.util.Optional.of(s));
        var stop = new com.tekwatt.session.dto.StopSessionRequest(new BigDecimal("1000"),SessionStatus.COMPLETED);
        var first = service.stop(s.getId(),stop);
        var second = service.stop(s.getId(),stop);
        assertThat(second.stoppedAt()).isEqualTo(first.stoppedAt());
        verify(readings,org.mockito.Mockito.times(1)).save(any());
        assertThrows(ResponseStatusException.class,()->service.stop(s.getId(),new com.tekwatt.session.dto.StopSessionRequest(new BigDecimal("2000"),SessionStatus.COMPLETED)));
    }

    @Test
    void rejectsKnownDuplicateTransactionBeforeResolvingTariff() {
        when(sessions.existsByTransactionId(request.transactionId())).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.start(request));

        assertConflict(exception, "Transaction ID already exists");
        verifyNoInteractions(tariffs);
        verify(sessions, never()).saveAndFlush(any());
    }

    @Test
    void rejectsConnectorThatAlreadyHasAnActiveSession() {
        when(sessions.existsByConnectorIdAndStatus(request.connectorId(), SessionStatus.ACTIVE)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.start(request));

        assertConflict(exception, "Connector already has an active session");
        verifyNoInteractions(tariffs);
        verify(sessions, never()).saveAndFlush(any());
    }

    @Test
    void translatesConcurrentDuplicateTransactionConstraintToConflict() {
        when(tariffs.resolve(request.tenantId(), request.chargerId())).thenReturn(tariff());
        when(sessions.saveAndFlush(any(ChargingSession.class))).thenThrow(new DataIntegrityViolationException(
                "insert failed", new SQLException("Duplicate entry 'TX-100' for key 'charging_sessions.transaction_id'")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.start(request));

        assertConflict(exception, "Transaction ID already exists");
    }

    @Test
    void translatesConcurrentActiveConnectorConstraintToConflict() {
        when(tariffs.resolve(request.tenantId(), request.chargerId())).thenReturn(tariff());
        when(sessions.saveAndFlush(any(ChargingSession.class))).thenThrow(new DataIntegrityViolationException(
                "insert failed", new SQLException("Duplicate entry for key 'uk_charging_sessions_active_connector'")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.start(request));

        assertConflict(exception, "Connector already has an active session");
    }

    @Test
    void startsSessionWithResolvedTariffSnapshot() {
        TariffClient.ResolvedTariff tariff = tariff();
        when(tariffs.resolve(request.tenantId(), request.chargerId())).thenReturn(tariff);
        when(sessions.saveAndFlush(any(ChargingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessionResponse response = service.start(request);

        assertThat(response.status()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(response.tariffId()).isEqualTo(tariff.id());
        assertThat(response.pricePerKwh()).isEqualByComparingTo(tariff.energyPricePerKwh());
    }

    @Test
    void freshAvailableReleasesSessionWithoutBillingAndLateEndKeepsStopTime() {
        var s = recoverySession();
        s.applyMeterValue(new BigDecimal("4767"));
        org.springframework.test.util.ReflectionTestUtils.setField(s,"updatedAt",java.time.Instant.now().minusSeconds(2));
        var result = service.reconcileAvailable(s.getId(), new com.tekwatt.session.dto.ReconcileAvailableRequest(s.getTenantId(),s.getConnectorId(),java.time.Instant.now()));
        assertThat(result.status()).isEqualTo(SessionStatus.INTERRUPTED);
        assertThat(result.meterStopWh()).isEqualByComparingTo("4767");
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(s,"activeConnectorId")).isNull();
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(s,"billingPending")).isEqualTo(false);
        var ended = service.stop(s.getId(),new com.tekwatt.session.dto.StopSessionRequest(new BigDecimal("4800"),SessionStatus.COMPLETED));
        assertThat(ended.stoppedAt()).isEqualTo(result.stoppedAt());
        assertThat(ended.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(s,"billingPending")).isEqualTo(true);
        service.stop(s.getId(),new com.tekwatt.session.dto.StopSessionRequest(new BigDecimal("4800"),SessionStatus.COMPLETED));
        verify(readings,org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void staleFutureAndWrongConnectorReportsCannotReleaseAnActiveSession() {
        var s = recoverySession();
        for (var at : java.util.List.of(s.getStartedAt().minusSeconds(1),java.time.Instant.now().plusSeconds(60)))
            assertThrows(ResponseStatusException.class,()->service.reconcileAvailable(s.getId(),new com.tekwatt.session.dto.ReconcileAvailableRequest(s.getTenantId(),s.getConnectorId(),at)));
        assertThrows(ResponseStatusException.class,()->service.reconcileAvailable(s.getId(),new com.tekwatt.session.dto.ReconcileAvailableRequest(s.getTenantId(),UUID.randomUUID(),java.time.Instant.now())));
        assertThat(s.getStatus()).isEqualTo(SessionStatus.ACTIVE);
    }

    @Test
    void recoveryIsIdempotentAndMeterCannotGoBackwards() {
        var s = recoverySession();
        s.applyMeterValue(new BigDecimal("4767"));
        org.springframework.test.util.ReflectionTestUtils.setField(s,"updatedAt",java.time.Instant.now().minusSeconds(2));
        var evidence = new com.tekwatt.session.dto.ReconcileAvailableRequest(s.getTenantId(),s.getConnectorId(),java.time.Instant.now());
        var first=service.reconcileAvailable(s.getId(),evidence);
        assertThat(service.reconcileAvailable(s.getId(),evidence).stoppedAt()).isEqualTo(first.stoppedAt());
        assertThrows(ResponseStatusException.class,()->service.stop(s.getId(),new com.tekwatt.session.dto.StopSessionRequest(BigDecimal.ZERO,SessionStatus.COMPLETED)));
        assertThat(s.getStatus()).isEqualTo(SessionStatus.INTERRUPTED);
    }

    private ChargingSession recoverySession() {
        var s = new ChargingSession(request.tenantId(),request.userId(),request.chargerId(),request.connectorId(),UUID.randomUUID(),"RECOVER-1",BigDecimal.ZERO,BigDecimal.TEN,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,"INR");
        when(sessions.findForUpdate(s.getId())).thenReturn(java.util.Optional.of(s));
        return s;
    }

    private TariffClient.ResolvedTariff tariff() {
        return new TariffClient.ResolvedTariff(
                UUID.randomUUID(), request.tenantId(), "STANDARD", "Standard",
                new BigDecimal("12.5000"), new BigDecimal("0.5000"),
                new BigDecimal("5.00"), new BigDecimal("18.00"), "INR");
    }

    private void assertConflict(ResponseStatusException exception, String reason) {
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getReason()).isEqualTo(reason);
    }
}
