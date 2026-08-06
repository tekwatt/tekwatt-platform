package com.tekwatt.analytics.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
public record DailyAnalytics(LocalDate date,long sessions,BigDecimal energyKwh,BigDecimal revenue,long averageDurationSeconds,long activeChargers){}
