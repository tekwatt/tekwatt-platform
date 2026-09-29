package com.tekwatt.user;

import com.tekwatt.user.dto.UpdateOwnProfileRequest;
import com.tekwatt.user.entity.UserProfile;
import com.tekwatt.user.repository.UserProfileRepository;
import com.tekwatt.user.service.CurrentCustomerService;
import com.tekwatt.user.service.CurrentIdentityService;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CurrentCustomerServiceTest {
    private static final String AUTHORIZATION = "Bearer valid-session";
    private final UUID userId = UUID.randomUUID();
    private final CurrentIdentityService identities = mock(CurrentIdentityService.class);
    private final UserProfileRepository profiles = mock(UserProfileRepository.class);
    private final CurrentCustomerService service = new CurrentCustomerService(identities, profiles);

    @Test
    void rejectsOtherCustomersProfileWithoutReadingIt() {
        when(identities.require(AUTHORIZATION)).thenReturn(new CurrentIdentityService.Identity(userId, "one@example.com", "DRIVER"));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.byAuthUser(AUTHORIZATION, UUID.randomUUID()));
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(profiles);
    }

    @Test
    void selfEditCannotChangeLoginEmailStatusTenantOrChargerAssignments() {
        UUID chargerId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UserProfile profile = new UserProfile(userId, tenantId, "Old", "Name", null,
                "one@example.com", null, null, null, "ACTIVE");
        profile.assignChargers(Set.of(chargerId));
        when(identities.require(AUTHORIZATION)).thenReturn(new CurrentIdentityService.Identity(userId, "one@example.com", "DRIVER"));
        when(profiles.findByAuthUserId(userId)).thenReturn(Optional.of(profile));

        var updated = service.updateMe(AUTHORIZATION,
                new UpdateOwnProfileRequest("New", "Name", "9876543210", "Chennai", "600042"));

        assertThat(updated.firstName()).isEqualTo("New");
        assertThat(updated.email()).isEqualTo("one@example.com");
        assertThat(updated.status().name()).isEqualTo("ACTIVE");
        assertThat(updated.tenantId()).isEqualTo(tenantId);
        assertThat(updated.assignedChargerIds()).containsExactly(chargerId);
    }

    @Test
    void inactiveProfileCannotUseSelfService() {
        UserProfile profile = new UserProfile(userId, UUID.randomUUID(), "Old", "Name", null,
                "one@example.com", null, null, null, "INACTIVE");
        when(identities.require(AUTHORIZATION)).thenReturn(new CurrentIdentityService.Identity(userId, "one@example.com", "DRIVER"));
        when(profiles.findByAuthUserId(userId)).thenReturn(Optional.of(profile));
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.me(AUTHORIZATION));
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
