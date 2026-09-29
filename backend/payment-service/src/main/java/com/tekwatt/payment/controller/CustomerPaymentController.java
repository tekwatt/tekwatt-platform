package com.tekwatt.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.tekwatt.payment.dto.PaymentResponse;
import com.tekwatt.payment.dto.RazorpayOrderResponse;
import com.tekwatt.payment.dto.RazorpayVerificationRequest;
import com.tekwatt.payment.service.CustomerPaymentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments/my")
public class CustomerPaymentController {
    private final CustomerPaymentService payments;
    public CustomerPaymentController(CustomerPaymentService payments) { this.payments = payments; }

    @GetMapping
    public List<PaymentResponse> mine(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return payments.mine(authorization);
    }

    @PostMapping("/invoices/{invoiceId}/razorpay-order")
    public RazorpayOrderResponse order(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID invoiceId) {
        return payments.order(authorization, invoiceId);
    }

    @PostMapping("/razorpay-verify")
    public PaymentResponse verify(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody RazorpayVerificationRequest request) {
        return payments.verify(authorization, request);
    }

    @PostMapping("/invoices/{invoiceId}/settle")
    public JsonNode settle(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID invoiceId) {
        return payments.settle(authorization, invoiceId);
    }
}
