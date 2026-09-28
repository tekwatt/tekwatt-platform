package com.tekwatt.auth.dto;

import jakarta.validation.constraints.*;

public record SmtpSettingsRequest(
        @NotBlank @Size(max = 255) String host,
        @Min(1) @Max(65535) int port,
        @NotBlank String securityMode,
        @NotBlank @Size(max = 254) String username,
        @Size(max = 1024) String password,
        @NotBlank @Email @Size(max = 254) String fromEmail,
        @Email @Size(max = 254) String replyTo) { }
