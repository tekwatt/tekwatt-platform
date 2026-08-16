package com.tekwatt.admin.dto;import jakarta.validation.constraints.*;import java.util.*;public record RolePolicyRequest(@NotBlank String roleName,@NotNull Set<String>permissions){}
