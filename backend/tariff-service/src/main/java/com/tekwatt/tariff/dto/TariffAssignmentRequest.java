package com.tekwatt.tariff.dto;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record TariffAssignmentRequest(@NotNull UUID tenantId, @NotNull UUID chargerId) {}
