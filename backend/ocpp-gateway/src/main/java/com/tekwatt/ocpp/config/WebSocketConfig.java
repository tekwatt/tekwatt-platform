package com.tekwatt.ocpp.config;

import com.tekwatt.ocpp.websocket.OcppHandshakeInterceptor;
import com.tekwatt.ocpp.websocket.OcppWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final OcppWebSocketHandler handler;
    private final OcppHandshakeInterceptor interceptor;

    public WebSocketConfig(OcppWebSocketHandler handler, OcppHandshakeInterceptor interceptor) {
        this.handler = handler;
        this.interceptor = interceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ocpp", "/ocpp/{stationId}")
                .addInterceptors(interceptor)
                .setAllowedOrigins("*");
    }
}
