package com.tekwatt.tariff.repository;

import com.tekwatt.tariff.entity.TariffAssignment;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TariffAssignmentRepository extends JpaRepository<TariffAssignment, UUID> {
    Optional<TariffAssignment> findByTenantIdAndChargerId(UUID tenantId, UUID chargerId);
    List<TariffAssignment> findAllByTenantId(UUID tenantId);
    long countByTariffId(UUID tariffId);
}
