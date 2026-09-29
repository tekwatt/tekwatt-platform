package com.tekwatt.reservation.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CustomerReservationRequest(@NotNull UUID chargerId, @NotNull UUID connectorId,
        @NotNull Instant startsAt, @NotNull Instant expiresAt) { }
