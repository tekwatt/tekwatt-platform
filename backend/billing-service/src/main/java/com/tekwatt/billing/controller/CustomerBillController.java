package com.tekwatt.billing.controller;

import com.tekwatt.billing.dto.BillResponse;
import com.tekwatt.billing.service.CustomerBillService;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bills/my")
public class CustomerBillController {
    private final CustomerBillService bills;
    public CustomerBillController(CustomerBillService bills) { this.bills = bills; }

    @GetMapping("/{billId}")
    public BillResponse mine(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization, @PathVariable UUID billId) {
        return bills.mine(authorization, billId);
    }
}
