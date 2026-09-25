package com.tekwatt.support.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "station_reviews")
public class StationReview {
    @Id private UUID id;
    @Column(nullable = false) private UUID tenantId;
    @Column(nullable = false) private UUID stationId;
    private UUID customerId;
    @Column(nullable = false, length = 120) private String customerName;
    @Column(nullable = false) private int rating;
    @Column(nullable = false, columnDefinition = "TEXT") private String comment;
    @Column(nullable = false, length = 20) private String status;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected StationReview() {}

    public StationReview(UUID tenantId, UUID stationId, UUID customerId, String customerName, int rating, String comment) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.stationId = stationId;
        this.customerId = customerId;
        this.customerName = customerName.trim();
        this.rating = rating;
        this.comment = comment.trim();
        this.status = "PENDING";
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void moderate(String status) { this.status = status; this.updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getStationId() { return stationId; }
    public UUID getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
