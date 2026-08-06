package com.tekwatt.session.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meter_readings")
public class MeterReading {
    @Id private UUID id;
    @Column(nullable = false) private UUID sessionId;
    @Column(nullable = false, precision = 14, scale = 3) private BigDecimal meterWh;
    @Column(nullable = false) private Instant recordedAt;
    protected MeterReading() {}
    public MeterReading(UUID sessionId, BigDecimal meterWh, Instant recordedAt) { this.id = UUID.randomUUID(); this.sessionId = sessionId; this.meterWh = meterWh; this.recordedAt = recordedAt; }
}
