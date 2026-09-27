package com.tekwatt.notification.controller;

import com.tekwatt.notification.service.AdminCredentialAccess;
import com.tekwatt.notification.service.SmsProviderCredentialService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/provider-credentials")
public class SmsProviderCredentialController {
    private final SmsProviderCredentialService credentials;
    private final AdminCredentialAccess access;

    public SmsProviderCredentialController(SmsProviderCredentialService credentials, AdminCredentialAccess access) {
        this.credentials = credentials;
        this.access = access;
    }

    @GetMapping
    public List<SmsProviderCredentialService.Summary> list(@RequestParam UUID tenantId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        access.requireAdmin(authorization, tenantId);
        return credentials.list(tenantId);
    }

    @PutMapping
    public SmsProviderCredentialService.Summary save(@RequestParam UUID tenantId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody SmsProviderCredentialService.Request request) {
        access.requireAdmin(authorization, tenantId);
        return credentials.save(tenantId, request);
    }
}
