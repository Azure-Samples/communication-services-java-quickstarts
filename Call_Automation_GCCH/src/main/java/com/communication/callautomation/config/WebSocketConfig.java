package com.communication.callautomation.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.communication.callautomation.websocket.LogWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private LogWebSocketHandler logWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Primary WebSocket endpoint with SockJS fallback
        registry.addHandler(logWebSocketHandler, "/ws/logs")
                .setAllowedOrigins("*")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setSessionCookieNeeded(false)
                .setHeartbeatTime(25000); // 25 second heartbeat
        
        // Direct WebSocket endpoint without SockJS
        registry.addHandler(logWebSocketHandler, "/websocket/logs")
                .setAllowedOrigins("*")
                .setAllowedOriginPatterns("*");
    }
}