package com.tekwatt.ocpp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tekwatt.ocpp.service.OcppCommandTracker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OcppCommandTrackerTest {
    private final ObjectMapper json = new ObjectMapper();
    private final OcppCommandTracker tracker = new OcppCommandTracker();

    @Test
    void tracksAcceptedCallResult() throws Exception {
        tracker.register("accepted-1");
        tracker.complete("accepted-1", 3, json.readTree("[3,\"accepted-1\",{\"status\":\"Accepted\"}]"));
        assertThat(tracker.result("accepted-1").result()).isEqualTo("ACCEPTED");
    }

    @Test
    void tracksRejectedCallResult() throws Exception {
        tracker.register("rejected-1");
        tracker.complete("rejected-1", 3, json.readTree("[3,\"rejected-1\",{\"status\":\"Occupied\"}]"));
        assertThat(tracker.result("rejected-1").result()).isEqualTo("REJECTED");
        assertThat(tracker.result("rejected-1").message()).isEqualTo("Occupied");
    }

    @Test
    void tracksCallError() throws Exception {
        tracker.register("failed-1");
        tracker.complete("failed-1", 4, json.readTree("[4,\"failed-1\",\"InternalError\",\"Failure\",{}]"));
        assertThat(tracker.result("failed-1").result()).isEqualTo("FAILED");
        assertThat(tracker.result("failed-1").message()).isEqualTo("Failure");
    }

    @Test
    void recordsTransportFailure() {
        tracker.register("transport-1");
        tracker.fail("transport-1", "Could not send OCPP command");
        assertThat(tracker.result("transport-1").result()).isEqualTo("FAILED");
    }
}
