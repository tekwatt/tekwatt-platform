package com.tekwatt.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.tekwatt.admin.entity.RolePolicy;
import com.tekwatt.admin.service.GovernanceService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AdminServiceApplicationTests {
    @Autowired private GovernanceService governanceService;

    @Test
    void contextLoads() {}

    @Test
    void createsPredefinedPermissionsForEverySystemRole() {
        List<RolePolicy> roles = governanceService.roles(UUID.randomUUID());

        assertThat(roles).extracting(RolePolicy::getRoleName)
            .containsExactly("ADMIN", "CUSTOMER", "PARTNER", "TECHNICIAN");
        assertThat(roles).allSatisfy(role -> {
            assertThat(role.isSystemRole()).isTrue();
            assertThat(role.getPermissionList()).isNotEmpty();
        });
        assertThat(roles.stream().filter(role -> role.getRoleName().equals("ADMIN")).findFirst().orElseThrow().getPermissionList())
            .contains("Roles", "Administrators", "API Keys", "Install Modules", "General Settings", "Account Settings");
    }
}
