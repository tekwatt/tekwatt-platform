package com.tekwatt.payment.controller;

import com.tekwatt.payment.entity.Wallet;
import com.tekwatt.payment.entity.WalletEntry;
import com.tekwatt.payment.service.CustomerWalletService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/my/wallet")
public class CustomerWalletController {
    private final CustomerWalletService service;

    public CustomerWalletController(CustomerWalletService service) { this.service = service; }

    @GetMapping
    public Wallet mine(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.mine(authorization);
    }

    @PostMapping
    public Wallet create(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.create(authorization);
    }

    @GetMapping("/entries")
    public List<WalletEntry> entries(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.entries(authorization);
    }
}
