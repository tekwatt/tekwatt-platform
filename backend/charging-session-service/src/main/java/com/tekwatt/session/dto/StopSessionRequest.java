package com.tekwatt.session.dto;

import com.tekwatt.session.entity.SessionStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record StopSessionRequest(@NotNull @PositiveOrZero BigDecimal meterStopWh, SessionStatus status) {}
