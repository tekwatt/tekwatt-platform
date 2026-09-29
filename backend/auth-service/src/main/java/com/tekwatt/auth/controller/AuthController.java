package com.tekwatt.auth.controller;
import com.tekwatt.auth.dto.*;
import com.tekwatt.auth.service.AuthService;
import com.tekwatt.auth.service.Msg91OtpService;
import com.tekwatt.auth.service.PasswordResetService;
import com.tekwatt.auth.service.SmtpAdminAccess;
import com.tekwatt.auth.service.SmtpSettingsService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final SmtpSettingsService smtpSettings;
    private final SmtpAdminAccess smtpAdminAccess;
    private final Msg91OtpService msg91Otp;
    public AuthController(AuthService authService, PasswordResetService passwordResetService,
            SmtpSettingsService smtpSettings, SmtpAdminAccess smtpAdminAccess, Msg91OtpService msg91Otp) {
        this.authService = authService; this.passwordResetService = passwordResetService;
        this.smtpSettings = smtpSettings; this.smtpAdminAccess = smtpAdminAccess; this.msg91Otp = msg91Otp;
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) TokenResponse register(@Valid @RequestBody RegisterRequest request,HttpServletRequest client) { return authService.register(request,ip(client),client.getHeader("User-Agent")); }
    @PostMapping("/login") TokenResponse login(@Valid @RequestBody LoginRequest request,HttpServletRequest client) { return authService.login(request,ip(client),client.getHeader("User-Agent")); }
    @GetMapping("/otp/msg91/config") ResponseEntity<Msg91OtpService.WidgetConfig> msg91Config() {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(msg91Otp.widgetConfig());
    }
    @PostMapping("/otp/msg91/login") TokenResponse msg91Login(@Valid @RequestBody Msg91LoginRequest request,HttpServletRequest client) {
        msg91Otp.verifyEmail(request.accessToken(), request.email());
        return authService.loginVerifiedEmail(request.email(), ip(client), client.getHeader("User-Agent"));
    }
    @PostMapping("/otp/msg91/phone/login") TokenResponse msg91PhoneLogin(@Valid @RequestBody Msg91PhoneRequest request,HttpServletRequest client) {
        msg91Otp.verifyPhone(request.accessToken(), request.phone());
        return authService.loginVerifiedPhone(request.phone(), ip(client), client.getHeader("User-Agent"));
    }
    @GetMapping("/otp/msg91/phone") AuthService.VerifiedPhoneResponse verifiedPhone(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return authService.verifiedPhone(authorization);
    }
    @PostMapping("/otp/msg91/phone") AuthService.VerifiedPhoneResponse linkVerifiedPhone(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody Msg91PhoneRequest request) {
        authService.verifiedPhone(authorization);
        msg91Otp.verifyPhone(request.accessToken(), request.phone());
        return authService.linkVerifiedPhone(authorization, request.phone());
    }
    @PostMapping("/admin/activate") @ResponseStatus(HttpStatus.NO_CONTENT) void activateAdministrator(
            @RequestParam UUID tenantId, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody ActivateAdministratorRequest request) {
        smtpAdminAccess.requireAdmin(authorization, tenantId);
        smtpAdminAccess.requireActiveAdminRecord(tenantId, request.authUserId(), request.email());
        authService.activateAdministrator(request.email(), request.authUserId());
    }
    @GetMapping("/otp/msg91/settings") Msg91OtpService.SettingsSummary msg91Settings(
            @RequestParam UUID tenantId, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        smtpAdminAccess.requireAdmin(authorization, tenantId);
        return msg91Otp.status();
    }
    @PutMapping("/otp/msg91/settings") Msg91OtpService.SettingsSummary saveMsg91Settings(
            @RequestParam UUID tenantId, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody Msg91OtpSettingsRequest request) {
        smtpAdminAccess.requireAdmin(authorization, tenantId);
        return msg91Otp.save(request);
    }
    @PostMapping("/password-reset/request") @ResponseStatus(HttpStatus.NO_CONTENT) void requestPasswordReset(@Valid @RequestBody RequestPasswordReset request) { passwordResetService.request(request.email()); }
    @PostMapping("/password-reset/confirm") @ResponseStatus(HttpStatus.NO_CONTENT) void confirmPasswordReset(@Valid @RequestBody ConfirmPasswordReset request) {
        if (!passwordResetService.confirm(request)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The code is invalid or expired. Request a new code and try again.");
    }
    @GetMapping("/smtp-settings") SmtpSettingsService.Summary smtpStatus(
            @RequestParam UUID tenantId, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        smtpAdminAccess.requireAdmin(authorization, tenantId);
        return smtpSettings.status();
    }
    @PutMapping("/smtp-settings") SmtpSettingsService.Summary saveSmtp(
            @RequestParam UUID tenantId, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody SmtpSettingsRequest request) {
        smtpAdminAccess.requireAdmin(authorization, tenantId);
        return smtpSettings.save(request);
    }
    @PostMapping("/smtp-settings/test") @ResponseStatus(HttpStatus.NO_CONTENT) void testSmtp(
            @RequestParam UUID tenantId, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        String adminEmail = smtpAdminAccess.requireAdmin(authorization, tenantId);
        smtpSettings.sendTest(adminEmail);
    }
    @PostMapping("/refresh") TokenResponse refresh(@Valid @RequestBody RefreshRequest request,HttpServletRequest client) { return authService.refresh(request,ip(client),client.getHeader("User-Agent")); }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) void logout(@Valid @RequestBody LogoutRequest request){authService.logout(request);}
    @GetMapping("/sessions") List<UserSessionResponse> sessions(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization){return authService.sessions(authorization);}
    @GetMapping("/identity") AuthIdentityResponse identity(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization){return authService.identity(authorization);}
    @DeleteMapping("/sessions/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void revoke(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,@PathVariable UUID id){authService.revokeSession(authorization,id);}
    @PostMapping("/sessions/revoke-others") @ResponseStatus(HttpStatus.NO_CONTENT) void revokeOthers(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization){authService.revokeOtherSessions(authorization);}
    private String ip(HttpServletRequest request){String forwarded=request.getHeader("X-Forwarded-For");return forwarded==null||forwarded.isBlank()?request.getRemoteAddr():forwarded.split(",")[0].trim();}
}
