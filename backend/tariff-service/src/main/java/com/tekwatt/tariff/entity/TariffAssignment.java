package com.tekwatt.tariff.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tariff_assignments", uniqueConstraints = @UniqueConstraint(name = "uk_tariff_assignment_charger", columnNames = {"tenant_id", "charger_id"}))
public class TariffAssignment {
    @Id private UUID id;
    @Column(nullable = false) private UUID tenantId;
    @Column(nullable = false) private UUID tariffId;
    @Column(nullable = false) private UUID chargerId;
    @Column(nullable = false) private Instant assignedAt;

    protected TariffAssignment() {}
    public TariffAssignment(UUID tenantId, UUID tariffId, UUID chargerId) {
        id = UUID.randomUUID(); this.tenantId = tenantId; this.tariffId = tariffId; this.chargerId = chargerId; assignedAt = Instant.now();
    }
    public void assign(UUID tariffId) { this.tariffId = tariffId; assignedAt = Instant.now(); }
    public UUID getId() { return id; } public UUID getTenantId() { return tenantId; }
    public UUID getTariffId() { return tariffId; } public UUID getChargerId() { return chargerId; }
    public Instant getAssignedAt() { return assignedAt; }
}
