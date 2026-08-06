package com.tekwatt.organization.entity;
import jakarta.persistence.*;import java.time.Instant;import java.util.UUID;
@Entity @Table(name="organizations") public class Organization{
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(name="tenant_id",nullable=false) private UUID tenantId;
 @Column(name="parent_id") private UUID parentId;
 @Column(nullable=false,length=150) private String name;
 @Column(nullable=false,length=60) private String code;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private OrganizationStatus status=OrganizationStatus.ACTIVE;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();@Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
 protected Organization(){} public Organization(UUID tenantId,UUID parentId,String name,String code){this.tenantId=tenantId;update(parentId,name,code);}
 public void update(UUID parentId,String name,String code){this.parentId=parentId;this.name=name;this.code=code;updatedAt=Instant.now();} public void deactivate(){status=OrganizationStatus.INACTIVE;updatedAt=Instant.now();}
 public UUID getId(){return id;}public UUID getTenantId(){return tenantId;}public UUID getParentId(){return parentId;}public String getName(){return name;}public String getCode(){return code;}public OrganizationStatus getStatus(){return status;}public Instant getCreatedAt(){return createdAt;}public Instant getUpdatedAt(){return updatedAt;}
}
