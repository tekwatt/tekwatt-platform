package com.tekwatt.charger.dto;

import com.tekwatt.charger.entity.ChargerStatus;
import com.tekwatt.charger.entity.ProtocolVersion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ChargerResponse(UUID id, UUID tenantId, UUID organizationId, String stationId,
        String serialNumber, String vendor, String model, ProtocolVersion protocolVersion,
        String stationName, String address, String city, String state, String description,
        BigDecimal latitude, BigDecimal longitude, String stationStatus, String openingHours,
        BigDecimal powerKw, BigDecimal pricePerKwh, String contactPhone, String contactEmail,
        String firmwareVersion, String meterSerialNumber, String simNumber,
        ChargerStatus status, Instant lastHeartbeat, Instant createdAt, Instant updatedAt) {}
