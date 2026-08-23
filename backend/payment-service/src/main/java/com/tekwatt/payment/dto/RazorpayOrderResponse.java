package com.tekwatt.payment.dto;

import java.util.UUID;

public record RazorpayOrderResponse(
        UUID paymentId,
        String orderId,
        String keyId,
        long amount,
        String currency,
        String description) {
}
