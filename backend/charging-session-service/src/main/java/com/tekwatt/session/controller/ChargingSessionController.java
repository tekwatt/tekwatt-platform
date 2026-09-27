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
    @org.springframework.beans.factory.annotation.Value("${SESSION_RECOVERY_KEY:}") private String recoveryKey;
    public ChargingSessionController(ChargingSessionService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public SessionResponse start(@Valid @RequestBody StartSessionRequest request) { return service.start(request); }
    @GetMapping("/by-transaction/{transactionId}") public SessionResponse getByTransactionId(@PathVariable String transactionId) { return service.getByTransactionId(transactionId); }
    @GetMapping("/{id}") public SessionResponse get(@PathVariable UUID id) { return service.get(id); }
    @GetMapping public List<SessionResponse> list(@RequestParam UUID tenantId) { return service.list(tenantId); }
    @PostMapping("/{id}/meter-values") public SessionResponse meterValue(@PathVariable UUID id, @Valid @RequestBody MeterValueRequest request) { return service.meterValue(id, request); }
    @PostMapping("/{id}/stop") public SessionResponse stop(@PathVariable UUID id, @Valid @RequestBody StopSessionRequest request) { return service.stop(id, request); }
    @PostMapping("/{id}/reconcile-available") public SessionResponse reconcileAvailable(@PathVariable UUID id, @RequestHeader(value="X-Session-Recovery-Key", defaultValue="") String key, @Valid @RequestBody ReconcileAvailableRequest request) {
        if (recoveryKey == null || recoveryKey.isBlank() || !java.security.MessageDigest.isEqual(recoveryKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), key.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Internal charger recovery only");
        return service.reconcileAvailable(id, request);
    }
}
