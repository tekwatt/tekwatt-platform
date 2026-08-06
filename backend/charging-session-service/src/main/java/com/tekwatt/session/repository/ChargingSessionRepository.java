package com.tekwatt.session.repository;

import com.tekwatt.session.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ChargingSessionRepository extends JpaRepository<ChargingSession, UUID> {
    boolean existsByTransactionId(String transactionId);
    List<ChargingSession> findAllByTenantIdOrderByStartedAtDesc(UUID tenantId);
}
