package com.tekwatt.auth.service;

import com.tekwatt.auth.dto.ConfirmPasswordReset;
import com.tekwatt.auth.entity.AppUser;
import com.tekwatt.auth.entity.PasswordResetCode;
import com.tekwatt.auth.repository.AppUserRepository;
import com.tekwatt.auth.repository.PasswordResetCodeRepository;
import com.tekwatt.auth.repository.RefreshTokenRepository;
import java.security.SecureRandom;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordResetService {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String INVALID_CODE = "The code is invalid or expired. Request a new code and try again.";
    private final AppUserRepository users;
    private final PasswordResetCodeRepository codes;
    private final RefreshTokenRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final SmtpSettingsService smtp;

    public PasswordResetService(AppUserRepository users, PasswordResetCodeRepository codes,
            RefreshTokenRepository sessions, PasswordEncoder passwordEncoder,
            SmtpSettingsService smtp) {
        this.users = users; this.codes = codes; this.sessions = sessions;
        this.passwordEncoder = passwordEncoder; this.smtp = smtp;
    }

    // Always return the same response, including for unknown or disabled accounts.
    @Transactional
    public void request(String rawEmail) {
        String email = rawEmail.trim().toLowerCase();
        AppUser user = users.findForReset(email).filter(AppUser::isEnabled).orElse(null);
        if (user == null) return;
        Instant now = Instant.now();
        PasswordResetCode existing = codes.findByUser_Id(user.getId()).orElse(null);
        if (existing != null && existing.getRequestedAt().plusSeconds(60).isAfter(now)) return;
        String code = String.format("%08d", RANDOM.nextInt(100_000_000));
        try {
            smtp.send(user.getEmail(), "TekWatt password reset code", "Your TekWatt password reset code is " + code
                    + ". It expires in 10 minutes. If you did not request this, ignore this email.");
        } catch (RuntimeException failure) {
            LOG.warn("Password reset email could not be delivered ({})", failure.getClass().getSimpleName());
            return;
        }
        String hash = passwordEncoder.encode(code);
        if (existing == null) codes.save(new PasswordResetCode(user, hash, now));
        else existing.renew(hash, now);
    }

    @Transactional
    public boolean confirm(ConfirmPasswordReset request) {
        AppUser user = users.findForReset(request.email().trim().toLowerCase())
                .filter(AppUser::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_CODE));
        PasswordResetCode reset = codes.findLockedByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_CODE));
        Instant now = Instant.now();
        if (reset.getExpiresAt().isBefore(now) || reset.getAttempts() >= 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_CODE);
        }
        if (!passwordEncoder.matches(request.code(), reset.getCodeHash())) {
            reset.failedAttempt();
            // Do not throw here: the failed-attempt counter must be committed.
            return false;
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        sessions.findAllByUser_IdOrderByCreatedAtDesc(user.getId()).forEach(token -> {
            if (!token.isRevoked()) token.revoke();
        });
        codes.delete(reset);
        return true;
    }
}
