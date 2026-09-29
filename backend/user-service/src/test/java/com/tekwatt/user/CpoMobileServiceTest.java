package com.tekwatt.user;

import com.tekwatt.user.entity.Partner;
import com.tekwatt.user.repository.PartnerRepository;
import com.tekwatt.user.service.CpoMobileService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CpoMobileServiceTest {
    private final UUID userId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private final UUID tenantId = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Test
    void overviewOnlyReturnsChargersAndSessionsOwnedByAuthenticatedPartner() {
        PartnerRepository repository = mock(PartnerRepository.class);
        Partner partner = new Partner(tenantId, "Owner A", "A-001", "Operator", "owner@example.com",
                "123", BigDecimal.ZERO, "ACTIVE", "Chennai", "owner@example.com", userId);
        when(repository.findByAuthUserId(userId)).thenReturn(Optional.of(partner));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CpoMobileService service = new CpoMobileService(repository, builder, "https://auth.test", "https://charger.test", "https://session.test");
        server.expect(once(), requestTo("https://auth.test/api/v1/auth/identity"))
                .andRespond(withSuccess("{\"userId\":\"" + userId + "\",\"email\":\"owner@example.com\",\"role\":\"DRIVER\"}", MediaType.APPLICATION_JSON));
        String own = "30000000-0000-0000-0000-000000000003";
        String other = "40000000-0000-0000-0000-000000000004";
        server.expect(once(), requestTo("https://charger.test/api/v1/chargers?tenantId=" + tenantId))
                .andRespond(withSuccess("[{\"id\":\"" + own + "\",\"tenantId\":\"" + tenantId + "\",\"organizationId\":\"" + partner.getId() + "\",\"stationId\":\"OWN\",\"status\":\"AVAILABLE\"},{\"id\":\"" + other + "\",\"tenantId\":\"" + tenantId + "\",\"organizationId\":\"50000000-0000-0000-0000-000000000005\",\"stationId\":\"OTHER\"}]", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://session.test/api/v1/charging-sessions?tenantId=" + tenantId))
                .andRespond(withSuccess("[{\"id\":\"s1\",\"tenantId\":\"" + tenantId + "\",\"chargerId\":\"" + own + "\",\"status\":\"ACTIVE\"},{\"id\":\"s2\",\"tenantId\":\"" + tenantId + "\",\"chargerId\":\"" + other + "\",\"status\":\"ACTIVE\"}]", MediaType.APPLICATION_JSON));

        var overview = service.overview("Bearer test-token");
        assertEquals(1, overview.chargers().size());
        assertEquals("OWN", overview.chargers().getFirst().stationId());
        assertEquals(1, overview.sessions().size());
        assertEquals("s1", overview.sessions().getFirst().id());
        server.verify();
    }

    @Test
    void rejectsInvalidLoginBeforeLookingUpPartner() {
        PartnerRepository repository = mock(PartnerRepository.class);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CpoMobileService service = new CpoMobileService(repository, builder, "https://auth.test", "https://charger.test", "https://session.test");
        server.expect(once(), requestTo("https://auth.test/api/v1/auth/identity"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.me("Bearer bad-token"));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(repository);
        server.verify();
    }
}
