package com.tekwatt.tenant.dto;
import com.tekwatt.tenant.entity.*;
import java.time.Instant;
import java.util.UUID;
public record TenantResponse(UUID id,String name,String slug,String contactEmail,TenantStatus status,Instant createdAt,Instant updatedAt){public static TenantResponse from(Tenant t){return new TenantResponse(t.getId(),t.getName(),t.getSlug(),t.getContactEmail(),t.getStatus(),t.getCreatedAt(),t.getUpdatedAt());}}
