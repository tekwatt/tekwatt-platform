package com.tekwatt.connector.dto;

import com.tekwatt.connector.entity.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ConnectorResponse(UUID id, UUID tenantId, UUID chargerId, Integer connectorNumber, ConnectorType type,
        BigDecimal maxPowerKw, Integer maxVoltage, Integer maxCurrent, ConnectorStatus status, Instant createdAt, Instant updatedAt) {}
