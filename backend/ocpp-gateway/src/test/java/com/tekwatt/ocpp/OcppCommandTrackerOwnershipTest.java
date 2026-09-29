package com.tekwatt.ocpp;

import com.tekwatt.ocpp.service.OcppCommandTracker;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OcppCommandTrackerOwnershipTest {
    @Test
    void onlyCustomerWhoIssuedCommandCanReadItsResult() {
        var tracker = new OcppCommandTracker();
        UUID owner = UUID.randomUUID();
        tracker.register("message-1");
        tracker.claimForCustomer("message-1", owner);
        assertThat(tracker.resultForCustomer("message-1", owner).result()).isEqualTo("PENDING");
        assertThatThrownBy(() -> tracker.resultForCustomer("message-1", UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        assertThatThrownBy(() -> tracker.resultForCustomer("unknown", owner))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
    }
}
