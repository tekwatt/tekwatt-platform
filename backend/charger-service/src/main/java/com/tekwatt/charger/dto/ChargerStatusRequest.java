package com.tekwatt.charger.dto;

import com.tekwatt.charger.entity.ChargerStatus;
import jakarta.validation.constraints.NotNull;

public record ChargerStatusRequest(@NotNull ChargerStatus status) {}
