package com.tekwatt.user.dto;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record CreateUserRequest(@NotNull UUID authUserId, @NotNull UUID tenantId, @NotBlank @Size(max=100) String firstName, @NotBlank @Size(max=100) String lastName, @NotBlank @Email @Size(max=254) String email, @Size(max=32) String phone) { }
