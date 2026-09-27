package com.tekwatt.session.service;

import com.tekwatt.session.client.ChargingSmsClient;
import com.tekwatt.session.repository.ChargingSessionRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** The SMS queue is durable and independent of charging and billing transactions. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name="tekwatt.charging-sms.enabled", havingValue="true", matchIfMissing=true)
public class ChargingSmsWorker {
    private static final Logger log = LoggerFactory.getLogger(ChargingSmsWorker.class);
    private final ChargingSessionRepository sessions;
    private final ChargingSmsClient sms;

    public ChargingSmsWorker(ChargingSessionRepository sessions, ChargingSmsClient sms) {
        this.sessions = sessions; this.sms = sms;
    }

    @Scheduled(fixedDelayString="${tekwatt.charging-sms.poll-ms:5000}")
    public void process() {
        for (UUID id : sessions.pendingStartedSms(Instant.now(), PageRequest.of(0, 20))) {
            Instant now = Instant.now();
            if (sessions.claimStartedSms(id, now, now.plusSeconds(300)) != 1) continue;
            try {
                sms.sendStarted(sessions.findById(id).orElseThrow());
                sessions.completeStartedSms(id);
            } catch (RuntimeException failure) {
                log.warn("Charging-start SMS pending for session {} ({})", id, failure.getClass().getSimpleName());
            }
        }
        for (UUID id : sessions.pendingStoppedSms(Instant.now(), PageRequest.of(0, 20))) {
            Instant now = Instant.now();
            if (sessions.claimStoppedSms(id, now, now.plusSeconds(300)) != 1) continue;
            try {
                sms.sendStopped(sessions.findById(id).orElseThrow());
                sessions.completeStoppedSms(id);
            } catch (RuntimeException failure) {
                log.warn("Charging-stop SMS pending for session {} ({})", id, failure.getClass().getSimpleName());
            }
        }
    }
}
