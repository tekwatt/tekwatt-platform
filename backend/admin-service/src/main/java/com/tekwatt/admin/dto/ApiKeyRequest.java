package com.tekwatt.admin.dto;import jakarta.validation.constraints.*;public record ApiKeyRequest(@NotBlank String name,@NotBlank String roleName,@NotBlank String scopes){}
