package com.tekwatt.invoice.controller;

import com.tekwatt.invoice.dto.InvoiceResponse;
import com.tekwatt.invoice.service.CustomerInvoiceService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices/my")
public class CustomerInvoiceController {
    private final CustomerInvoiceService invoices;
    public CustomerInvoiceController(CustomerInvoiceService invoices) { this.invoices = invoices; }

    @GetMapping
    public List<InvoiceResponse> mine(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return invoices.mine(authorization);
    }

    @GetMapping("/{invoiceId}")
    public InvoiceResponse mine(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID invoiceId) {
        return invoices.mine(authorization, invoiceId);
    }
}
