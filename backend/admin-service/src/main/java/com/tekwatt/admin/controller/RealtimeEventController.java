package com.tekwatt.admin.controller;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/admin/events")
public class RealtimeEventController {
    private static final long STREAM_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(30);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2,
            Thread.ofVirtual().name("tekwatt-realtime-", 0).factory());

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter subscribe(@RequestParam UUID tenantId) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        send(emitter, "connected", tenantId);
        ScheduledFuture<?> pulse = scheduler.scheduleAtFixedRate(
                () -> send(emitter, "refresh", tenantId), 5, 5, TimeUnit.SECONDS);
        Runnable cleanup = () -> pulse.cancel(false);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> { cleanup.run(); emitter.complete(); });
        emitter.onError(error -> cleanup.run());
        return emitter;
    }

    private void send(SseEmitter emitter, String event, UUID tenantId) {
        try {
            emitter.send(SseEmitter.event()
                    .id(UUID.randomUUID().toString())
                    .name(event)
                    .reconnectTime(3_000)
                    .data(Map.of("tenantId", tenantId, "timestamp", Instant.now())));
        } catch (IOException | IllegalStateException exception) {
            emitter.completeWithError(exception);
        }
    }
}
