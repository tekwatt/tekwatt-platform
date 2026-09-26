package com.tekwatt.session.repository;

import com.tekwatt.session.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.Instant;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface ChargingSessionRepository extends JpaRepository<ChargingSession, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ChargingSession s where s.id = :id")
    Optional<ChargingSession> findForUpdate(UUID id);
    @Query("select s.id from ChargingSession s where s.billingPending = true and s.billingRetryAt <= :now order by s.billingRetryAt")
    List<UUID> pendingBilling(Instant now, Pageable page);
    @Transactional @Modifying
    @Query("update ChargingSession s set s.billingRetryAt = :leaseUntil where s.id = :id and s.billingPending = true and s.billingRetryAt <= :now")
    int claimBilling(UUID id, Instant now, Instant leaseUntil);
    @Transactional @Modifying
    @Query("update ChargingSession s set s.billingPending = false where s.id = :id")
    void completeBilling(UUID id);
    boolean existsByTransactionId(String transactionId);
    boolean existsByConnectorIdAndStatus(UUID connectorId, SessionStatus status);
    Optional<ChargingSession> findByTransactionId(String transactionId);
    List<ChargingSession> findAllByTenantIdOrderByStartedAtDesc(UUID tenantId);
}
