package com.tekwatt.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sms_provider_credentials", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "provider"}))
public class SmsProviderCredential {
    @Id private UUID id;
    @Column(nullable = false) private UUID tenantId;
    @Column(nullable = false, length = 20) private String provider;
    @Column(nullable = false, length = 200) private String publicIdentifier;
    @Column(length = 100) private String sender;
    @Column(length = 200) private String templateId;
    @Column(length = 200) private String chargingStartedTemplateId;
    @Column(length = 200) private String chargingCompletedTemplateId;
    @Column(length = 100) private String messageVariable;
    @Column(columnDefinition = "TEXT") private String flowTemplatesJson;
    @Column(nullable = false, columnDefinition = "TEXT") private String encryptedSecret;
    @Column(nullable = false) private Instant updatedAt;

    protected SmsProviderCredential() {}
    public SmsProviderCredential(UUID tenantId, String provider) {
        this.id = UUID.randomUUID(); this.tenantId = tenantId; this.provider = provider;
    }
    public void update(String publicIdentifier, String sender, String templateId, String chargingStartedTemplateId,
            String chargingCompletedTemplateId, String messageVariable, String flowTemplatesJson, String encryptedSecret) {
        this.publicIdentifier = publicIdentifier;
        this.sender = sender;
        this.templateId = templateId;
        this.chargingStartedTemplateId = chargingStartedTemplateId;
        this.chargingCompletedTemplateId = chargingCompletedTemplateId;
        this.messageVariable = messageVariable;
        this.flowTemplatesJson = flowTemplatesJson;
        this.encryptedSecret = encryptedSecret;
        this.updatedAt = Instant.now();
    }
    public String getProvider() { return provider; }
    public String getPublicIdentifier() { return publicIdentifier; }
    public String getSender() { return sender; }
    public String getTemplateId() { return templateId; }
    public String getChargingStartedTemplateId() { return chargingStartedTemplateId; }
    public String getChargingCompletedTemplateId() { return chargingCompletedTemplateId; }
    public String getMessageVariable() { return messageVariable; }
    public String getFlowTemplatesJson() { return flowTemplatesJson; }
    public String getEncryptedSecret() { return encryptedSecret; }
    public Instant getUpdatedAt() { return updatedAt; }
}
