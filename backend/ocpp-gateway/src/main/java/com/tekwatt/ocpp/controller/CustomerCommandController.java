package com.tekwatt.ocpp.controller;

import com.tekwatt.ocpp.service.CustomerCommandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ocpp/customer-commands")
public class CustomerCommandController {
    private final CustomerCommandService service;
    public CustomerCommandController(CustomerCommandService service) { this.service = service; }

    @PostMapping("/start")
    public Map<String, String> start(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody Start request) {
        return Map.of("messageId", service.start(authorization, request.chargerId(), request.connectorId()));
    }

    @PostMapping("/stop")
    public Map<String, String> stop(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody Stop request) {
        return Map.of("messageId", service.stop(authorization, request.sessionId()));
    }

    @GetMapping("/{messageId}/result")
    public com.tekwatt.ocpp.service.OcppCommandTracker.Result result(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization, @PathVariable String messageId) {
        return service.result(authorization, messageId);
    }

    public record Start(@NotNull UUID chargerId, @NotNull UUID connectorId) { }
    public record Stop(@NotNull UUID sessionId) { }
}
