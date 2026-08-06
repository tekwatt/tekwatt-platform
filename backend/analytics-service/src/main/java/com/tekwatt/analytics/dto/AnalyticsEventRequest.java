package com.tekwatt.analytics.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record AnalyticsEventRequest(@NotBlank @Size(max=100) String externalEventId,@NotNull UUID tenantId,@NotNull UUID chargerId,@NotNull UUID sessionId,@NotNull @DecimalMin("0.0") BigDecimal energyKwh,@NotNull @DecimalMin("0.0") BigDecimal revenue,@NotBlank @Pattern(regexp="[A-Za-z]{3}") String currency,@PositiveOrZero long durationSeconds,@NotNull Instant occurredAt){}
