package com.tekwatt.organization.dto;import jakarta.validation.constraints.*;import java.util.UUID;
public record OrganizationRequest(@NotNull UUID tenantId,UUID parentId,@NotBlank @Size(max=150) String name,@NotBlank @Pattern(regexp="^[A-Za-z0-9_-]+$") @Size(max=60) String code){}
