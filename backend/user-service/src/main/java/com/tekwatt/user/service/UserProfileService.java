package com.tekwatt.user.service;

import com.tekwatt.user.dto.*;
import com.tekwatt.user.entity.UserProfile;
import com.tekwatt.user.repository.UserProfileRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {
    private final UserProfileRepository repository;
    public UserProfileService(UserProfileRepository repository) { this.repository=repository; }
    @Transactional public UserResponse create(CreateUserRequest r) {
        if(repository.existsByAuthUserId(r.authUserId())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Authentication user already has a profile");
        if(repository.existsByEmailIgnoreCase(r.email())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email address is already in use");
        return UserResponse.from(repository.save(new UserProfile(r.authUserId(),r.tenantId(),r.firstName().trim(),r.lastName().trim(),r.fullName(),r.email().trim().toLowerCase(),r.phone(),r.city(),r.zipcode(),r.status())));
    }
    @Transactional(readOnly=true) public UserResponse get(UUID id) { return UserResponse.from(find(id)); }
    @Transactional(readOnly=true) public UserResponse getByAuthUserId(UUID authUserId) { return UserResponse.from(repository.findByAuthUserId(authUserId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"User profile not found"))); }
    @Transactional(readOnly=true) public Page<UserResponse> list(UUID tenantId, Pageable pageable) { return (tenantId == null ? repository.findAll(pageable) : repository.findByTenantId(tenantId,pageable)).map(UserResponse::from); }
    @Transactional public UserResponse update(UUID id, UpdateUserRequest r) {
        UserProfile u=find(id); String email=r.email().trim().toLowerCase();
        if(repository.existsByEmailIgnoreCaseAndIdNot(email,id)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email address is already in use");
        u.update(r.firstName().trim(),r.lastName().trim(),r.fullName(),email,r.phone(),r.city(),r.zipcode(),r.status()); return UserResponse.from(u);
    }
    @Transactional public void deactivate(UUID id) { find(id).deactivate(); }
    private UserProfile find(UUID id) { return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"User profile not found")); }
}
