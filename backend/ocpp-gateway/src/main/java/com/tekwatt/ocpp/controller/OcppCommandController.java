package com.tekwatt.ocpp.controller;

import com.tekwatt.ocpp.dto.*;
import com.tekwatt.ocpp.service.OcppCommandService;
import com.tekwatt.ocpp.service.OcppCommandTracker;
import com.tekwatt.ocpp.service.OperatorCommandAuthorization;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ocpp/commands")
public class OcppCommandController {
    private final OcppCommandService service;
    private final OcppCommandTracker tracker;
    private final OperatorCommandAuthorization access;

    public OcppCommandController(OcppCommandService service, OcppCommandTracker tracker,
            OperatorCommandAuthorization access) {
        this.service = service;
        this.tracker = tracker;
        this.access = access;
    }

    @PostMapping("/firmware")
    Map<String,String> firmware(@RequestHeader(name=HttpHeaders.AUTHORIZATION, required=false) String authorization,
            @RequestHeader(name="X-Tekwatt-Internal-Command-Key", required=false) String serviceKey,
            @Valid @RequestBody FirmwareCommandRequest request) {
        access.requireAdministrator(authorization, request.stationId(), serviceKey);
        return Map.of("messageId", service.firmware(request));
    }

    @PostMapping("/remote-start")
    Map<String,String> remoteStart(@RequestHeader(name=HttpHeaders.AUTHORIZATION, required=false) String authorization,
            @RequestHeader(name="X-Tekwatt-Internal-Command-Key", required=false) String serviceKey,
            @Valid @RequestBody RemoteStartRequest request) {
        access.requireAdministrator(authorization, request.stationId(), serviceKey);
        return Map.of("messageId", service.remoteStart(request));
    }

    @PostMapping("/remote-stop")
    Map<String,String> remoteStop(@RequestHeader(name=HttpHeaders.AUTHORIZATION, required=false) String authorization,
            @RequestHeader(name="X-Tekwatt-Internal-Command-Key", required=false) String serviceKey,
            @Valid @RequestBody RemoteStopRequest request) {
        access.requireAdministrator(authorization, request.stationId(), serviceKey);
        return Map.of("messageId", service.remoteStop(request));
    }

    @PostMapping("/reserve-now")
    Map<String,String> reserveNow(@RequestHeader(name=HttpHeaders.AUTHORIZATION, required=false) String authorization,
            @RequestHeader(name="X-Tekwatt-Internal-Command-Key", required=false) String serviceKey,
            @Valid @RequestBody ReserveNowRequest request) {
        access.requireAdministrator(authorization, request.stationId(), serviceKey);
        return Map.of("messageId", service.reserveNow(request));
    }

    @PostMapping("/cancel-reservation")
    Map<String,String> cancelReservation(@RequestHeader(name=HttpHeaders.AUTHORIZATION, required=false) String authorization,
            @RequestHeader(name="X-Tekwatt-Internal-Command-Key", required=false) String serviceKey,
            @Valid @RequestBody CancelReservationRequest request) {
        access.requireAdministrator(authorization, request.stationId(), serviceKey);
        return Map.of("messageId", service.cancelReservation(request));
    }

    @PostMapping("/unlock-connector")
    Map<String,String> unlockConnector(@RequestHeader(name=HttpHeaders.AUTHORIZATION, required=false) String authorization,
            @RequestHeader(name="X-Tekwatt-Internal-Command-Key", required=false) String serviceKey,
            @Valid @RequestBody UnlockConnectorRequest request) {
        access.requireAdministrator(authorization, request.stationId(), serviceKey);
        return Map.of("messageId", service.unlockConnector(request));
    }

    @GetMapping("/{messageId}/result")
    OcppCommandTracker.Result result(@PathVariable String messageId) { return tracker.result(messageId); }
}
