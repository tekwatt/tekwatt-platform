package com.tekwatt.support.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record StationReviewRequest(
        @NotNull UUID tenantId,
        @NotNull UUID stationId,
        UUID customerId,
        @NotBlank @Size(max = 120) String customerName,
        @Min(1) @Max(5) int rating,
        @NotBlank @Size(max = 4000) String comment) {}
