package com.tekwatt.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "refresh_tokens")
public class RefreshToken {
    @Id private UUID id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id", nullable = false) private AppUser user;
    @Column(nullable = false, unique = true, length = 64) private String token;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(nullable = false) private boolean revoked;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "last_used_at", nullable = false) private Instant lastUsedAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "ip_address", length = 64) private String ipAddress;
    @Column(name = "user_agent", length = 500) private String userAgent;
    protected RefreshToken() { }
    public RefreshToken(UUID id, AppUser user, String token, Instant expiresAt, String ipAddress, String userAgent) { this.id = id; this.user = user; this.token = token; this.expiresAt = expiresAt;this.createdAt=Instant.now();this.lastUsedAt=createdAt;this.ipAddress=ipAddress;this.userAgent=userAgent; }
    public UUID getId(){return id;}
    public AppUser getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt(){return createdAt;} public Instant getLastUsedAt(){return lastUsedAt;} public Instant getRevokedAt(){return revokedAt;} public String getIpAddress(){return ipAddress;} public String getUserAgent(){return userAgent;}
    public boolean isRevoked() { return revoked; }
    public void revoke() { revoked = true;revokedAt=Instant.now();lastUsedAt=revokedAt; }
}
