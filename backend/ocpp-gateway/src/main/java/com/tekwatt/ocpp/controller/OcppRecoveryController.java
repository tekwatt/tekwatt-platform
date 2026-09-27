package com.tekwatt.ocpp.controller;

import com.tekwatt.ocpp.service.OcppCommandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ocpp/commands")
public class OcppRecoveryController {
    private final OcppCommandService commands;
    public OcppRecoveryController(OcppCommandService commands) { this.commands = commands; }
    public record StatusRequest(@NotBlank String stationId, @NotBlank String ocppVersion) {}
    @PostMapping("/status")
    public Map<String,String> status(@Valid @RequestBody StatusRequest request) {
        return Map.of("messageId", commands.requestStatus(request.stationId(), request.ocppVersion()));
    }
}
