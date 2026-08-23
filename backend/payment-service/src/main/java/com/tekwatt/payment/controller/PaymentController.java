package com.tekwatt.payment.controller;

import com.tekwatt.payment.dto.*;
import com.tekwatt.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService service;
    public PaymentController(PaymentService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) PaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) { return service.create(request); }
    @GetMapping("/{id}") PaymentResponse get(@PathVariable UUID id) { return service.get(id); }
    @GetMapping List<PaymentResponse> list(@RequestParam UUID tenantId) { return service.list(tenantId); }
    @PatchMapping("/{id}/result") PaymentResponse result(@PathVariable UUID id, @Valid @RequestBody PaymentResultRequest request) { return service.result(id, request); }
    @PostMapping("/{id}/refund") PaymentResponse refund(@PathVariable UUID id) { return service.refund(id); }
    @PostMapping("/razorpay/orders") @ResponseStatus(HttpStatus.CREATED) RazorpayOrderResponse razorpayOrder(@Valid @RequestBody RazorpayOrderRequest request) { return service.createRazorpayOrder(request); }
    @PostMapping("/razorpay/verify") PaymentResponse verifyRazorpay(@Valid @RequestBody RazorpayVerificationRequest request) { return service.verifyRazorpay(request); }
}
