package com.tekwatt.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, unique = true, length = 254) private String email;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @Column(nullable = false, length = 64) private String role;
    @Column(name = "verified_phone", unique = true, length = 16) private String verifiedPhone;
    @Column(name = "phone_verified_at") private Instant phoneVerifiedAt;
    @Column(nullable = false) private boolean enabled = true;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    protected AppUser() { }
    public AppUser(String email, String passwordHash, String role) { this.email = email; this.passwordHash = passwordHash; this.role = role; }
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public String getVerifiedPhone() { return verifiedPhone; }
    public Instant getPhoneVerifiedAt() { return phoneVerifiedAt; }
    public boolean isEnabled() { return enabled; }
    public void changePassword(String passwordHash) { this.passwordHash = passwordHash; }
    public void linkVerifiedPhone(String phone) { this.verifiedPhone = phone; this.phoneVerifiedAt = Instant.now(); }
    public void changeRole(String role) { this.role = role; }
}
