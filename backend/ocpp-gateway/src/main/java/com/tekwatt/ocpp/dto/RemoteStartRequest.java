package com.tekwatt.ocpp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RemoteStartRequest(
        @NotBlank String stationId,
        @NotBlank String ocppVersion,
        @NotNull Integer connectorId,
        @NotBlank String idToken) {}
