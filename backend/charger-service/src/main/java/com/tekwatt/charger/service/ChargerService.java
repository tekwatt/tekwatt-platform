package com.tekwatt.charger.service;

import com.tekwatt.charger.dto.*;
import com.tekwatt.charger.entity.Charger;
import com.tekwatt.charger.repository.ChargerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ChargerService {
    private final ChargerRepository repository;
    public ChargerService(ChargerRepository repository) { this.repository = repository; }

    public ChargerResponse create(ChargerRequest request) {
        if (repository.existsByStationId(request.stationId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Station ID already exists");
        return map(repository.save(new Charger(request.tenantId(), request.organizationId(), request.stationId(), request.serialNumber(), request.vendor(), request.model(), request.protocolVersion())));
    }
    @Transactional(readOnly = true) public ChargerResponse get(UUID id) { return map(find(id)); }
    @Transactional(readOnly = true) public List<ChargerResponse> list(UUID tenantId) { return repository.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::map).toList(); }
    public ChargerResponse update(UUID id, ChargerRequest request) {
        Charger charger = find(id);
        if (!charger.getTenantId().equals(request.tenantId()) || !charger.getStationId().equals(request.stationId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenantId and stationId cannot be changed");
        charger.update(request.organizationId(), request.serialNumber(), request.vendor(), request.model(), request.protocolVersion());
        return map(charger);
    }
    public ChargerResponse updateStatus(UUID id, ChargerStatusRequest request) { Charger c = find(id); c.changeStatus(request.status()); return map(c); }
    public ChargerResponse heartbeat(UUID id) { Charger c = find(id); c.recordHeartbeat(); return map(c); }
    private Charger find(UUID id) { return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Charger not found")); }
    private ChargerResponse map(Charger c) { return new ChargerResponse(c.getId(), c.getTenantId(), c.getOrganizationId(), c.getStationId(), c.getSerialNumber(), c.getVendor(), c.getModel(), c.getProtocolVersion(), c.getStatus(), c.getLastHeartbeat(), c.getCreatedAt(), c.getUpdatedAt()); }
}
