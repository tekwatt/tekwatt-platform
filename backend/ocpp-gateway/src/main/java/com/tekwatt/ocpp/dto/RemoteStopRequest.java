package com.tekwatt.ocpp.dto;

import jakarta.validation.constraints.NotBlank;

public record RemoteStopRequest(
        @NotBlank String stationId,
        @NotBlank String ocppVersion,
        @NotBlank String transactionId) {}
