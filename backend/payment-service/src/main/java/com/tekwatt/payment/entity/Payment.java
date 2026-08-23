package com.tekwatt.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {
    @Id private UUID id;
    @Column(nullable = false) private UUID tenantId;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false) private UUID billId;
    private UUID invoiceId;
    @Column(nullable = false, unique = true, length = 64) private String idempotencyKey;
    @Column(nullable = false, length = 50) private String provider;
    @Column(nullable = false, length = 30) private String paymentMethod;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentStatus status;
    @Column(length = 150) private String providerOrderId;
    @Column(length = 150) private String providerReference;
    @Column(length = 500) private String failureReason;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    private Instant completedAt;
    private Instant refundedAt;
    @Column(nullable = false) private Instant updatedAt;

    protected Payment() {}

    public Payment(UUID tenantId, UUID userId, UUID billId, UUID invoiceId, String idempotencyKey,
                   String provider, String paymentMethod, BigDecimal amount, String currency) {
        id = UUID.randomUUID(); this.tenantId = tenantId; this.userId = userId; this.billId = billId;
        this.invoiceId = invoiceId; this.idempotencyKey = idempotencyKey; this.provider = provider;
        this.paymentMethod = paymentMethod; this.amount = amount; this.currency = currency.toUpperCase();
        status = PaymentStatus.PENDING; createdAt = Instant.now(); updatedAt = createdAt;
    }

    public void process() { status = PaymentStatus.PROCESSING; updatedAt = Instant.now(); }
    public void assignProviderOrder(String orderId) { providerOrderId = orderId; updatedAt = Instant.now(); }
    public void succeed(String reference) { status = PaymentStatus.SUCCEEDED; providerReference = reference; failureReason = null; completedAt = Instant.now(); updatedAt = completedAt; }
    public void fail(String reference, String reason) { status = PaymentStatus.FAILED; providerReference = reference; failureReason = reason; completedAt = Instant.now(); updatedAt = completedAt; }
    public void refund() { status = PaymentStatus.REFUNDED; refundedAt = Instant.now(); updatedAt = refundedAt; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getUserId() { return userId; }
    public UUID getBillId() { return billId; }
    public UUID getInvoiceId() { return invoiceId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getProvider() { return provider; }
    public String getPaymentMethod() { return paymentMethod; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getProviderOrderId() { return providerOrderId; }
    public String getProviderReference() { return providerReference; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getRefundedAt() { return refundedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
