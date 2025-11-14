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
        // Direct WebSocket endpoint - primary endpoint to avoid CORS issues
        registry.addHandler(logWebSocketHandler, "/websocket/logs")
                .setAllowedOriginPatterns("*");
        
        // Simple WebSocket endpoint without any CORS configuration for basic compatibility
        registry.addHandler(logWebSocketHandler, "/ws/logs");
    }
}