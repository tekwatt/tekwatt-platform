package com.tekwatt.user.controller;

import com.tekwatt.user.service.CpoMobileService;
import com.tekwatt.user.service.CpoMobileService.CpoIdentity;
import com.tekwatt.user.service.CpoMobileService.CpoOverview;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/cpo/me")
public class CpoMobileController {
    private final CpoMobileService service;
    public CpoMobileController(CpoMobileService service) { this.service = service; }
    @GetMapping public CpoIdentity me(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.me(authorization);
    }
    @GetMapping("/overview") public CpoOverview overview(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.overview(authorization);
    }
}
