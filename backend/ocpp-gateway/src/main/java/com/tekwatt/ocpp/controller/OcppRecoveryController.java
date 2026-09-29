package com.tekwatt.ocpp.controller;

import com.tekwatt.ocpp.service.OcppCommandService;
import com.tekwatt.ocpp.service.OperatorCommandAuthorization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ocpp/commands")
public class OcppRecoveryController {
    private final OcppCommandService commands;
    private final OperatorCommandAuthorization access;
    public OcppRecoveryController(OcppCommandService commands, OperatorCommandAuthorization access) {
        this.commands = commands;
        this.access = access;
    }
    public record StatusRequest(@NotBlank String stationId, @NotBlank String ocppVersion) {}
    @PostMapping("/status")
    public Map<String,String> status(@RequestHeader(name=HttpHeaders.AUTHORIZATION, required=false) String authorization,
            @RequestHeader(name="X-Tekwatt-Internal-Command-Key", required=false) String serviceKey,
            @Valid @RequestBody StatusRequest request) {
        access.requireAdministrator(authorization, request.stationId(), serviceKey);
        return Map.of("messageId", commands.requestStatus(request.stationId(), request.ocppVersion()));
    }
}
