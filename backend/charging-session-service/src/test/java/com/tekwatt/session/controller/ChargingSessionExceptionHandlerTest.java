package com.tekwatt.session.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tekwatt.session.service.ChargingSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

class ChargingSessionExceptionHandlerTest {
    @Test
    void returnsResponseStatusReasonAsProblemDetail() throws Exception {
        ChargingSessionService service = mock(ChargingSessionService.class);
        when(service.start(any())).thenThrow(new ResponseStatusException(
                HttpStatus.CONFLICT, "Assign an active tariff to this charger before starting a session"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ChargingSessionController(service))
                .setControllerAdvice(new ChargingSessionExceptionHandler())
                .build();

        mvc.perform(post("/api/v1/charging-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId":"c5043ec4-ebea-4236-8260-c6400e4ef923",
                                  "userId":"bda5a975-664f-49a3-92e3-4ad1400b3137",
                                  "chargerId":"576c7988-24ca-44b4-9555-78ff41b126f2",
                                  "connectorId":"8540cf7b-d8a5-4a35-934b-2960064397fe",
                                  "transactionId":"TX-100",
                                  "meterStartWh":100
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Assign an active tariff to this charger before starting a session"));
    }
}
