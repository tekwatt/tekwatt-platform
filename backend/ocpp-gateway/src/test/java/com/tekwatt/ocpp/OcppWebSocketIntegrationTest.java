package com.tekwatt.ocpp;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "tekwatt.ocpp.shared-key=test-shared-key")
class OcppWebSocketIntegrationTest {
    @LocalServerPort
    private int port;

    @Test
    void negotiatesOcpp201AndKeepsTheConnectionOpen() throws Exception {
        assertStableConnection("TEST-STATION-201", "ocpp2.0.1", "TEST-STATION-201");
    }

    @Test
    void acceptsTheSimulatorProtocolAndEmptyBasicAuthUsername() throws Exception {
        assertStableConnection("TEST-STATION-SIMULATOR", "ocpp2.0", "");
    }

    private void assertStableConnection(String stationId, String requestedProtocol, String basicAuthUsername)
            throws Exception {
        CountDownLatch opened = new CountDownLatch(1);
        CountDownLatch closed = new CountDownLatch(1);
        AtomicReference<Integer> closeCode = new AtomicReference<>();
        String credentials = Base64.getEncoder().encodeToString(
                (basicAuthUsername + ":test-shared-key").getBytes(StandardCharsets.UTF_8));

        WebSocket socket = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build()
                .newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .header("Authorization", "Basic " + credentials)
                .subprotocols(requestedProtocol)
                .buildAsync(URI.create("ws://localhost:" + port + "/ocpp/" + stationId),
                        new WebSocket.Listener() {
                            @Override
                            public void onOpen(WebSocket webSocket) {
                                opened.countDown();
                                webSocket.request(1);
                            }

                            @Override
                            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                                closeCode.set(statusCode);
                                closed.countDown();
                                return null;
                            }
                        })
                .get(10, TimeUnit.SECONDS);

        assertThat(opened.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(socket.getSubprotocol()).isEqualTo(requestedProtocol);
        assertThat(closed.await(750, TimeUnit.MILLISECONDS))
                .as("the CSMS must not immediately close a valid OCPP connection; close code=%s", closeCode.get())
                .isFalse();

        socket.sendClose(WebSocket.NORMAL_CLOSURE, "test complete").get(5, TimeUnit.SECONDS);
    }
}
