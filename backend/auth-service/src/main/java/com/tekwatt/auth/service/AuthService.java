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
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AppUserRepository users; private final RefreshTokenRepository refreshTokens; private final PasswordEncoder passwordEncoder; private final JwtService jwtService; private final JwtProperties jwtProperties;
    public AuthService(AppUserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder, JwtService jwtService, JwtProperties jwtProperties) { this.users = users; this.refreshTokens = refreshTokens; this.passwordEncoder = passwordEncoder; this.jwtService = jwtService; this.jwtProperties = jwtProperties; }
    @Transactional public TokenResponse register(RegisterRequest request,String ipAddress,String userAgent) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email address is already registered");
        return issueTokens(users.save(new AppUser(email, passwordEncoder.encode(request.password()), "DRIVER")),ipAddress,userAgent);
    }
    @Transactional public TokenResponse login(LoginRequest request,String ipAddress,String userAgent) {
        AppUser user = users.findByEmailIgnoreCase(request.email().trim()).filter(AppUser::isEnabled).filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash())).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        return issueTokens(user,ipAddress,userAgent);
    }
    @Transactional public TokenResponse loginVerifiedEmail(String email,String ipAddress,String userAgent) {
        AppUser user = users.findByEmailIgnoreCase(email.trim()).filter(AppUser::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No active TekWatt account matches the verified email"));
        return issueTokens(user,ipAddress,userAgent);
    }
    @Transactional public TokenResponse loginVerifiedPhone(String phone,String ipAddress,String userAgent) {
        String normalized = Msg91OtpService.normalizePhone(phone);
        AppUser user = users.findByVerifiedPhone(normalized).filter(AppUser::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No active TekWatt account has this verified phone"));
        return issueTokens(user,ipAddress,userAgent);
    }
    @Transactional(readOnly=true) public VerifiedPhoneResponse verifiedPhone(String authorization) {
        var identity = activeIdentity(authorization);
        AppUser user = users.findById(identity.userId()).filter(AppUser::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is not active"));
        return new VerifiedPhoneResponse(user.getVerifiedPhone() == null ? "" : "+" + user.getVerifiedPhone(), user.getPhoneVerifiedAt());
    }
    @Transactional public VerifiedPhoneResponse linkVerifiedPhone(String authorization,String phone) {
        var identity = activeIdentity(authorization);
        String normalized = Msg91OtpService.normalizePhone(phone);
        AppUser user = users.findById(identity.userId()).filter(AppUser::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is not active"));
        if (users.findByVerifiedPhone(normalized).filter(other -> !other.getId().equals(user.getId())).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This phone is already linked to another account");
        user.linkVerifiedPhone(normalized);
        try { users.flush(); }
        catch (DataIntegrityViolationException conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This phone is already linked to another account");
        }
        return new VerifiedPhoneResponse("+" + normalized, user.getPhoneVerifiedAt());
    }
    public record VerifiedPhoneResponse(String phone, Instant verifiedAt) { }
    @Transactional public void activateAdministrator(String email, UUID authUserId) {
        AppUser user = users.findById(authUserId).filter(AppUser::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Authentication account not found"));
        if (!user.getEmail().equalsIgnoreCase(email.trim()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Administrator email does not match the authentication account");
        user.changeRole("ADMIN");
        refreshTokens.findAllByUser_IdOrderByCreatedAtDesc(user.getId()).forEach(RefreshToken::revoke);
    }
    @Transactional public TokenResponse refresh(RefreshRequest request,String ipAddress,String userAgent) {
        RefreshToken token = refreshTokens.findByToken(request.refreshToken()).filter(candidate -> !candidate.isRevoked()).filter(candidate -> candidate.getExpiresAt().isAfter(Instant.now())).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        token.revoke(); return issueTokens(token.getUser(),ipAddress,userAgent);
    }
    @Transactional(readOnly=true) public List<UserSessionResponse> sessions(String authorization){var identity=activeIdentity(authorization);var now=Instant.now();return refreshTokens.findAllByUser_IdOrderByCreatedAtDesc(identity.userId()).stream().filter(token->!token.isRevoked()&&token.getExpiresAt().isAfter(now)).map(token->map(token,identity.sessionId())).toList();}
    @Transactional(readOnly=true) public AuthIdentityResponse identity(String authorization){var active=activeIdentity(authorization);var user=users.findById(active.userId()).filter(AppUser::isEnabled).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Account is not active"));return new AuthIdentityResponse(user.getId(),user.getEmail(),user.getRole());}
    @Transactional public void revokeSession(String authorization,UUID id){var identity=activeIdentity(authorization);var token=refreshTokens.findByIdAndUser_Id(id,identity.userId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User session not found"));if(token.getId().equals(identity.sessionId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Use sign out to revoke the current session");if(!token.isRevoked())token.revoke();}
    @Transactional public void revokeOtherSessions(String authorization){var identity=activeIdentity(authorization);refreshTokens.findAllByUser_IdOrderByCreatedAtDesc(identity.userId()).stream().filter(token->!token.getId().equals(identity.sessionId())&&!token.isRevoked()).forEach(RefreshToken::revoke);}
    @Transactional public void logout(LogoutRequest request){refreshTokens.findByToken(request.refreshToken()).filter(token->!token.isRevoked()).ifPresent(RefreshToken::revoke);}
    private UserSessionResponse map(RefreshToken token,UUID currentId){return new UserSessionResponse(token.getId(),device(token.getUserAgent()),token.getIpAddress(),token.getCreatedAt(),token.getLastUsedAt(),token.getExpiresAt(),token.getId().equals(currentId));}
    private JwtService.AccessIdentity activeIdentity(String authorization){var identity=jwtService.parse(authorization);refreshTokens.findByIdAndUser_Id(identity.sessionId(),identity.userId()).filter(token->!token.isRevoked()&&token.getExpiresAt().isAfter(Instant.now())).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Login session is no longer active"));return identity;}
    private String device(String userAgent){if(userAgent==null||userAgent.isBlank())return "Unknown device";String browser=userAgent.contains("Edg/")?"Microsoft Edge":userAgent.contains("Chrome/")?"Google Chrome":userAgent.contains("Firefox/")?"Mozilla Firefox":userAgent.contains("Safari/")?"Safari":"Browser";String os=userAgent.contains("Windows")?"Windows":userAgent.contains("Android")?"Android":userAgent.contains("iPhone")||userAgent.contains("iPad")?"iOS":userAgent.contains("Mac OS")?"macOS":userAgent.contains("Linux")?"Linux":"Unknown OS";return browser+" on "+os;}
    private TokenResponse issueTokens(AppUser user,String ipAddress,String userAgent) {
        byte[] bytes = new byte[48]; RANDOM.nextBytes(bytes); String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken session=refreshTokens.save(new RefreshToken(UUID.randomUUID(), user, refreshToken, Instant.now().plusSeconds(jwtProperties.refreshTokenTtlSeconds()),ipAddress,userAgent));
        return new TokenResponse(jwtService.createAccessToken(user,session.getId()), refreshToken, "Bearer", jwtProperties.accessTokenTtlSeconds());
    }
}
