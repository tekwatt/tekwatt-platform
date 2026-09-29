package com.tekwatt.auth.dto;

import java.util.UUID;

public record AuthIdentityResponse(UUID userId, String email, String role) {}
