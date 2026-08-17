package com.tekwatt.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="technicians")
public class Technician {
    @Id private UUID id;
    @Column(nullable=false) private UUID tenantId;
    @Column(nullable=false,length=120) private String name;
    @Column(nullable=false,length=190) private String email;
    @Column(length=32) private String phone;
    @Column(length=500) private String skills;
    @Column(nullable=false,length=20) private String status;
    private UUID authUserId;
    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    protected Technician() {}
    public Technician(UUID tenantId,String name,String email,String phone,String skills,String status,UUID authUserId){id=UUID.randomUUID();this.tenantId=tenantId;update(name,email,phone,skills,status,authUserId);createdAt=updatedAt;}
    public void update(String name,String email,String phone,String skills,String status,UUID authUserId){this.name=name;this.email=email;this.phone=phone;this.skills=skills;this.status=status==null?"ACTIVE":status;if(authUserId!=null)this.authUserId=authUserId;updatedAt=Instant.now();}
    public UUID getId(){return id;} public UUID getTenantId(){return tenantId;} public String getName(){return name;} public String getEmail(){return email;} public String getPhone(){return phone;} public String getSkills(){return skills;} public String getStatus(){return status;} public UUID getAuthUserId(){return authUserId;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
