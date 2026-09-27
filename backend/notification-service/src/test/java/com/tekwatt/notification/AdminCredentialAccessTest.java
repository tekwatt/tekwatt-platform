package com.tekwatt.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tekwatt.notification.service.AdminCredentialAccess;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AdminCredentialAccessTest {
    @Test void acceptsAdminWithActiveLoginSession() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://auth/api/v1/auth/sessions"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        var access = new AdminCredentialAccess(builder, new ObjectMapper(), "http://auth", "http://admin", "");
        access.requireAdmin(token("ADMIN"), UUID.randomUUID());
        server.verify();
    }
    @Test void acceptsAllowlistedLegacyAdminOnlyForTheirActiveWorkspace() {
        UUID tenant = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://auth/api/v1/auth/sessions"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://admin/api/v1/admin/governance/administrators?tenantId=" + tenant))
                .andRespond(withSuccess("[{\"authUserId\":\"" + user + "\",\"email\":\"admin@tekwatt.in\",\"status\":\"ACTIVE\"}]", MediaType.APPLICATION_JSON));
        var access = new AdminCredentialAccess(builder, new ObjectMapper(), "http://auth", "http://admin", "admin@tekwatt.in");
        access.requireAdmin(token("DRIVER", user, "admin@tekwatt.in"), tenant);
        server.verify();
    }
    @Test void rejectsCustomerEvenWhenLoginSessionIsActive() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://auth/api/v1/auth/sessions"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        var access = new AdminCredentialAccess(builder, new ObjectMapper(), "http://auth", "http://admin", "");
        assertThatThrownBy(() -> access.requireAdmin(token("CUSTOMER"), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
        server.verify();
    }

    @Test void allowlistedEmailAloneDoesNotGrantAccessWithoutActiveWorkspaceRecord() {
        UUID tenant = UUID.randomUUID();
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://auth/api/v1/auth/sessions"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://admin/api/v1/admin/governance/administrators?tenantId=" + tenant))
                .andRespond(withSuccess("[{\"email\":\"admin@tekwatt.in\",\"status\":\"DISABLED\"}]", MediaType.APPLICATION_JSON));
        var access = new AdminCredentialAccess(builder, new ObjectMapper(), "http://auth", "http://admin", "admin@tekwatt.in");
        assertThatThrownBy(() -> access.requireAdmin(token("DRIVER", UUID.randomUUID(), "admin@tekwatt.in"), tenant))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        server.verify();
    }

    private String token(String role) {
        return token(role, UUID.randomUUID(), "customer@tekwatt.in");
    }
    private String token(String role, UUID user, String email) {
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"" + user + "\",\"email\":\"" + email + "\",\"roles\":[\"" + role + "\"]}").getBytes(StandardCharsets.UTF_8));
        return "Bearer header." + payload + ".signature";
    }
}
