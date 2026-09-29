package com.tekwatt.charger.controller;

import com.tekwatt.charger.dto.ChargerResponse;
import com.tekwatt.charger.service.CustomerChargerService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chargers/my")
public class CustomerChargerController {
    private final CustomerChargerService service;

    public CustomerChargerController(CustomerChargerService service) { this.service = service; }

    @GetMapping
    public List<ChargerResponse> mine(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.mine(authorization);
    }
}
