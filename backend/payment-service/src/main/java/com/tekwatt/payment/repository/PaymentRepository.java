package com.tekwatt.payment.repository;

import com.tekwatt.payment.entity.Payment;
import com.tekwatt.payment.entity.PaymentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdempotencyKey(String key);
    List<Payment> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<Payment> findAllByTenantIdAndUserIdOrderByCreatedAtDesc(UUID tenantId, UUID userId);
    List<Payment> findAllByTenantIdAndUserIdAndInvoiceIdAndStatus(UUID tenantId, UUID userId, UUID invoiceId, PaymentStatus status);
}
