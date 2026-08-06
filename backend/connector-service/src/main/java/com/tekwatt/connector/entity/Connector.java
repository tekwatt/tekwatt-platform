package com.tekwatt.connector.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "connectors", uniqueConstraints = @UniqueConstraint(name = "uk_connector_charger_number", columnNames = {"charger_id", "connector_number"}))
public class Connector {
    @Id private UUID id;
    @Column(nullable = false) private UUID tenantId;
    @Column(nullable = false) private UUID chargerId;
    @Column(nullable = false) private Integer connectorNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ConnectorType type;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal maxPowerKw;
    @Column(nullable = false) private Integer maxVoltage;
    @Column(nullable = false) private Integer maxCurrent;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ConnectorStatus status;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected Connector() {}
    public Connector(UUID tenantId, UUID chargerId, Integer connectorNumber, ConnectorType type, BigDecimal maxPowerKw, Integer maxVoltage, Integer maxCurrent) {
        this.id = UUID.randomUUID(); this.tenantId = tenantId; this.chargerId = chargerId; this.connectorNumber = connectorNumber;
        this.type = type; this.maxPowerKw = maxPowerKw; this.maxVoltage = maxVoltage; this.maxCurrent = maxCurrent;
        this.status = ConnectorStatus.UNAVAILABLE; this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }
    public void update(ConnectorType type, BigDecimal maxPowerKw, Integer maxVoltage, Integer maxCurrent) {
        this.type = type; this.maxPowerKw = maxPowerKw; this.maxVoltage = maxVoltage; this.maxCurrent = maxCurrent; this.updatedAt = Instant.now();
    }
    public void changeStatus(ConnectorStatus status) { this.status = status; this.updatedAt = Instant.now(); }
    public UUID getId() { return id; } public UUID getTenantId() { return tenantId; } public UUID getChargerId() { return chargerId; }
    public Integer getConnectorNumber() { return connectorNumber; } public ConnectorType getType() { return type; }
    public BigDecimal getMaxPowerKw() { return maxPowerKw; } public Integer getMaxVoltage() { return maxVoltage; }
    public Integer getMaxCurrent() { return maxCurrent; } public ConnectorStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
