package com.tekwatt.charger.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "chargers")
public class Charger {
    @Id @JdbcTypeCode(SqlTypes.BINARY) private UUID id;
    @Column(nullable = false) @JdbcTypeCode(SqlTypes.BINARY) private UUID tenantId;
    @JdbcTypeCode(SqlTypes.BINARY) private UUID organizationId;
    @Column(nullable = false, unique = true, length = 100) private String stationId;
    @Column(nullable = false, length = 100) private String serialNumber;
    @Column(nullable = false, length = 100) private String vendor;
    @Column(nullable = false, length = 100) private String model;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ProtocolVersion protocolVersion;
    @Column(length = 160) private String stationName;
    @Column(length = 500) private String address;
    @Column(length = 100) private String city;
    @Column(length = 100) private String state;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(precision = 10, scale = 7) private BigDecimal latitude;
    @Column(precision = 10, scale = 7) private BigDecimal longitude;
    @Column(length = 20) private String stationStatus;
    @Column(length = 100) private String openingHours;
    @Column(precision = 10, scale = 2) private BigDecimal powerKw;
    @Column(precision = 10, scale = 2) private BigDecimal pricePerKwh;
    @Column(length = 80) private String contactPhone;
    @Column(length = 190) private String contactEmail;
    @Column(length = 80) private String firmwareVersion;
    @Column(length = 100) private String meterSerialNumber;
    @Column(length = 80) private String simNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ChargerStatus status;
    private Instant lastHeartbeat;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected Charger() {}

    public Charger(UUID tenantId, UUID organizationId, String stationId, String serialNumber, String vendor, String model,
            ProtocolVersion protocolVersion, String stationName, String address, String city, String state,
            String description, BigDecimal latitude, BigDecimal longitude, String stationStatus,
            String openingHours, BigDecimal powerKw, BigDecimal pricePerKwh, String contactPhone,
            String contactEmail, String firmwareVersion, String meterSerialNumber, String simNumber) {
        this.id = UUID.randomUUID(); this.tenantId = tenantId; this.organizationId = organizationId;
        this.stationId = stationId; this.serialNumber = serialNumber; this.vendor = vendor; this.model = model;
        this.stationName = stationName == null || stationName.isBlank() ? stationId : stationName;
        this.address = address; this.city = city; this.state = state; this.description = description;
        this.latitude = latitude; this.longitude = longitude;
        this.stationStatus = stationStatus == null || stationStatus.isBlank() ? "ACTIVE" : stationStatus.toUpperCase();
        this.openingHours = openingHours == null || openingHours.isBlank() ? "24/7" : openingHours;
        this.powerKw = powerKw; this.pricePerKwh = pricePerKwh; this.contactPhone = contactPhone;
        this.contactEmail = contactEmail; this.firmwareVersion = firmwareVersion;
        this.meterSerialNumber = meterSerialNumber; this.simNumber = simNumber;
        this.protocolVersion = protocolVersion; this.status = ChargerStatus.REGISTERED;
        this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }

    public void update(UUID organizationId, String serialNumber, String vendor, String model, ProtocolVersion protocolVersion,
            String stationName, String address, String city, String state, String description,
            BigDecimal latitude, BigDecimal longitude, String stationStatus, String openingHours,
            BigDecimal powerKw, BigDecimal pricePerKwh, String contactPhone, String contactEmail,
            String firmwareVersion, String meterSerialNumber, String simNumber) {
        this.organizationId = organizationId; this.serialNumber = serialNumber; this.vendor = vendor;
        this.model = model; this.protocolVersion = protocolVersion; this.stationName = stationName;
        this.address = address; this.city = city; this.state = state; this.description = description;
        this.latitude = latitude; this.longitude = longitude; this.stationStatus = stationStatus;
        this.openingHours = openingHours; this.powerKw = powerKw; this.pricePerKwh = pricePerKwh;
        this.contactPhone = contactPhone; this.contactEmail = contactEmail; this.firmwareVersion = firmwareVersion;
        this.meterSerialNumber = meterSerialNumber; this.simNumber = simNumber; this.updatedAt = Instant.now();
    }
    public void changeStatus(ChargerStatus status) { this.status = status; this.updatedAt = Instant.now(); }
    public void recordHeartbeat() { this.lastHeartbeat = Instant.now(); this.updatedAt = this.lastHeartbeat; }
    public UUID getId() { return id; } public UUID getTenantId() { return tenantId; }
    public UUID getOrganizationId() { return organizationId; } public String getStationId() { return stationId; }
    public String getSerialNumber() { return serialNumber; } public String getVendor() { return vendor; }
    public String getModel() { return model; } public ProtocolVersion getProtocolVersion() { return protocolVersion; }
    public String getStationName() { return stationName; } public String getAddress() { return address; }
    public String getCity() { return city; } public String getState() { return state; }
    public String getDescription() { return description; } public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; } public String getStationStatus() { return stationStatus; }
    public String getOpeningHours() { return openingHours; } public BigDecimal getPowerKw() { return powerKw; }
    public BigDecimal getPricePerKwh() { return pricePerKwh; } public String getContactPhone() { return contactPhone; }
    public String getContactEmail() { return contactEmail; } public String getFirmwareVersion() { return firmwareVersion; }
    public String getMeterSerialNumber() { return meterSerialNumber; } public String getSimNumber() { return simNumber; }
    public ChargerStatus getStatus() { return status; } public Instant getLastHeartbeat() { return lastHeartbeat; }
    public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
