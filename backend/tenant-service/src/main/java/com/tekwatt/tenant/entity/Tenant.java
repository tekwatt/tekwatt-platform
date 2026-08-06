package com.tekwatt.tenant.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="tenants")
public class Tenant {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(nullable=false,length=150) private String name;
 @Column(nullable=false,unique=true,length=80) private String slug;
 @Column(name="contact_email",nullable=false,length=254) private String contactEmail;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private TenantStatus status=TenantStatus.ACTIVE;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
 @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
 protected Tenant(){}
 public Tenant(String name,String slug,String contactEmail){update(name,slug,contactEmail);}
 public void update(String name,String slug,String contactEmail){this.name=name;this.slug=slug;this.contactEmail=contactEmail;this.updatedAt=Instant.now();}
 public void setStatus(TenantStatus status){this.status=status;this.updatedAt=Instant.now();}
 public UUID getId(){return id;} public String getName(){return name;} public String getSlug(){return slug;} public String getContactEmail(){return contactEmail;} public TenantStatus getStatus(){return status;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
