package com.tekwatt.ocpp;

import com.tekwatt.ocpp.service.OperatorCommandAuthorization;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OperatorCommandAuthorizationTest {
    private static final String STATION = "CHARGER-1";

    @Test void rejectsMissingBearerBeforeLookingUpCharger() {
        OperatorCommandAuthorization access = new OperatorCommandAuthorization(RestClient.builder(),
                "http://auth", "http://chargers", "http://admins", "");
        assertThatThrownBy(() -> access.requireAdministrator(null, STATION, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("401");
    }

    @Test void rejectsCustomerEvenWithActiveSession() {
        UUID tenant = UUID.randomUUID();
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://auth/api/v1/auth/identity"))
                .andRespond(withSuccess("{\"userId\":\"customer-1\",\"email\":\"customer@example.com\",\"role\":\"DRIVER\"}", MediaType.APPLICATION_JSON));
        var access = new OperatorCommandAuthorization(builder, "http://auth", "http://chargers", "http://admins", "");
        assertThatThrownBy(() -> access.requireAdministrator("Bearer active-customer", STATION, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        server.verify();
    }

    @Test void permitsOnlyActiveAdministratorOfThatChargersWorkspace() {
        UUID tenant = UUID.randomUUID();
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://auth/api/v1/auth/identity"))
                .andRespond(withSuccess("{\"userId\":\"admin-1\",\"email\":\"admin@example.com\",\"role\":\"ADMIN\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://chargers/api/v1/chargers/by-station/" + STATION))
                .andRespond(withSuccess("{\"tenantId\":\"" + tenant + "\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://admins/api/v1/admin/governance/administrators?tenantId=" + tenant))
                .andRespond(withSuccess("[{\"tenantId\":\"" + tenant
                        + "\",\"authUserId\":\"admin-1\",\"email\":\"admin@example.com\",\"status\":\"ACTIVE\"}]",
                        MediaType.APPLICATION_JSON));
        var access = new OperatorCommandAuthorization(builder, "http://auth", "http://chargers", "http://admins", "");
        access.requireAdministrator("Bearer active-admin", STATION, null);
        server.verify();
    }

    @Test void rejectsAdministratorFromAnotherWorkspace() {
        UUID tenant = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://auth/api/v1/auth/identity"))
                .andRespond(withSuccess("{\"userId\":\"admin-1\",\"email\":\"admin@example.com\",\"role\":\"ADMIN\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://chargers/api/v1/chargers/by-station/" + STATION))
                .andRespond(withSuccess("{\"tenantId\":\"" + tenant + "\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://admins/api/v1/admin/governance/administrators?tenantId=" + tenant))
                .andRespond(withSuccess("[{\"tenantId\":\"" + otherTenant
                        + "\",\"authUserId\":\"admin-1\",\"status\":\"ACTIVE\"}]", MediaType.APPLICATION_JSON));
        var access = new OperatorCommandAuthorization(builder, "http://auth", "http://chargers", "http://admins", "");
        assertThatThrownBy(() -> access.requireAdministrator("Bearer active-admin", STATION, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        server.verify();
    }

    @Test void doesNotTrustAnUnlinkedAdministratorEmail() {
        UUID tenant = UUID.randomUUID();
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://auth/api/v1/auth/identity"))
                .andRespond(withSuccess("{\"userId\":\"user-1\",\"email\":\"admin@example.com\",\"role\":\"ADMIN\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://chargers/api/v1/chargers/by-station/" + STATION))
                .andRespond(withSuccess("{\"tenantId\":\"" + tenant + "\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://admins/api/v1/admin/governance/administrators?tenantId=" + tenant))
                .andRespond(withSuccess("[{\"tenantId\":\"" + tenant
                        + "\",\"email\":\"admin@example.com\",\"status\":\"ACTIVE\"}]", MediaType.APPLICATION_JSON));
        var access = new OperatorCommandAuthorization(builder, "http://auth", "http://chargers", "http://admins", "");
        assertThatThrownBy(() -> access.requireAdministrator("Bearer active-account", STATION, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        server.verify();
    }

    @Test void acceptsConfiguredInternalServiceKeyWithoutUserBearer() {
        var access = new OperatorCommandAuthorization(RestClient.builder(),
                "http://auth", "http://chargers", "http://admins", "private-service-key");
        access.requireAdministrator(null, STATION, "private-service-key");
        assertThatThrownBy(() -> access.requireAdministrator(null, STATION, "wrong-key"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("401");
    }
}
