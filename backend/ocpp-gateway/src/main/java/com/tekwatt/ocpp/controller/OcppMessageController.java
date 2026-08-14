package com.tekwatt.ocpp.controller;

import com.tekwatt.ocpp.entity.OcppMessage;
import com.tekwatt.ocpp.repository.OcppMessageRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ocpp/messages")
public class OcppMessageController {
    private final OcppMessageRepository repository;

    public OcppMessageController(OcppMessageRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    List<OcppMessage> list(@RequestParam(required = false) String stationId) {
        return stationId == null || stationId.isBlank()
                ? repository.findTop200ByOrderByCreatedAtDesc()
                : repository.findTop200ByStationIdOrderByCreatedAtDesc(stationId);
    }
}
