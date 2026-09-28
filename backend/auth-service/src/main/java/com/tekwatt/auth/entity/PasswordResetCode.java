package com.tekwatt.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "password_reset_codes")
public class PasswordResetCode {
    @Id private UUID id;
    @OneToOne(optional = false) @JoinColumn(name = "user_id", nullable = false, unique = true) private AppUser user;
    @Column(name = "code_hash", nullable = false) private String codeHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "requested_at", nullable = false) private Instant requestedAt;
    @Column(nullable = false) private int attempts;
    protected PasswordResetCode() { }
    public PasswordResetCode(AppUser user, String codeHash, Instant now) {
        this.id = UUID.randomUUID(); this.user = user; this.codeHash = codeHash;
        this.requestedAt = now; this.expiresAt = now.plusSeconds(600);
    }
    public AppUser getUser() { return user; }
    public String getCodeHash() { return codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRequestedAt() { return requestedAt; }
    public int getAttempts() { return attempts; }
    public void failedAttempt() { attempts++; }
    public void renew(String hash, Instant now) {
        codeHash = hash; requestedAt = now; expiresAt = now.plusSeconds(600); attempts = 0;
    }
}
