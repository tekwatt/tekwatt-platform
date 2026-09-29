package com.tekwatt.user.controller;

import com.tekwatt.user.entity.RfidCard;
import com.tekwatt.user.service.CustomerRfidService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/rfid-cards")
public class CustomerRfidController {
    private final CustomerRfidService service;

    public CustomerRfidController(CustomerRfidService service) { this.service = service; }

    @GetMapping
    public List<RfidCard> mine(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.mine(authorization);
    }
}
