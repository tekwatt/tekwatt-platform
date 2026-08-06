package com.tekwatt.tenant.dto;
import com.tekwatt.tenant.entity.TenantStatus;
import jakarta.validation.constraints.NotNull;
public record TenantStatusRequest(@NotNull TenantStatus status){}
