package com.tekwatt.tariff.dto;
import com.tekwatt.tariff.entity.TariffAssignment;
import java.time.Instant;
import java.util.UUID;
public record TariffAssignmentResponse(UUID id, UUID tenantId, UUID tariffId, UUID chargerId, Instant assignedAt) {
    public static TariffAssignmentResponse from(TariffAssignment a) { return new TariffAssignmentResponse(a.getId(), a.getTenantId(), a.getTariffId(), a.getChargerId(), a.getAssignedAt()); }
}
