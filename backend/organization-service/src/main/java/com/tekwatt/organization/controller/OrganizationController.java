package com.tekwatt.organization.controller;import com.tekwatt.organization.dto.*;import com.tekwatt.organization.service.OrganizationService;import jakarta.validation.Valid;import java.util.UUID;import org.springframework.data.domain.*;import org.springframework.http.HttpStatus;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/organizations") public class OrganizationController{
 private final OrganizationService service;public OrganizationController(OrganizationService service){this.service=service;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) OrganizationResponse create(@Valid @RequestBody OrganizationRequest r){return service.create(r);}
 @GetMapping("/{id}") OrganizationResponse get(@PathVariable UUID id){return service.get(id);}
 @GetMapping Page<OrganizationResponse> list(@RequestParam(required=false) UUID tenantId,Pageable p){return service.list(tenantId,p);}
 @PutMapping("/{id}") OrganizationResponse update(@PathVariable UUID id,@Valid @RequestBody OrganizationRequest r){return service.update(id,r);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deactivate(@PathVariable UUID id){service.deactivate(id);}
}
