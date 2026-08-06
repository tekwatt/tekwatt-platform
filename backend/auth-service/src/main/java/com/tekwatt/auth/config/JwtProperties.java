package com.tekwatt.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tekwatt.auth.jwt")
public record JwtProperties(String secret, long accessTokenTtlSeconds, long refreshTokenTtlSeconds) { }
