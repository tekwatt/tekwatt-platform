package com.tekwatt.session.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record StartSessionRequest(@NotNull UUID tenantId, @NotNull UUID userId, @NotNull UUID chargerId, @NotNull UUID connectorId,
        @NotBlank String transactionId, @NotNull @PositiveOrZero BigDecimal meterStartWh,
        @NotNull @PositiveOrZero BigDecimal pricePerKwh, @NotBlank @Size(min = 3, max = 3) String currency) {}
