package com.tekwatt.analytics.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="analytics_events") public class AnalyticsEvent {
 @Id private UUID id; @Column(nullable=false,unique=true,length=100) private String externalEventId; @Column(nullable=false) private UUID tenantId; @Column(nullable=false) private UUID chargerId; @Column(nullable=false) private UUID sessionId; @Column(nullable=false,precision=14,scale=4) private BigDecimal energyKwh; @Column(nullable=false,precision=14,scale=2) private BigDecimal revenue; @Column(nullable=false,length=3) private String currency; @Column(nullable=false) private long durationSeconds; @Column(nullable=false) private Instant occurredAt; @Column(nullable=false,updatable=false) private Instant recordedAt;
 protected AnalyticsEvent(){} public AnalyticsEvent(String eventId,UUID tenantId,UUID chargerId,UUID sessionId,BigDecimal energy,BigDecimal revenue,String currency,long duration,Instant occurred){id=UUID.randomUUID();externalEventId=eventId;this.tenantId=tenantId;this.chargerId=chargerId;this.sessionId=sessionId;energyKwh=energy;this.revenue=revenue;this.currency=currency.toUpperCase();durationSeconds=duration;occurredAt=occurred;recordedAt=Instant.now();} public UUID getId(){return id;}
}
