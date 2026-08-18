package com.tekwatt.auth.dto;
import java.time.Instant;
import java.util.UUID;
public record UserSessionResponse(UUID id,String device,String ipAddress,Instant createdAt,Instant lastUsedAt,Instant expiresAt,boolean current){}
