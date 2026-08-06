package com.tekwatt.connector.repository;

import com.tekwatt.connector.entity.Connector;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ConnectorRepository extends JpaRepository<Connector, UUID> {
    boolean existsByChargerIdAndConnectorNumber(UUID chargerId, Integer connectorNumber);
    List<Connector> findAllByChargerIdOrderByConnectorNumber(UUID chargerId);
}
