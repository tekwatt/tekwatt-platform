package com.tekwatt.charger.dto;

import com.tekwatt.charger.entity.ChargerStatus;
import com.tekwatt.charger.entity.ProtocolVersion;
import java.time.Instant;
import java.util.UUID;

public record ChargerResponse(UUID id, UUID tenantId, UUID organizationId, String stationId,
        String serialNumber, String vendor, String model, ProtocolVersion protocolVersion,
        ChargerStatus status, Instant lastHeartbeat, Instant createdAt, Instant updatedAt) {}
