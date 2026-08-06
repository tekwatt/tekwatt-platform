package com.tekwatt.user.controller;
import com.tekwatt.user.dto.*;
import com.tekwatt.user.service.UserProfileService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/users")
public class UserController {
    private final UserProfileService service;
    public UserController(UserProfileService service){this.service=service;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) UserResponse create(@Valid @RequestBody CreateUserRequest request){return service.create(request);}
    @GetMapping("/{id}") UserResponse get(@PathVariable UUID id){return service.get(id);}
    @GetMapping("/by-auth-user/{authUserId}") UserResponse byAuthUser(@PathVariable UUID authUserId){return service.getByAuthUserId(authUserId);}
    @GetMapping Page<UserResponse> list(@RequestParam(required=false) UUID tenantId, Pageable pageable){return service.list(tenantId,pageable);}
    @PutMapping("/{id}") UserResponse update(@PathVariable UUID id,@Valid @RequestBody UpdateUserRequest request){return service.update(id,request);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deactivate(@PathVariable UUID id){service.deactivate(id);}
}
