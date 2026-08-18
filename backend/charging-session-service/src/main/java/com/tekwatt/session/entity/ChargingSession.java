package com.tekwatt.session.entity;

import jakarta.persistence.*;
import java.math.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "charging_sessions")
public class ChargingSession {
    @Id private UUID id;
    @Column(nullable = false) private UUID tenantId;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false) private UUID chargerId;
    @Column(nullable = false) private UUID connectorId;
    private UUID tariffId;
    @Column(nullable = false, unique = true, length = 100) private String transactionId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SessionStatus status;
    @Column(nullable = false, precision = 14, scale = 3) private BigDecimal meterStartWh;
    @Column(precision = 14, scale = 3) private BigDecimal meterStopWh;
    @Column(nullable = false, precision = 12, scale = 3) private BigDecimal energyKwh;
    @Column(nullable = false, precision = 10, scale = 4) private BigDecimal pricePerKwh;
    @Column(nullable = false, precision = 10, scale = 4) private BigDecimal timePricePerMinute;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal sessionFee;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal taxPercent;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal totalCost;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false) private Instant startedAt;
    private Instant stoppedAt;
    @Column(nullable = false) private Instant updatedAt;

    protected ChargingSession() {}
    public ChargingSession(UUID tenantId, UUID userId, UUID chargerId, UUID connectorId, UUID tariffId, String transactionId, BigDecimal meterStartWh, BigDecimal pricePerKwh, BigDecimal timePricePerMinute, BigDecimal sessionFee, BigDecimal taxPercent, String currency) {
        this.id = UUID.randomUUID(); this.tenantId = tenantId; this.userId = userId; this.chargerId = chargerId; this.connectorId = connectorId;
        this.tariffId = tariffId;
        this.transactionId = transactionId; this.status = SessionStatus.ACTIVE; this.meterStartWh = meterStartWh;
        this.energyKwh = BigDecimal.ZERO.setScale(3); this.pricePerKwh = pricePerKwh; this.timePricePerMinute = timePricePerMinute; this.sessionFee = sessionFee; this.taxPercent = taxPercent; this.totalCost = BigDecimal.ZERO.setScale(2);
        this.currency = currency.toUpperCase(); this.startedAt = Instant.now(); this.updatedAt = this.startedAt;
    }
    public void applyMeterValue(BigDecimal meterWh) {
        if (meterWh.compareTo(meterStartWh) < 0) throw new IllegalArgumentException("Meter value cannot be below the start value");
        this.meterStopWh = meterWh;
        this.energyKwh = meterWh.subtract(meterStartWh).divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
        this.updatedAt = Instant.now(); calculateTotal(updatedAt);
    }
    private void calculateTotal(Instant at) {
        long seconds = Math.max(0, java.time.Duration.between(startedAt, at).getSeconds());
        long minutes = seconds == 0 ? 0 : (seconds + 59) / 60;
        BigDecimal subtotal = energyKwh.multiply(pricePerKwh).add(timePricePerMinute.multiply(BigDecimal.valueOf(minutes))).add(sessionFee);
        totalCost = subtotal.add(subtotal.multiply(taxPercent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)).setScale(2, RoundingMode.HALF_UP);
    }
    public void stop(BigDecimal meterWh, SessionStatus finalStatus) { applyMeterValue(meterWh); this.status = finalStatus; this.stoppedAt = Instant.now(); this.updatedAt = stoppedAt; calculateTotal(stoppedAt); }
    public UUID getId() { return id; } public UUID getTenantId() { return tenantId; } public UUID getUserId() { return userId; }
    public UUID getChargerId() { return chargerId; } public UUID getConnectorId() { return connectorId; } public String getTransactionId() { return transactionId; }
    public SessionStatus getStatus() { return status; } public BigDecimal getMeterStartWh() { return meterStartWh; } public BigDecimal getMeterStopWh() { return meterStopWh; }
    public UUID getTariffId() { return tariffId; } public BigDecimal getEnergyKwh() { return energyKwh; } public BigDecimal getPricePerKwh() { return pricePerKwh; } public BigDecimal getTimePricePerMinute() { return timePricePerMinute; } public BigDecimal getSessionFee() { return sessionFee; } public BigDecimal getTaxPercent() { return taxPercent; } public BigDecimal getTotalCost() { return totalCost; }
    public String getCurrency() { return currency; } public Instant getStartedAt() { return startedAt; } public Instant getStoppedAt() { return stoppedAt; } public Instant getUpdatedAt() { return updatedAt; }
}
