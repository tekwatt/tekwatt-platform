package com.tekwatt.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConfirmPasswordReset(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "[0-9]{8}") String code,
        @NotBlank @Size(min = 12, max = 72) String newPassword) { }
