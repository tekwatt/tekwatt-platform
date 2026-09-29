package com.tekwatt.user.service;

import com.tekwatt.user.dto.UpdateOwnProfileRequest;
import com.tekwatt.user.dto.UserResponse;
import com.tekwatt.user.entity.UserProfile;
import com.tekwatt.user.entity.UserStatus;
import com.tekwatt.user.repository.UserProfileRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentCustomerService {
    private final CurrentIdentityService identities;
    private final UserProfileRepository profiles;

    public CurrentCustomerService(CurrentIdentityService identities, UserProfileRepository profiles) {
        this.identities = identities;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public UserResponse me(String authorization) {
        return UserResponse.from(ownProfile(authorization));
    }

    @Transactional(readOnly = true)
    public UserResponse byAuthUser(String authorization, UUID authUserId) {
        UUID caller = identities.require(authorization).userId();
        if (!caller.equals(authUserId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your customer profile");
        return UserResponse.from(activeProfile(caller));
    }

    @Transactional
    public UserResponse updateMe(String authorization, UpdateOwnProfileRequest request) {
        UserProfile profile = ownProfile(authorization);
        profile.update(request.firstName().trim(), request.lastName().trim(), null,
                profile.getEmail(), request.phone(), request.city(), request.zipcode(), null);
        return UserResponse.from(profile);
    }

    private UserProfile ownProfile(String authorization) {
        return activeProfile(identities.require(authorization).userId());
    }

    private UserProfile activeProfile(UUID authUserId) {
        UserProfile profile = findByAuthUserId(authUserId);
        if (profile.getStatus() != UserStatus.ACTIVE)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customer profile is not active");
        return profile;
    }

    private UserProfile findByAuthUserId(UUID authUserId) {
        return profiles.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile not found"));
    }
}
