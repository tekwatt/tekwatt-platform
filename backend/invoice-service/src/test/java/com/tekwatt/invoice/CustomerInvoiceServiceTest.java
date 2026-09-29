package com.tekwatt.invoice;

import com.tekwatt.invoice.entity.Invoice;
import com.tekwatt.invoice.repository.InvoiceRepository;
import com.tekwatt.invoice.service.CustomerInvoiceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CustomerInvoiceServiceTest {
    @Test void listsOnlyInvoicesMatchingVerifiedCustomerAndWorkspace() {
        UUID tenant = UUID.randomUUID(), user = UUID.randomUUID();
        InvoiceRepository repository = mock(InvoiceRepository.class);
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://users/api/v1/users/me"))
                .andRespond(withSuccess(profile(tenant, user), MediaType.APPLICATION_JSON));
        when(repository.findAllByTenantIdAndUserIdOrderByCreatedAtDesc(tenant, user)).thenReturn(List.of(invoice(tenant, user)));
        var service = new CustomerInvoiceService(repository, builder, "http://users");
        assertThat(service.mine("Bearer customer")).hasSize(1);
        verify(repository).findAllByTenantIdAndUserIdOrderByCreatedAtDesc(tenant, user);
        server.verify();
    }

    @Test void rejectsAnotherCustomersInvoice() {
        UUID tenant = UUID.randomUUID(), user = UUID.randomUUID();
        Invoice foreign = invoice(tenant, UUID.randomUUID());
        InvoiceRepository repository = mock(InvoiceRepository.class);
        when(repository.findById(foreign.getId())).thenReturn(Optional.of(foreign));
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://users/api/v1/users/me"))
                .andRespond(withSuccess(profile(tenant, user), MediaType.APPLICATION_JSON));
        var service = new CustomerInvoiceService(repository, builder, "http://users");
        assertThatThrownBy(() -> service.mine("Bearer customer", foreign.getId()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        server.verify();
    }

    private static String profile(UUID tenant, UUID user) {
        return "{\"id\":\"" + user + "\",\"tenantId\":\"" + tenant + "\",\"status\":\"ACTIVE\"}";
    }

    private static Invoice invoice(UUID tenant, UUID user) {
        return new Invoice(tenant, user, UUID.randomUUID(), "INV-1", "Customer", "customer@example.com", null,
                null, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, "INR", LocalDate.now(), LocalDate.now().plusDays(1));
    }
}
