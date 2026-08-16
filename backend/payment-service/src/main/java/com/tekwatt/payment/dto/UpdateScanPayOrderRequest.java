package com.tekwatt.payment.dto;import jakarta.validation.constraints.NotBlank;import java.util.UUID;public record UpdateScanPayOrderRequest(UUID paymentId,UUID sessionId,@NotBlank String status){}
