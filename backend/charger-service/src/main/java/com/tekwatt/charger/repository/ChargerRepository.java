package com.tekwatt.charger.repository;

import com.tekwatt.charger.entity.Charger;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChargerRepository extends JpaRepository<Charger, UUID> {
    boolean existsByStationId(String stationId);
    Optional<Charger> findByStationId(String stationId);
    List<Charger> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
