package com.tekwatt.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "smtp_settings")
public class SmtpSettings {
    @Id private Integer id = 1;
    @Column(nullable = false) private String host;
    @Column(nullable = false) private int port;
    @Column(name = "security_mode", nullable = false) private String securityMode;
    @Column(nullable = false) private String username;
    @Column(name = "encrypted_password", nullable = false, length = 2048) private String encryptedPassword;
    @Column(name = "from_email", nullable = false) private String fromEmail;
    @Column(name = "reply_to") private String replyTo;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected SmtpSettings() { }
    public SmtpSettings(String host, int port, String securityMode, String username,
            String encryptedPassword, String fromEmail, String replyTo) {
        update(host, port, securityMode, username, encryptedPassword, fromEmail, replyTo);
    }
    public void update(String host, int port, String securityMode, String username,
            String encryptedPassword, String fromEmail, String replyTo) {
        this.host = host; this.port = port; this.securityMode = securityMode;
        this.username = username; this.encryptedPassword = encryptedPassword;
        this.fromEmail = fromEmail; this.replyTo = replyTo; this.updatedAt = Instant.now();
    }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getSecurityMode() { return securityMode; }
    public String getUsername() { return username; }
    public String getEncryptedPassword() { return encryptedPassword; }
    public String getFromEmail() { return fromEmail; }
    public String getReplyTo() { return replyTo; }
    public Instant getUpdatedAt() { return updatedAt; }
}
