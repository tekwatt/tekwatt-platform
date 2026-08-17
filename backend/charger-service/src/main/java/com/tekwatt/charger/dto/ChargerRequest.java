package com.tekwatt.charger.dto;

import com.tekwatt.charger.entity.ProtocolVersion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ChargerRequest(@NotNull UUID tenantId, UUID organizationId, @NotBlank String stationId,
        @NotBlank String serialNumber, @NotBlank String vendor, @NotBlank String model,
        @NotNull ProtocolVersion protocolVersion, String stationName, String address, String city,
        String state, String description, BigDecimal latitude, BigDecimal longitude, String stationStatus,
        String openingHours, BigDecimal powerKw, BigDecimal pricePerKwh, String contactPhone,
        String contactEmail, String firmwareVersion, String meterSerialNumber, String simNumber) {}
