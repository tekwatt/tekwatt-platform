package com.tekwatt.analytics.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record AnalyticsOverview(UUID tenantId,Instant from,Instant to,long sessions,BigDecimal energyKwh,BigDecimal revenue,long averageDurationSeconds,long activeChargers){}
