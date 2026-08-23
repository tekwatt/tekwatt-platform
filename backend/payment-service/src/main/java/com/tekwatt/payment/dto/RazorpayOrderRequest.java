package com.tekwatt.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record RazorpayOrderRequest(
        @NotNull UUID tenantId,
        @NotNull UUID userId,
        @NotNull UUID billId,
        UUID invoiceId,
        @NotBlank @Size(max = 64) String idempotencyKey,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @Size(max = 200) String description) {
}
