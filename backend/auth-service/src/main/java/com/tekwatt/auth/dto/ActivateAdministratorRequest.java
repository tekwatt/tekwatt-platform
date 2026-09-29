package com.tekwatt.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ActivateAdministratorRequest(@NotBlank @Email String email, @NotNull UUID authUserId) { }
