package com.tekwatt.user;

import com.tekwatt.user.dto.UserResponse;
import com.tekwatt.user.entity.RfidCard;
import com.tekwatt.user.repository.RfidCardRepository;
import com.tekwatt.user.service.CurrentCustomerService;
import com.tekwatt.user.service.CustomerRfidService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CustomerRfidServiceTest {
    @Test
    void cardQueryUsesVerifiedCustomerAndWorkspace() {
        UUID tenant = UUID.randomUUID(), user = UUID.randomUUID();
        CurrentCustomerService customers = mock(CurrentCustomerService.class);
        RfidCardRepository cards = mock(RfidCardRepository.class);
        UserResponse profile = mock(UserResponse.class);
        RfidCard card = mock(RfidCard.class);
        when(profile.tenantId()).thenReturn(tenant);
        when(profile.id()).thenReturn(user);
        when(customers.me("Bearer customer")).thenReturn(profile);
        when(cards.findAllByTenantIdAndUserIdOrderByIssuedAtDesc(tenant, user)).thenReturn(List.of(card));
        var service = new CustomerRfidService(customers, cards);
        assertThat(service.mine("Bearer customer")).containsExactly(card);
        verify(cards).findAllByTenantIdAndUserIdOrderByIssuedAtDesc(tenant, user);
    }
}
