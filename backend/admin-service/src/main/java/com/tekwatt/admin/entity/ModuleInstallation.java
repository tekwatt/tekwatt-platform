package com.tekwatt.admin.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="module_installations",uniqueConstraints=@UniqueConstraint(name="uk_module_tenant_key",columnNames={"tenant_id","module_key"}))
public class ModuleInstallation {
    @Id private UUID id; @Column(nullable=false) private UUID tenantId; @Column(nullable=false,length=80) private String moduleKey;
    @Column(nullable=false) private Instant installedAt; @Column(nullable=false) private Instant updatedAt;
    protected ModuleInstallation() {}
    public ModuleInstallation(UUID tenantId,String moduleKey){id=UUID.randomUUID();this.tenantId=tenantId;this.moduleKey=moduleKey;installedAt=Instant.now();updatedAt=installedAt;}
    public UUID getId(){return id;} public UUID getTenantId(){return tenantId;} public String getModuleKey(){return moduleKey;} public Instant getInstalledAt(){return installedAt;} public Instant getUpdatedAt(){return updatedAt;}
}
