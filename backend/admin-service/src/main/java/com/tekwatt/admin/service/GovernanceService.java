package com.tekwatt.admin.service;

import com.tekwatt.admin.dto.*;
import com.tekwatt.admin.entity.*;
import com.tekwatt.admin.repository.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class GovernanceService {
    public static final Map<String, Set<String>> DEFAULT_ROLE_PERMISSIONS = Map.of(
        "ADMIN", Set.of("Dashboard", "Customers", "Partners", "Technicians", "Stations", "Chargers", "Connectors", "Live Monitoring", "Map View", "Tariff Management", "RFID Cards", "Coupons & Promos", "Charging Sessions", "Transactions", "Scan & Pay", "Wallet", "Invoices", "Payment Gateways", "Support Tickets", "Maintenance", "AMC Contracts", "Reports", "Diagnostics", "OCPP Logs", "Firmware", "Roles", "Administrators", "API Keys", "Settings"),
        "PARTNER", Set.of("Dashboard", "Customers", "Stations", "Chargers", "Connectors", "Live Monitoring", "Map View", "Tariff Management", "RFID Cards", "Charging Sessions", "Transactions", "Scan & Pay", "Wallet", "Invoices", "Support Tickets", "Reports"),
        "TECHNICIAN", Set.of("Dashboard", "Stations", "Chargers", "Connectors", "Live Monitoring", "Map View", "Support Tickets", "Maintenance", "Diagnostics", "OCPP Logs", "Firmware"),
        "CUSTOMER", Set.of("Dashboard", "Stations", "Map View", "RFID Cards", "Coupons & Promos", "Charging Sessions", "Transactions", "Scan & Pay", "Wallet", "Invoices", "Support Tickets")
    );

    private final RolePolicyRepository roles;
    private final AdministratorRepository admins;
    private final AdminApiKeyRepository keys;
    private final PlatformSettingRepository settings;
    private final SecureRandom random = new SecureRandom();

    public GovernanceService(RolePolicyRepository roles, AdministratorRepository admins, AdminApiKeyRepository keys, PlatformSettingRepository settings) {
        this.roles = roles;
        this.admins = admins;
        this.keys = keys;
        this.settings = settings;
    }

    public RolePolicy role(UUID tenantId, RolePolicyRequest request) {
        String roleName = request.roleName().toUpperCase();
        RolePolicy policy = roles.findByTenantIdAndRoleNameIgnoreCase(tenantId, roleName)
            .orElseGet(() -> new RolePolicy(tenantId, roleName, DEFAULT_ROLE_PERMISSIONS.containsKey(roleName)));
        policy.update(request.permissions());
        return roles.save(policy);
    }

    public List<RolePolicy> roles(UUID tenantId) {
        DEFAULT_ROLE_PERMISSIONS.forEach((name, permissions) ->
            roles.findByTenantIdAndRoleNameIgnoreCase(tenantId, name).orElseGet(() -> {
                RolePolicy policy = new RolePolicy(tenantId, name, true);
                policy.update(permissions);
                return roles.save(policy);
            }));
        return roles.findAllByTenantIdOrderByRoleName(tenantId);
    }

    public Administrator admin(UUID tenant, UUID id, AdministratorRequest request) {
        Administrator admin = id == null
            ? new Administrator(tenant, request.authUserId(), request.name(), request.email(), request.phone(), request.roleName())
            : admins.findById(id).orElseThrow(() -> notFound("Administrator"));
        if (id != null) admin.update(request.name(), request.email(), request.phone(), request.roleName(), request.status() == null ? "ACTIVE" : request.status());
        return admins.save(admin);
    }

    @Transactional(readOnly = true)
    public List<Administrator> admins(UUID tenantId) { return admins.findAllByTenantIdOrderByCreatedAtDesc(tenantId); }
    public void deleteAdmin(UUID id) { admins.deleteById(id); }

    public ApiKeyResponse createKey(UUID tenant, ApiKeyRequest request) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String keyId = "tw_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        AdminApiKey key = keys.save(new AdminApiKey(tenant, keyId, hash(secret), request.name(), request.roleName(), request.scopes()));
        return ApiKeyResponse.from(key, secret);
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> keys(UUID tenantId) { return keys.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(key -> ApiKeyResponse.from(key, null)).toList(); }
    public ApiKeyResponse disableKey(UUID id) { AdminApiKey key = keys.findById(id).orElseThrow(() -> notFound("API key")); key.disable(); return ApiKeyResponse.from(key, null); }

    public Map<String, String> saveSettings(UUID tenant, Map<String, String> values) {
        values.forEach((name, value) -> { PlatformSetting setting = settings.findByTenantIdAndSettingKey(tenant, name).orElseGet(() -> new PlatformSetting(tenant, name)); setting.value(value); settings.save(setting); });
        return getSettings(tenant);
    }

    @Transactional(readOnly = true)
    public Map<String, String> getSettings(UUID tenantId) { Map<String, String> result = new TreeMap<>(); settings.findAllByTenantId(tenantId).forEach(setting -> result.put(setting.getSettingKey(), setting.getSettingValue())); return result; }

    private String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); } }
    private ResponseStatusException notFound(String name) { return new ResponseStatusException(HttpStatus.NOT_FOUND, name + " not found"); }
}
