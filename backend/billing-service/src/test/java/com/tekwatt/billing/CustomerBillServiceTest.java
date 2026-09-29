package com.tekwatt.billing;

import com.tekwatt.billing.entity.Bill;
import com.tekwatt.billing.repository.BillRepository;
import com.tekwatt.billing.service.CustomerBillService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CustomerBillServiceTest {
    @Test void rejectsAnotherCustomersBill() {
        UUID tenant = UUID.randomUUID(), user = UUID.randomUUID();
        Bill foreign = new Bill(tenant, UUID.randomUUID(), UUID.randomUUID(), null, "BILL-1",
                BigDecimal.ONE, 10, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "INR");
        BillRepository repository = mock(BillRepository.class);
        when(repository.findById(foreign.getId())).thenReturn(Optional.of(foreign));
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://users/api/v1/users/me"))
                .andRespond(withSuccess("{\"id\":\"" + user + "\",\"tenantId\":\"" + tenant
                        + "\",\"status\":\"ACTIVE\"}", MediaType.APPLICATION_JSON));
        var service = new CustomerBillService(repository, builder, "http://users");
        assertThatThrownBy(() -> service.mine("Bearer customer", foreign.getId()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        server.verify();
    }
}
