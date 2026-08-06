package com.tekwatt.session.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public record MeterValueRequest(@NotNull @PositiveOrZero BigDecimal meterWh, Instant recordedAt) {}
