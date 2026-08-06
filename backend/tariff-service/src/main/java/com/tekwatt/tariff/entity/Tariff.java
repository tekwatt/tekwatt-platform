package com.tekwatt.tariff.entity;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.Instant;import java.util.UUID;
@Entity @Table(name="tariffs",uniqueConstraints=@UniqueConstraint(name="uk_tariff_tenant_code",columnNames={"tenant_id","code"}))
public class Tariff {
 @Id private UUID id;@Column(nullable=false)private UUID tenantId;@Column(nullable=false,length=60)private String code;@Column(nullable=false,length=150)private String name;
 @Column(nullable=false,precision=10,scale=4)private BigDecimal energyPricePerKwh;@Column(nullable=false,precision=10,scale=4)private BigDecimal timePricePerMinute;
 @Column(nullable=false,precision=10,scale=2)private BigDecimal sessionFee;@Column(nullable=false,precision=5,scale=2)private BigDecimal taxPercent;
 @Column(nullable=false,length=3)private String currency;@Enumerated(EnumType.STRING)@Column(nullable=false,length=20)private TariffStatus status;
 @Column(nullable=false)private Instant validFrom;private Instant validTo;@Column(nullable=false,updatable=false)private Instant createdAt;@Column(nullable=false)private Instant updatedAt;
 protected Tariff(){} public Tariff(UUID t,String c,String n,BigDecimal e,BigDecimal m,BigDecimal f,BigDecimal tax,String cur,Instant from,Instant to){id=UUID.randomUUID();tenantId=t;code=c;status=TariffStatus.DRAFT;createdAt=Instant.now();update(n,e,m,f,tax,cur,from,to);}
 public void update(String n,BigDecimal e,BigDecimal m,BigDecimal f,BigDecimal tax,String cur,Instant from,Instant to){name=n;energyPricePerKwh=e;timePricePerMinute=m;sessionFee=f;taxPercent=tax;currency=cur.toUpperCase();validFrom=from;validTo=to;updatedAt=Instant.now();}
 public void setStatus(TariffStatus s){status=s;updatedAt=Instant.now();}
 public UUID getId(){return id;}public UUID getTenantId(){return tenantId;}public String getCode(){return code;}public String getName(){return name;}public BigDecimal getEnergyPricePerKwh(){return energyPricePerKwh;}public BigDecimal getTimePricePerMinute(){return timePricePerMinute;}public BigDecimal getSessionFee(){return sessionFee;}public BigDecimal getTaxPercent(){return taxPercent;}public String getCurrency(){return currency;}public TariffStatus getStatus(){return status;}public Instant getValidFrom(){return validFrom;}public Instant getValidTo(){return validTo;}public Instant getCreatedAt(){return createdAt;}public Instant getUpdatedAt(){return updatedAt;}
}
