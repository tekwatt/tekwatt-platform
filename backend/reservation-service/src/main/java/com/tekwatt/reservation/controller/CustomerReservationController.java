package com.tekwatt.reservation.controller;

import com.tekwatt.reservation.dto.CustomerReservationRequest;
import com.tekwatt.reservation.dto.ReservationResponse;
import com.tekwatt.reservation.service.CustomerReservationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations/my")
public class CustomerReservationController {
    private final CustomerReservationService service;

    public CustomerReservationController(CustomerReservationService service) { this.service = service; }

    @GetMapping
    public List<ReservationResponse> mine(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.mine(authorization);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody CustomerReservationRequest request) {
        return service.create(authorization, request);
    }

    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID id) {
        return service.cancel(authorization, id);
    }

    @PostMapping("/{id}/complete")
    public ReservationResponse complete(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID id) {
        return service.complete(authorization, id);
    }
}
