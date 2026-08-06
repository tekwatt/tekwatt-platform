package com.tekwatt.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "user_profiles")
public class UserProfile {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name="auth_user_id", nullable=false, unique=true) private UUID authUserId;
    @Column(name="tenant_id", nullable=false) private UUID tenantId;
    @Column(name="first_name", nullable=false, length=100) private String firstName;
    @Column(name="last_name", nullable=false, length=100) private String lastName;
    @Column(nullable=false, unique=true, length=254) private String email;
    @Column(length=32) private String phone;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private UserStatus status = UserStatus.ACTIVE;
    @Column(name="created_at", nullable=false) private Instant createdAt = Instant.now();
    @Column(name="updated_at", nullable=false) private Instant updatedAt = Instant.now();
    protected UserProfile() { }
    public UserProfile(UUID authUserId, UUID tenantId, String firstName, String lastName, String email, String phone) { this.authUserId=authUserId; this.tenantId=tenantId; update(firstName,lastName,email,phone); }
    public void update(String firstName, String lastName, String email, String phone) { this.firstName=firstName; this.lastName=lastName; this.email=email; this.phone=phone; this.updatedAt=Instant.now(); }
    public void deactivate() { status=UserStatus.INACTIVE; updatedAt=Instant.now(); }
    public UUID getId(){return id;} public UUID getAuthUserId(){return authUserId;} public UUID getTenantId(){return tenantId;} public String getFirstName(){return firstName;} public String getLastName(){return lastName;} public String getEmail(){return email;} public String getPhone(){return phone;} public UserStatus getStatus(){return status;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
