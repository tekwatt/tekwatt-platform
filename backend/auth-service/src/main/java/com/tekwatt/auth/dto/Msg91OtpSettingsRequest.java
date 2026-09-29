package com.tekwatt.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Msg91OtpSettingsRequest(
        @NotBlank @Size(max = 128) String widgetId,
        @Size(max = 4096) String tokenAuth,
        @Size(max = 2048) String serverAuthKey) { }
