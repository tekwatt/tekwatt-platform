package com.tekwatt.auth.controller;
import com.tekwatt.auth.dto.*;
import com.tekwatt.auth.service.AuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) TokenResponse register(@Valid @RequestBody RegisterRequest request,HttpServletRequest client) { return authService.register(request,ip(client),client.getHeader("User-Agent")); }
    @PostMapping("/login") TokenResponse login(@Valid @RequestBody LoginRequest request,HttpServletRequest client) { return authService.login(request,ip(client),client.getHeader("User-Agent")); }
    @PostMapping("/refresh") TokenResponse refresh(@Valid @RequestBody RefreshRequest request,HttpServletRequest client) { return authService.refresh(request,ip(client),client.getHeader("User-Agent")); }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) void logout(@Valid @RequestBody LogoutRequest request){authService.logout(request);}
    @GetMapping("/sessions") List<UserSessionResponse> sessions(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization){return authService.sessions(authorization);}
    @DeleteMapping("/sessions/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void revoke(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,@PathVariable UUID id){authService.revokeSession(authorization,id);}
    @PostMapping("/sessions/revoke-others") @ResponseStatus(HttpStatus.NO_CONTENT) void revokeOthers(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization){authService.revokeOtherSessions(authorization);}
    private String ip(HttpServletRequest request){String forwarded=request.getHeader("X-Forwarded-For");return forwarded==null||forwarded.isBlank()?request.getRemoteAddr():forwarded.split(",")[0].trim();}
}
