package com.tekwatt.ocpp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record ReserveNowRequest(@NotBlank String stationId, @NotBlank String ocppVersion,
        @NotNull @Positive Integer connectorId, @NotNull Integer reservationId,
        @NotBlank String idToken, @NotNull Instant expiryDate) {}
