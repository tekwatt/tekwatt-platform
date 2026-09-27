package com.tekwatt.payment.controller;

import com.tekwatt.payment.repository.PaymentGatewayConfigRepository;
import com.tekwatt.payment.service.RazorpayClient;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/operations/gateways")
public class GatewayConnectionTestController {
    private final PaymentGatewayConfigRepository gateways;
    private final RazorpayClient razorpay;

    public GatewayConnectionTestController(PaymentGatewayConfigRepository gateways, RazorpayClient razorpay) {
        this.gateways = gateways;
        this.razorpay = razorpay;
    }

    @PostMapping("/razorpay/test")
    public GatewayTestResponse testRazorpay(@RequestParam UUID tenantId) {
        var configured = gateways.findByTenantIdAndProviderIgnoreCase(tenantId, "RAZORPAY");
        if (configured.isEmpty()) return new GatewayTestResponse(false, "Save the Razorpay configuration first.");
        var gateway = configured.get();
        if (!gateway.isEnabled()) return new GatewayTestResponse(false, "Enable Razorpay before testing.");
        var check = razorpay.testConnection(gateway.getPublicKey(), gateway.getSecretKey());
        return new GatewayTestResponse(check.success(), check.message());
    }

    public record GatewayTestResponse(boolean success, String message) {}
}
