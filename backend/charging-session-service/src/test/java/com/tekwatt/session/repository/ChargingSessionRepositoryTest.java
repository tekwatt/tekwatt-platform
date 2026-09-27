package com.tekwatt.session.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tekwatt.session.entity.ChargingSession;
import com.tekwatt.session.entity.SessionStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class ChargingSessionRepositoryTest {
    @Autowired private ChargingSessionRepository sessions;
    @Autowired private EntityManager entityManager;

    @Test
    void billingQueueIsDurableAndClaimsAreExclusive() {
        var completed = session(UUID.randomUUID(), "BILLING-1");
        completed.stop(BigDecimal.TEN, SessionStatus.COMPLETED);
        sessions.saveAndFlush(completed);
        var now = java.time.Instant.now().plusSeconds(1);
        assertThat(sessions.pendingBilling(now, org.springframework.data.domain.PageRequest.of(0,20))).contains(completed.getId());
        assertThat(sessions.claimBilling(completed.getId(), now, now.plusSeconds(120))).isEqualTo(1);
        assertThat(sessions.claimBilling(completed.getId(), now, now.plusSeconds(120))).isZero();
        sessions.completeBilling(completed.getId());
        assertThat(sessions.pendingBilling(now.plusSeconds(121), org.springframework.data.domain.PageRequest.of(0,20))).isEmpty();
    }

    @Test
    void chargingSmsQueuesAreDurableAndIdempotentlyClaimed() {
        var charging = session(UUID.randomUUID(), "SMS-1");
        sessions.saveAndFlush(charging);
        var now = java.time.Instant.now().plusSeconds(1);
        var page = org.springframework.data.domain.PageRequest.of(0, 20);
        assertThat(sessions.pendingStartedSms(now, page)).contains(charging.getId());
        assertThat(sessions.pendingStoppedSms(now, page)).isEmpty();
        assertThat(sessions.claimStartedSms(charging.getId(), now, now.plusSeconds(300))).isEqualTo(1);
        assertThat(sessions.claimStartedSms(charging.getId(), now, now.plusSeconds(300))).isZero();
        sessions.completeStartedSms(charging.getId());
        entityManager.clear();
        charging = sessions.findById(charging.getId()).orElseThrow();
        charging.stop(BigDecimal.TEN, SessionStatus.COMPLETED);
        sessions.saveAndFlush(charging);
        assertThat(sessions.pendingStartedSms(now.plusSeconds(301), page)).isEmpty();
        assertThat(sessions.pendingStoppedSms(now.plusSeconds(301), page)).contains(charging.getId());
        assertThat(sessions.claimStoppedSms(charging.getId(), now.plusSeconds(301), now.plusSeconds(601))).isEqualTo(1);
        sessions.completeStoppedSms(charging.getId());
        assertThat(sessions.pendingStoppedSms(now.plusSeconds(602), page)).isEmpty();
    }

    @Test
    void databaseRejectsTwoActiveSessionsForOneConnector() {
        UUID connectorId = UUID.randomUUID();
        sessions.saveAndFlush(session(connectorId, "TX-1"));

        assertThatThrownBy(() -> sessions.saveAndFlush(session(connectorId, "TX-2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void completedSessionReleasesConnectorForAnotherSession() {
        UUID connectorId = UUID.randomUUID();
        ChargingSession completed = session(connectorId, "TX-1");
        completed.stop(BigDecimal.ZERO, SessionStatus.COMPLETED);
        sessions.saveAndFlush(completed);

        sessions.saveAndFlush(session(connectorId, "TX-2"));

        assertThat(sessions.existsByConnectorIdAndStatus(connectorId, SessionStatus.ACTIVE)).isTrue();
        assertThat(sessions.count()).isEqualTo(2);
    }

    @Test
    void interruptedSessionReleasesNextCarAndDoesNotQueueBillingUntilFinalMeter() {
        UUID connectorId=UUID.randomUUID();
        var recovered=session(connectorId,"RECOVERY-1");
        recovered.stop(BigDecimal.TEN,SessionStatus.INTERRUPTED);
        sessions.saveAndFlush(recovered);
        sessions.saveAndFlush(session(connectorId,"NEXT-CAR"));
        assertThat(sessions.pendingBilling(java.time.Instant.now().plusSeconds(1),org.springframework.data.domain.PageRequest.of(0,20))).isEmpty();
        recovered.completeRecovered(new BigDecimal("11"));
        sessions.saveAndFlush(recovered);
        assertThat(sessions.existsByConnectorIdAndStatus(connectorId,SessionStatus.ACTIVE)).isTrue();
        assertThat(sessions.count()).isEqualTo(2);
        assertThat(sessions.pendingBilling(java.time.Instant.now().plusSeconds(1),org.springframework.data.domain.PageRequest.of(0,20))).containsExactly(recovered.getId());
    }

    private ChargingSession session(UUID connectorId, String transactionId) {
        return new ChargingSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), connectorId, UUID.randomUUID(), transactionId,
                BigDecimal.ZERO, new BigDecimal("12.5000"), new BigDecimal("0.5000"),
                new BigDecimal("5.00"), new BigDecimal("18.00"), "INR");
    }
}
