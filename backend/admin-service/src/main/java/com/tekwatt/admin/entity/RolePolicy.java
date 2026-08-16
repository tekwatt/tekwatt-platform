package com.tekwatt.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

@Entity
@Table(name = "role_policies", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "role_name"}))
public class RolePolicy {
    @Id private UUID id;
    @Column(nullable = false) private UUID tenantId;
    @Column(nullable = false, length = 60) private String roleName;
    @Column(nullable = false, columnDefinition = "TEXT") private String permissions;
    @Column(nullable = false) private boolean systemRole;
    @Column(nullable = false) private Instant updatedAt;

    protected RolePolicy() {}

    public RolePolicy(UUID tenantId, String roleName) {
        this(tenantId, roleName, false);
    }

    public RolePolicy(UUID tenantId, String roleName, boolean systemRole) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.roleName = roleName.toUpperCase();
        this.permissions = "";
        this.systemRole = systemRole;
        this.updatedAt = Instant.now();
    }

    public void update(Collection<String> permissions) {
        this.permissions = String.join(",", new TreeSet<>(permissions));
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getRoleName() { return roleName; }
    public List<String> getPermissionList() { return permissions.isBlank() ? List.of() : List.of(permissions.split(",")); }
    public boolean isSystemRole() { return systemRole; }
    public Instant getUpdatedAt() { return updatedAt; }
}
