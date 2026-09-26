package com.tekwatt.session.service;

import com.tekwatt.session.client.SessionBillingClient;
import com.tekwatt.session.repository.ChargingSessionRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Durable retry queue stored alongside the session in the same stop transaction. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name="tekwatt.billing.enabled", havingValue="true", matchIfMissing=true)
public class SessionBillingWorker {
    private static final Logger log = LoggerFactory.getLogger(SessionBillingWorker.class);
    private final ChargingSessionRepository sessions;
    private final SessionBillingClient billing;
    public SessionBillingWorker(ChargingSessionRepository sessions, SessionBillingClient billing) {
        this.sessions = sessions; this.billing = billing;
    }
    @Scheduled(fixedDelayString="${tekwatt.billing.poll-ms:5000}")
    public void process() {
        for (var id : sessions.pendingBilling(Instant.now(), PageRequest.of(0, 20))) {
            var now = Instant.now();
            if (sessions.claimBilling(id, now, now.plusSeconds(120)) != 1) continue;
            try {
                billing.issue(sessions.findById(id).orElseThrow());
                sessions.completeBilling(id);
            } catch (RuntimeException failure) {
                // No payloads/customer details in logs. Lease expiry retries after failure or restart.
                log.warn("Automatic billing pending for session {} ({})", id, failure.getClass().getSimpleName());
            }
        }
    }
}
