package com.tekwatt.payment.service;

import com.tekwatt.payment.entity.Wallet;
import com.tekwatt.payment.repository.WalletEntryRepository;
import com.tekwatt.payment.repository.WalletRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CustomerWalletServiceTest {
    @Test
    void createsWalletOnlyForAuthenticatedCustomer() {
        UUID tenant = UUID.randomUUID(), user = UUID.randomUUID();
        WalletRepository wallets = mock(WalletRepository.class);
        WalletEntryRepository entries = mock(WalletEntryRepository.class);
        when(wallets.findByTenantIdAndUserId(tenant, user)).thenReturn(Optional.empty());
        when(wallets.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://users/api/v1/users/me"))
                .andRespond(withSuccess("{\"id\":\"" + user + "\",\"tenantId\":\"" + tenant
                        + "\",\"status\":\"ACTIVE\"}", MediaType.APPLICATION_JSON));
        var service = new CustomerWalletService(wallets, entries, builder, "http://users");
        Wallet wallet = service.create("Bearer customer");
        assertThat(wallet.getTenantId()).isEqualTo(tenant);
        assertThat(wallet.getUserId()).isEqualTo(user);
        assertThat(wallet.getCurrency()).isEqualTo("INR");
        server.verify();
    }
}
