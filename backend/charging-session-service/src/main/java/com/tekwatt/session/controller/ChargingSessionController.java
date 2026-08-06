package com.tekwatt.session.controller;

import com.tekwatt.session.dto.*;
import com.tekwatt.session.service.ChargingSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/charging-sessions")
public class ChargingSessionController {
    private final ChargingSessionService service;
    public ChargingSessionController(ChargingSessionService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public SessionResponse start(@Valid @RequestBody StartSessionRequest request) { return service.start(request); }
    @GetMapping("/{id}") public SessionResponse get(@PathVariable UUID id) { return service.get(id); }
    @GetMapping public List<SessionResponse> list(@RequestParam UUID tenantId) { return service.list(tenantId); }
    @PostMapping("/{id}/meter-values") public SessionResponse meterValue(@PathVariable UUID id, @Valid @RequestBody MeterValueRequest request) { return service.meterValue(id, request); }
    @PostMapping("/{id}/stop") public SessionResponse stop(@PathVariable UUID id, @Valid @RequestBody StopSessionRequest request) { return service.stop(id, request); }
}
