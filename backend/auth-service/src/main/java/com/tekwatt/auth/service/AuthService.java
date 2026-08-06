package com.tekwatt.auth.service;

import com.tekwatt.auth.config.JwtProperties;
import com.tekwatt.auth.dto.*;
import com.tekwatt.auth.entity.AppUser;
import com.tekwatt.auth.entity.RefreshToken;
import com.tekwatt.auth.repository.AppUserRepository;
import com.tekwatt.auth.repository.RefreshTokenRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AppUserRepository users; private final RefreshTokenRepository refreshTokens; private final PasswordEncoder passwordEncoder; private final JwtService jwtService; private final JwtProperties jwtProperties;
    public AuthService(AppUserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder, JwtService jwtService, JwtProperties jwtProperties) { this.users = users; this.refreshTokens = refreshTokens; this.passwordEncoder = passwordEncoder; this.jwtService = jwtService; this.jwtProperties = jwtProperties; }
    @Transactional public TokenResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email address is already registered");
        return issueTokens(users.save(new AppUser(email, passwordEncoder.encode(request.password()), "DRIVER")));
    }
    @Transactional public TokenResponse login(LoginRequest request) {
        AppUser user = users.findByEmailIgnoreCase(request.email().trim()).filter(AppUser::isEnabled).filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash())).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        return issueTokens(user);
    }
    @Transactional public TokenResponse refresh(RefreshRequest request) {
        RefreshToken token = refreshTokens.findByToken(request.refreshToken()).filter(candidate -> !candidate.isRevoked()).filter(candidate -> candidate.getExpiresAt().isAfter(Instant.now())).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        token.revoke(); return issueTokens(token.getUser());
    }
    private TokenResponse issueTokens(AppUser user) {
        byte[] bytes = new byte[48]; RANDOM.nextBytes(bytes); String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(new RefreshToken(UUID.randomUUID(), user, refreshToken, Instant.now().plusSeconds(jwtProperties.refreshTokenTtlSeconds())));
        return new TokenResponse(jwtService.createAccessToken(user), refreshToken, "Bearer", jwtProperties.accessTokenTtlSeconds());
    }
}
