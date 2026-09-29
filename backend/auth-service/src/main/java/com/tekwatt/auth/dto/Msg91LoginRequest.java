package com.tekwatt.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Msg91LoginRequest(@NotBlank @Email String email,
                                @NotBlank @Size(max = 8192) String accessToken) { }
