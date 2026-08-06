package com.tekwatt.connector.dto;

import com.tekwatt.connector.entity.ConnectorType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record ConnectorRequest(@NotNull UUID tenantId, @NotNull UUID chargerId, @NotNull @Positive Integer connectorNumber,
        @NotNull ConnectorType type, @NotNull @DecimalMin("0.01") BigDecimal maxPowerKw,
        @NotNull @Positive Integer maxVoltage, @NotNull @Positive Integer maxCurrent) {}
