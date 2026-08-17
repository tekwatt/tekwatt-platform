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
    @Column(name="full_name", length=200) private String fullName;
    @Column(nullable=false, unique=true, length=254) private String email;
    @Column(length=32) private String phone;
    @Column(length=100) private String city;
    @Column(length=20) private String zipcode;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private UserStatus status = UserStatus.ACTIVE;
    @Column(name="created_at", nullable=false) private Instant createdAt = Instant.now();
    @Column(name="updated_at", nullable=false) private Instant updatedAt = Instant.now();
    protected UserProfile() { }
    public UserProfile(UUID authUserId, UUID tenantId, String firstName, String lastName, String fullName, String email, String phone, String city, String zipcode, String status) { this.authUserId=authUserId; this.tenantId=tenantId; update(firstName,lastName,fullName,email,phone,city,zipcode,status); }
    public void update(String firstName, String lastName, String fullName, String email, String phone, String city, String zipcode, String status) { this.firstName=firstName; this.lastName=lastName; this.fullName=fullName==null||fullName.isBlank()?(firstName+" "+lastName).trim():fullName.trim(); this.email=email; this.phone=phone; this.city=city; this.zipcode=zipcode; if(status!=null&&!status.isBlank())this.status=UserStatus.valueOf(status.toUpperCase()); this.updatedAt=Instant.now(); }
    public void deactivate() { status=UserStatus.INACTIVE; updatedAt=Instant.now(); }
    public UUID getId(){return id;} public UUID getAuthUserId(){return authUserId;} public UUID getTenantId(){return tenantId;} public String getFirstName(){return firstName;} public String getLastName(){return lastName;} public String getFullName(){return fullName;} public String getEmail(){return email;} public String getPhone(){return phone;} public String getCity(){return city;} public String getZipcode(){return zipcode;} public UserStatus getStatus(){return status;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
