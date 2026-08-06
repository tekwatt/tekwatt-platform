package com.tekwatt.session.dto;

import com.tekwatt.session.entity.SessionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SessionResponse(UUID id, UUID tenantId, UUID userId, UUID chargerId, UUID connectorId, String transactionId,
        SessionStatus status, BigDecimal meterStartWh, BigDecimal meterStopWh, BigDecimal energyKwh, BigDecimal pricePerKwh,
        BigDecimal totalCost, String currency, Instant startedAt, Instant stoppedAt, Instant updatedAt) {}
