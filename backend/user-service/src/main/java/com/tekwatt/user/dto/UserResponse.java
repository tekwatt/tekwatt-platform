package com.tekwatt.user.dto;
import com.tekwatt.user.entity.UserProfile;
import com.tekwatt.user.entity.UserStatus;
import java.time.Instant;
import java.util.UUID;
public record UserResponse(UUID id, UUID authUserId, UUID tenantId, String firstName, String lastName, String fullName, String email, String phone, String city, String zipcode, UserStatus status, Instant createdAt, Instant updatedAt) {
    public static UserResponse from(UserProfile u) { return new UserResponse(u.getId(),u.getAuthUserId(),u.getTenantId(),u.getFirstName(),u.getLastName(),u.getFullName(),u.getEmail(),u.getPhone(),u.getCity(),u.getZipcode(),u.getStatus(),u.getCreatedAt(),u.getUpdatedAt()); }
}
