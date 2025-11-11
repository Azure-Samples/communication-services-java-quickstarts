package com.communication.callautomation.config;

import com.communication.callautomation.websocket.LogWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private LogWebSocketHandler logWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(logWebSocketHandler, "/ws/logs")
                .setAllowedOriginPatterns("*") // Allow all origins for testing (compatible with credentials)
                .setAllowedOrigins("*"); // Also add this for broader compatibility
        
        // Also register without SockJS for direct WebSocket connections
        registry.addHandler(logWebSocketHandler, "/websocket/logs")
                .setAllowedOriginPatterns("*")
                .setAllowedOrigins("*");
    }
}