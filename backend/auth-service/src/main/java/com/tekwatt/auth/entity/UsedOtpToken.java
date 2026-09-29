package com.tekwatt.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity @Table(name = "used_otp_tokens")
public class UsedOtpToken {
    @Id @Column(name = "token_hash", length = 64) private String tokenHash;
    @Column(name = "used_at", nullable = false) private Instant usedAt;
    protected UsedOtpToken() { }
    public UsedOtpToken(String tokenHash) { this.tokenHash = tokenHash; this.usedAt = Instant.now(); }
}
