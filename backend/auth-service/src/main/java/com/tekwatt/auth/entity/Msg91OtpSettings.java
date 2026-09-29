package com.tekwatt.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity @Table(name = "msg91_otp_settings")
public class Msg91OtpSettings {
    @Id private Integer id = 1;
    @Column(name = "widget_id", nullable = false, length = 128) private String widgetId;
    @Column(name = "encrypted_widget_token", nullable = false, length = 8192) private String encryptedWidgetToken;
    @Column(name = "encrypted_server_auth_key", nullable = false, length = 4096) private String encryptedServerAuthKey;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Msg91OtpSettings() { }
    public Msg91OtpSettings(String widgetId, String encryptedWidgetToken, String encryptedServerAuthKey) {
        update(widgetId, encryptedWidgetToken, encryptedServerAuthKey);
    }
    public void update(String widgetId, String encryptedWidgetToken, String encryptedServerAuthKey) {
        this.widgetId = widgetId;
        this.encryptedWidgetToken = encryptedWidgetToken;
        this.encryptedServerAuthKey = encryptedServerAuthKey;
        this.updatedAt = Instant.now();
    }
    public String getWidgetId() { return widgetId; }
    public String getEncryptedWidgetToken() { return encryptedWidgetToken; }
    public String getEncryptedServerAuthKey() { return encryptedServerAuthKey; }
    public Instant getUpdatedAt() { return updatedAt; }
}
