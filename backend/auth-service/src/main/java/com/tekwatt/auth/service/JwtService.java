package com.tekwatt.auth.service;

import com.tekwatt.auth.config.JwtProperties;
import com.tekwatt.auth.entity.AppUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
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
    public String createAccessToken(AppUser user, java.util.UUID sessionId) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getId().toString()).claim("sid",sessionId.toString()).claim("email", user.getEmail()).claim("roles", new String[] {user.getRole()})
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(properties.accessTokenTtlSeconds()))).signWith(signingKey).compact();
    }
    public AccessIdentity parse(String authorization){
        if(authorization==null||!authorization.startsWith("Bearer "))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,"Bearer token is required");
        try{var claims=Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(authorization.substring(7)).getPayload();String sessionId=claims.get("sid",String.class);if(sessionId==null)throw new IllegalArgumentException("Session claim is missing");return new AccessIdentity(java.util.UUID.fromString(claims.getSubject()),java.util.UUID.fromString(sessionId));}
        catch(JwtException|IllegalArgumentException e){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,"Invalid or expired access token");}
    }
    public record AccessIdentity(java.util.UUID userId,java.util.UUID sessionId){}
}
