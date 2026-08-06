package com.tekwatt.charger.controller;

import com.tekwatt.charger.dto.*;
import com.tekwatt.charger.service.ChargerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chargers")
public class ChargerController {
    private final ChargerService service;
    public ChargerController(ChargerService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ChargerResponse create(@Valid @RequestBody ChargerRequest request) { return service.create(request); }
    @GetMapping("/{id}") public ChargerResponse get(@PathVariable UUID id) { return service.get(id); }
    @GetMapping public List<ChargerResponse> list(@RequestParam UUID tenantId) { return service.list(tenantId); }
    @PutMapping("/{id}") public ChargerResponse update(@PathVariable UUID id, @Valid @RequestBody ChargerRequest request) { return service.update(id, request); }
    @PatchMapping("/{id}/status") public ChargerResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody ChargerStatusRequest request) { return service.updateStatus(id, request); }
    @PostMapping("/{id}/heartbeat") public ChargerResponse heartbeat(@PathVariable UUID id) { return service.heartbeat(id); }
}
