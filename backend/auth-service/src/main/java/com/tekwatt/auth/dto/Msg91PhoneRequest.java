package com.tekwatt.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record Msg91PhoneRequest(@NotBlank String phone, @NotBlank String accessToken) { }
