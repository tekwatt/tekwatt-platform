package com.tekwatt.charger.dto;

import com.tekwatt.charger.entity.ProtocolVersion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChargerRequest(@NotNull UUID tenantId, UUID organizationId, @NotBlank String stationId,
        @NotBlank String serialNumber, @NotBlank String vendor, @NotBlank String model,
        @NotNull ProtocolVersion protocolVersion) {}
