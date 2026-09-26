package com.tekwatt.session.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tekwatt.session.entity.ChargingSession;
import com.tekwatt.session.entity.SessionStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class ChargingSessionRepositoryTest {
    @Autowired private ChargingSessionRepository sessions;

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

    private ChargingSession session(UUID connectorId, String transactionId) {
        return new ChargingSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), connectorId, UUID.randomUUID(), transactionId,
                BigDecimal.ZERO, new BigDecimal("12.5000"), new BigDecimal("0.5000"),
                new BigDecimal("5.00"), new BigDecimal("18.00"), "INR");
    }
}
