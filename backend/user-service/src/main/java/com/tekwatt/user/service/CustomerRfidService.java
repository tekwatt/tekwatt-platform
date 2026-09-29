package com.tekwatt.user.service;

import com.tekwatt.user.entity.RfidCard;
import com.tekwatt.user.repository.RfidCardRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerRfidService {
    private final CurrentCustomerService customers;
    private final RfidCardRepository cards;

    public CustomerRfidService(CurrentCustomerService customers, RfidCardRepository cards) {
        this.customers = customers;
        this.cards = cards;
    }

    @Transactional(readOnly = true)
    public List<RfidCard> mine(String authorization) {
        var customer = customers.me(authorization);
        return cards.findAllByTenantIdAndUserIdOrderByIssuedAtDesc(customer.tenantId(), customer.id());
    }
}
