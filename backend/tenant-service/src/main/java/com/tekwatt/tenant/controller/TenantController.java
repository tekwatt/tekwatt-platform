package com.tekwatt.tenant.controller;
import com.tekwatt.tenant.dto.*;
import com.tekwatt.tenant.service.TenantService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/tenants")
public class TenantController {
 private final TenantService service; public TenantController(TenantService service){this.service=service;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) TenantResponse create(@Valid @RequestBody TenantRequest r){return service.create(r);}
 @GetMapping("/{id}") TenantResponse get(@PathVariable UUID id){return service.get(id);}
 @GetMapping Page<TenantResponse> list(Pageable pageable){return service.list(pageable);}
 @PutMapping("/{id}") TenantResponse update(@PathVariable UUID id,@Valid @RequestBody TenantRequest r){return service.update(id,r);}
 @PatchMapping("/{id}/status") TenantResponse status(@PathVariable UUID id,@Valid @RequestBody TenantStatusRequest r){return service.changeStatus(id,r);}
}
