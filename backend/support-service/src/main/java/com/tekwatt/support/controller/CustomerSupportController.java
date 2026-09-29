package com.tekwatt.support.controller;

import com.tekwatt.support.dto.CustomerTicketRequest;
import com.tekwatt.support.dto.TicketDetailResponse;
import com.tekwatt.support.entity.SupportTicket;
import com.tekwatt.support.entity.TicketComment;
import com.tekwatt.support.service.CustomerSupportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/api/v1/support/tickets/my")
public class CustomerSupportController {
    private final CustomerSupportService service;

    public CustomerSupportController(CustomerSupportService service) { this.service = service; }

    @GetMapping
    public List<SupportTicket> mine(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.mine(authorization);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupportTicket create(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody CustomerTicketRequest request) {
        return service.create(authorization, request);
    }

    @GetMapping("/{id}")
    public TicketDetailResponse detail(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID id) {
        return service.detail(authorization, id);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketComment comment(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID id, @Valid @RequestBody Reply request) {
        return service.comment(authorization, id, request.body());
    }

    public record Reply(@NotBlank String body) { }
}
