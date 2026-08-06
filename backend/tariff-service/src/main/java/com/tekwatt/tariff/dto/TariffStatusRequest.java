package com.tekwatt.tariff.dto;import com.tekwatt.tariff.entity.TariffStatus;import jakarta.validation.constraints.NotNull;public record TariffStatusRequest(@NotNull TariffStatus status){}
