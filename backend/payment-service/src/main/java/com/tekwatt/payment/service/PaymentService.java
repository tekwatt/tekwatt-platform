package com.tekwatt.payment.service;

import com.tekwatt.payment.dto.*;
import com.tekwatt.payment.entity.*;
import com.tekwatt.payment.repository.PaymentGatewayConfigRepository;
import com.tekwatt.payment.repository.PaymentRepository;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class PaymentService {
    private final PaymentRepository payments;
    private final PaymentGatewayConfigRepository gateways;
    private final RazorpayClient razorpay;

    public PaymentService(PaymentRepository payments, PaymentGatewayConfigRepository gateways, RazorpayClient razorpay) {
        this.payments = payments;
        this.gateways = gateways;
        this.razorpay = razorpay;
    }

    public PaymentResponse create(CreatePaymentRequest request) {
        Optional<Payment> existing = payments.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) return PaymentResponse.from(existing.get());
        Payment payment = new Payment(request.tenantId(), request.userId(), request.billId(), request.invoiceId(),
                request.idempotencyKey(), request.provider(), request.paymentMethod(), request.amount(), request.currency());
        payment.process();
        return PaymentResponse.from(payments.save(payment));
    }

    public RazorpayOrderResponse createRazorpayOrder(RazorpayOrderRequest request) {
        PaymentGatewayConfig gateway = razorpayGateway(request.tenantId());
        Optional<Payment> existing = payments.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent() && existing.get().getProviderOrderId() != null) {
            Payment payment = existing.get();
            return orderResponse(payment, gateway, request.description());
        }
        if (existing.isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "A payment already exists for this invoice.");

        Payment payment = new Payment(request.tenantId(), request.userId(), request.billId(), request.invoiceId(),
                request.idempotencyKey(), "RAZORPAY", "ONLINE", request.amount(), request.currency());
        payment.process();
        payments.save(payment);
        long subunits;
        try {
            subunits = request.amount().movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must have no more than two decimal places.");
        }
        String orderId = razorpay.createOrder(gateway.getPublicKey(), gateway.getSecretKey(), subunits,
                request.currency().toUpperCase(), payment.getId().toString(), request.description());
        payment.assignProviderOrder(orderId);
        return new RazorpayOrderResponse(payment.getId(), orderId, gateway.getPublicKey(), subunits,
                payment.getCurrency(), request.description());
    }

    public PaymentResponse verifyRazorpay(RazorpayVerificationRequest request) {
        Payment payment = find(request.paymentId());
        if (!"RAZORPAY".equalsIgnoreCase(payment.getProvider())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This payment was not created through Razorpay.");
        }
        if (!request.razorpayOrderId().equals(payment.getProviderOrderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay order does not match this payment.");
        }
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) return PaymentResponse.from(payment);
        if (payment.getStatus() != PaymentStatus.PROCESSING && payment.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment is already finalized.");
        }
        PaymentGatewayConfig gateway = razorpayGateway(payment.getTenantId());
        String payload = request.razorpayOrderId() + "|" + request.razorpayPaymentId();
        if (!validSignature(payload, request.razorpaySignature(), gateway.getSecretKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment verification failed. No charge was recorded.");
        }
        payment.succeed(request.razorpayPaymentId());
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(UUID id) { return PaymentResponse.from(find(id)); }

    @Transactional(readOnly = true)
    public List<PaymentResponse> list(UUID tenantId) {
        return payments.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(PaymentResponse::from).toList();
    }

    public PaymentResponse result(UUID id, PaymentResultRequest request) {
        Payment payment = find(id);
        if (payment.getStatus() != PaymentStatus.PROCESSING && payment.getStatus() != PaymentStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment is already finalized");
        if (request.successful()) {
            if (request.providerReference() == null || request.providerReference().isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provider reference is required for success");
            payment.succeed(request.providerReference());
        } else payment.fail(request.providerReference(), request.failureReason());
        return PaymentResponse.from(payment);
    }

    public PaymentResponse refund(UUID id) {
        Payment payment = find(id);
        if (payment.getStatus() != PaymentStatus.SUCCEEDED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only successful payments can be refunded");
        if ("RAZORPAY".equalsIgnoreCase(payment.getProvider())) {
            PaymentGatewayConfig gateway = razorpayGateway(payment.getTenantId());
            razorpay.refund(gateway.getPublicKey(), gateway.getSecretKey(), payment.getProviderReference());
        }
        payment.refund();
        return PaymentResponse.from(payment);
    }

    private RazorpayOrderResponse orderResponse(Payment payment, PaymentGatewayConfig gateway, String description) {
        long amount = payment.getAmount().movePointRight(2).longValueExact();
        return new RazorpayOrderResponse(payment.getId(), payment.getProviderOrderId(), gateway.getPublicKey(),
                amount, payment.getCurrency(), description);
    }

    private PaymentGatewayConfig razorpayGateway(UUID tenantId) {
        PaymentGatewayConfig gateway = gateways.findByTenantIdAndProviderIgnoreCase(tenantId, "RAZORPAY")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Configure Razorpay in Payment Gateways before accepting online payments."));
        if (!gateway.isEnabled() || gateway.getPublicKey() == null || gateway.getPublicKey().isBlank() || !gateway.hasSecret())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Razorpay is disabled or its Key ID and Key Secret are incomplete.");
        return gateway;
    }

    private boolean validSignature(String payload, String suppliedSignature, String secret) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] supplied = HexFormat.of().parseHex(suppliedSignature);
            return MessageDigest.isEqual(expected, supplied);
        } catch (Exception exception) {
            return false;
        }
    }

    private Payment find(UUID id) {
        return payments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }
}
