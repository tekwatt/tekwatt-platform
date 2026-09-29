package com.tekwatt.connector.controller;

import com.tekwatt.connector.dto.ConnectorResponse;
import com.tekwatt.connector.service.CustomerConnectorService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/connectors/my")
public class CustomerConnectorController {
    private final CustomerConnectorService service;

    public CustomerConnectorController(CustomerConnectorService service) { this.service = service; }

    @GetMapping
    public List<ConnectorResponse> mine(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam UUID chargerId) {
        return service.mine(authorization, chargerId);
    }
}
