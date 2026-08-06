package com.tekwatt.connector.controller;

import com.tekwatt.connector.dto.*;
import com.tekwatt.connector.service.ConnectorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/connectors")
public class ConnectorController {
    private final ConnectorService service;
    public ConnectorController(ConnectorService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ConnectorResponse create(@Valid @RequestBody ConnectorRequest request) { return service.create(request); }
    @GetMapping("/{id}") public ConnectorResponse get(@PathVariable UUID id) { return service.get(id); }
    @GetMapping public List<ConnectorResponse> list(@RequestParam UUID chargerId) { return service.list(chargerId); }
    @PutMapping("/{id}") public ConnectorResponse update(@PathVariable UUID id, @Valid @RequestBody ConnectorRequest request) { return service.update(id, request); }
    @PatchMapping("/{id}/status") public ConnectorResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody ConnectorStatusRequest request) { return service.updateStatus(id, request); }
}
