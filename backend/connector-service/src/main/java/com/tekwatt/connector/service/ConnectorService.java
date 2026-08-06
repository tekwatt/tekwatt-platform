package com.tekwatt.connector.service;

import com.tekwatt.connector.dto.*;
import com.tekwatt.connector.entity.Connector;
import com.tekwatt.connector.repository.ConnectorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ConnectorService {
    private final ConnectorRepository repository;
    public ConnectorService(ConnectorRepository repository) { this.repository = repository; }
    public ConnectorResponse create(ConnectorRequest request) {
        if (repository.existsByChargerIdAndConnectorNumber(request.chargerId(), request.connectorNumber()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Connector number already exists for this charger");
        return map(repository.save(new Connector(request.tenantId(), request.chargerId(), request.connectorNumber(), request.type(), request.maxPowerKw(), request.maxVoltage(), request.maxCurrent())));
    }
    @Transactional(readOnly = true) public ConnectorResponse get(UUID id) { return map(find(id)); }
    @Transactional(readOnly = true) public List<ConnectorResponse> list(UUID chargerId) { return repository.findAllByChargerIdOrderByConnectorNumber(chargerId).stream().map(this::map).toList(); }
    public ConnectorResponse update(UUID id, ConnectorRequest request) {
        Connector connector = find(id);
        if (!connector.getTenantId().equals(request.tenantId()) || !connector.getChargerId().equals(request.chargerId()) || !connector.getConnectorNumber().equals(request.connectorNumber()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenantId, chargerId and connectorNumber cannot be changed");
        connector.update(request.type(), request.maxPowerKw(), request.maxVoltage(), request.maxCurrent()); return map(connector);
    }
    public ConnectorResponse updateStatus(UUID id, ConnectorStatusRequest request) { Connector connector = find(id); connector.changeStatus(request.status()); return map(connector); }
    private Connector find(UUID id) { return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Connector not found")); }
    private ConnectorResponse map(Connector c) { return new ConnectorResponse(c.getId(), c.getTenantId(), c.getChargerId(), c.getConnectorNumber(), c.getType(), c.getMaxPowerKw(), c.getMaxVoltage(), c.getMaxCurrent(), c.getStatus(), c.getCreatedAt(), c.getUpdatedAt()); }
}
