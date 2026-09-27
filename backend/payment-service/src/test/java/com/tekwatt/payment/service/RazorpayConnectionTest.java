package com.tekwatt.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RazorpayConnectionTest {
    @Test
    void testModeCheckOnlyReadsOrders() throws Exception {
        HttpClient http = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        doReturn(response).when(http).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));

        var check = new RazorpayClient(new ObjectMapper(), http).testConnection("rzp_test_sample", "secret");

        assertThat(check.success()).isTrue();
        var request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertThat(request.getValue().method()).isEqualTo("GET");
        assertThat(request.getValue().uri().toString()).isEqualTo("https://api.razorpay.com/v1/orders?count=1");
        assertThat(request.getValue().bodyPublisher()).isEmpty();
    }

    @Test
    void liveKeysAreNeverSentByTheTestAction() {
        HttpClient http = mock(HttpClient.class);
        var check = new RazorpayClient(new ObjectMapper(), http).testConnection("rzp_live_sample", "secret");
        assertThat(check.success()).isFalse();
        verifyNoInteractions(http);
    }

    @Test
    void invalidTestCredentialsGetAUsefulMessage() throws Exception {
        HttpClient http = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(401);
        doReturn(response).when(http).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        var check = new RazorpayClient(new ObjectMapper(), http).testConnection("rzp_test_sample", "wrong");
        assertThat(check.success()).isFalse();
        assertThat(check.message()).contains("rejected");
    }
}
