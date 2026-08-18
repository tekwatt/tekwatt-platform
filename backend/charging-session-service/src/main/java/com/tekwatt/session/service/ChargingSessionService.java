package com.tekwatt.session.service;

import com.tekwatt.session.dto.*;
import com.tekwatt.session.client.TariffClient;
import com.tekwatt.session.entity.*;
import com.tekwatt.session.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class ChargingSessionService {
    private final ChargingSessionRepository sessions; private final MeterReadingRepository readings; private final TariffClient tariffs;
    public ChargingSessionService(ChargingSessionRepository sessions, MeterReadingRepository readings, TariffClient tariffs) { this.sessions = sessions; this.readings = readings; this.tariffs = tariffs; }
    public SessionResponse start(StartSessionRequest r) {
        if (sessions.existsByTransactionId(r.transactionId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction ID already exists");
        TariffClient.ResolvedTariff tariff = tariffs.resolve(r.tenantId(), r.chargerId());
        return map(sessions.save(new ChargingSession(r.tenantId(), r.userId(), r.chargerId(), r.connectorId(), tariff.id(), r.transactionId(), r.meterStartWh(), tariff.energyPricePerKwh(), tariff.timePricePerMinute(), tariff.sessionFee(), tariff.taxPercent(), tariff.currency())));
    }
    @Transactional(readOnly = true) public SessionResponse get(UUID id) { return map(find(id)); }
    @Transactional(readOnly = true) public List<SessionResponse> list(UUID tenantId) { return sessions.findAllByTenantIdOrderByStartedAtDesc(tenantId).stream().map(this::map).toList(); }
    public SessionResponse meterValue(UUID id, MeterValueRequest r) {
        ChargingSession s = active(id); try { s.applyMeterValue(r.meterWh()); } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); }
        readings.save(new MeterReading(id, r.meterWh(), r.recordedAt() == null ? Instant.now() : r.recordedAt())); return map(s);
    }
    public SessionResponse stop(UUID id, StopSessionRequest r) {
        ChargingSession s = active(id); SessionStatus finalStatus = r.status() == null ? SessionStatus.COMPLETED : r.status();
        if (finalStatus == SessionStatus.ACTIVE) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Final status cannot be ACTIVE");
        try { s.stop(r.meterStopWh(), finalStatus); } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); }
        readings.save(new MeterReading(id, r.meterStopWh(), Instant.now())); return map(s);
    }
    private ChargingSession active(UUID id) { ChargingSession s = find(id); if (s.getStatus() != SessionStatus.ACTIVE) throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is not active"); return s; }
    private ChargingSession find(UUID id) { return sessions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Charging session not found")); }
    private SessionResponse map(ChargingSession s) { return new SessionResponse(s.getId(), s.getTenantId(), s.getUserId(), s.getChargerId(), s.getConnectorId(), s.getTariffId(), s.getTransactionId(), s.getStatus(), s.getMeterStartWh(), s.getMeterStopWh(), s.getEnergyKwh(), s.getPricePerKwh(), s.getTimePricePerMinute(), s.getSessionFee(), s.getTaxPercent(), s.getTotalCost(), s.getCurrency(), s.getStartedAt(), s.getStoppedAt(), s.getUpdatedAt()); }
}
