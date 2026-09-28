package com.tekwatt.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestPasswordReset(@NotBlank @Email String email) { }
