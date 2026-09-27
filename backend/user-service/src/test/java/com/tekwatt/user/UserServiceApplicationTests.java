package com.tekwatt.user;

import com.tekwatt.user.entity.UserProfile;
import com.tekwatt.user.repository.UserProfileRepository;
import jakarta.persistence.EntityManager;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserServiceApplicationTests {
    @Autowired UserProfileRepository users;
    @Autowired EntityManager entityManager;

    @Test void contextLoads() { }

    @Test @Transactional void persistsAssignedChargers() {
        UUID chargerId = UUID.randomUUID();
        UserProfile customer = new UserProfile(UUID.randomUUID(), UUID.randomUUID(), "Test", "Driver",
                "Test Driver", "test-" + UUID.randomUUID() + "@example.com", null, null, null, "ACTIVE");
        customer.assignChargers(Set.of(chargerId));
        UUID id = users.saveAndFlush(customer).getId();
        entityManager.clear();
        assertThat(users.findById(id).orElseThrow().getAssignedChargerIds()).containsExactly(chargerId);
    }
}
