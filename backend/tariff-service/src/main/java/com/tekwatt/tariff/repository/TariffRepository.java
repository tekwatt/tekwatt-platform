package com.tekwatt.tariff.repository;import com.tekwatt.tariff.entity.Tariff;import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface TariffRepository extends JpaRepository<Tariff,UUID>{boolean existsByTenantIdAndCodeIgnoreCase(UUID tenantId,String code);List<Tariff> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);}
