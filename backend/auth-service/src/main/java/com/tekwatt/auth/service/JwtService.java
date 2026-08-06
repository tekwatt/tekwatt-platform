package com.tekwatt.auth.service;

import com.tekwatt.auth.config.JwtProperties;
import com.tekwatt.auth.entity.AppUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final SecretKey signingKey;
    public JwtService(JwtProperties properties) { this.properties = properties; this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8)); }
    public String createAccessToken(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getId().toString()).claim("email", user.getEmail()).claim("roles", new String[] {user.getRole()})
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(properties.accessTokenTtlSeconds()))).signWith(signingKey).compact();
    }
}
