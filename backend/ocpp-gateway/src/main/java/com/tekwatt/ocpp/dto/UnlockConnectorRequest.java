package com.tekwatt.ocpp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UnlockConnectorRequest(@NotBlank String stationId, @NotBlank String ocppVersion,
        @NotNull @Positive Integer connectorId) {}
