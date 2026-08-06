package com.tekwatt.tenant.dto;
import jakarta.validation.constraints.*;
public record TenantRequest(@NotBlank @Size(max=150) String name,@NotBlank @Pattern(regexp="^[a-z0-9]+(?:-[a-z0-9]+)*$") @Size(max=80) String slug,@NotBlank @Email @Size(max=254) String contactEmail){}
