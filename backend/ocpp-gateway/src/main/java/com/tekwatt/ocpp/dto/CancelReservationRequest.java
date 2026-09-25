package com.tekwatt.ocpp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CancelReservationRequest(@NotBlank String stationId, @NotBlank String ocppVersion,
        @NotNull Integer reservationId) {}
