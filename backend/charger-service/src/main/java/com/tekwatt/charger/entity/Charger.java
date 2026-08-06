package com.tekwatt.charger.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chargers")
public class Charger {
    @Id private UUID id;
    @Column(nullable = false) private UUID tenantId;
    private UUID organizationId;
    @Column(nullable = false, unique = true, length = 100) private String stationId;
    @Column(nullable = false, length = 100) private String serialNumber;
    @Column(nullable = false, length = 100) private String vendor;
    @Column(nullable = false, length = 100) private String model;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ProtocolVersion protocolVersion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ChargerStatus status;
    private Instant lastHeartbeat;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected Charger() {}

    public Charger(UUID tenantId, UUID organizationId, String stationId, String serialNumber, String vendor, String model, ProtocolVersion protocolVersion) {
        this.id = UUID.randomUUID(); this.tenantId = tenantId; this.organizationId = organizationId;
        this.stationId = stationId; this.serialNumber = serialNumber; this.vendor = vendor; this.model = model;
        this.protocolVersion = protocolVersion; this.status = ChargerStatus.REGISTERED;
        this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }

    public void update(UUID organizationId, String serialNumber, String vendor, String model, ProtocolVersion protocolVersion) {
        this.organizationId = organizationId; this.serialNumber = serialNumber; this.vendor = vendor;
        this.model = model; this.protocolVersion = protocolVersion; this.updatedAt = Instant.now();
    }
    public void changeStatus(ChargerStatus status) { this.status = status; this.updatedAt = Instant.now(); }
    public void recordHeartbeat() { this.lastHeartbeat = Instant.now(); this.updatedAt = this.lastHeartbeat; }
    public UUID getId() { return id; } public UUID getTenantId() { return tenantId; }
    public UUID getOrganizationId() { return organizationId; } public String getStationId() { return stationId; }
    public String getSerialNumber() { return serialNumber; } public String getVendor() { return vendor; }
    public String getModel() { return model; } public ProtocolVersion getProtocolVersion() { return protocolVersion; }
    public ChargerStatus getStatus() { return status; } public Instant getLastHeartbeat() { return lastHeartbeat; }
    public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
