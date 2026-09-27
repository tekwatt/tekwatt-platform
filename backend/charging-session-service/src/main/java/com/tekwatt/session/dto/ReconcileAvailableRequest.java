package com.tekwatt.session.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/** Evidence from an authenticated charger's StatusNotification, not a timeout. */
public record ReconcileAvailableRequest(@NotNull UUID tenantId, @NotNull UUID connectorId,
        @NotNull Instant observedAt) {}
