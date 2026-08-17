package com.tekwatt.user.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="partners")
public class Partner {
    @Id private UUID id;
    @Column(nullable=false) private UUID tenantId;
    @Column(nullable=false,length=160) private String companyName;
    @Column(nullable=false,length=80) private String partnerUniqueId;
    @Column(nullable=false,length=120) private String contactName;
    @Column(length=190) private String email;
    @Column(length=32) private String phone;
    @Column(nullable=false,precision=5,scale=2) private BigDecimal commissionPercent;
    @Column(nullable=false,length=20) private String status;
    @Column(length=500) private String address;
    @Column(length=190) private String appLoginEmail;
    private UUID authUserId;
    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    protected Partner() {}
    public Partner(UUID tenantId,String companyName,String partnerUniqueId,String contactName,String email,String phone,BigDecimal commission,String status,String address,String appLoginEmail,UUID authUserId){id=UUID.randomUUID();this.tenantId=tenantId;update(companyName,partnerUniqueId,contactName,email,phone,commission,status,address,appLoginEmail,authUserId);createdAt=updatedAt;}
    public void update(String companyName,String partnerUniqueId,String contactName,String email,String phone,BigDecimal commission,String status,String address,String appLoginEmail,UUID authUserId){this.companyName=companyName;this.partnerUniqueId=partnerUniqueId;this.contactName=contactName;this.email=email;this.phone=phone;this.commissionPercent=commission;this.status=status==null?"ACTIVE":status;this.address=address;this.appLoginEmail=appLoginEmail;if(authUserId!=null)this.authUserId=authUserId;updatedAt=Instant.now();}
    public UUID getId(){return id;} public UUID getTenantId(){return tenantId;} public String getCompanyName(){return companyName;} public String getPartnerUniqueId(){return partnerUniqueId;} public String getContactName(){return contactName;} public String getEmail(){return email;} public String getPhone(){return phone;} public BigDecimal getCommissionPercent(){return commissionPercent;} public String getStatus(){return status;} public String getAddress(){return address;} public String getAppLoginEmail(){return appLoginEmail;} public UUID getAuthUserId(){return authUserId;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
