package com.tekwatt.invoice.repository;

import com.tekwatt.invoice.entity.Invoice;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    boolean existsByBillId(UUID billId);
    List<Invoice> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<Invoice> findAllByTenantIdAndUserIdOrderByCreatedAtDesc(UUID tenantId, UUID userId);
}
