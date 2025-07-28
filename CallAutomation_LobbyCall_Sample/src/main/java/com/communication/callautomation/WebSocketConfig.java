package com.communication.callautomation;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private LobbyWebSocketHandler lobbyWebSocketHandler;
    
    private WebSocketHandlerRegistry currentRegistry;
    private String currentSocketToken = "default";

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        this.currentRegistry = registry;
        // Initial registration with default token
        registry.addHandler(lobbyWebSocketHandler, "/ws/" + currentSocketToken).setAllowedOrigins("*");
    }
    
    public void updateWebSocketEndpoint(String newToken) {
        if (newToken != null && !newToken.isEmpty() && !newToken.equals(currentSocketToken)) {
            this.currentSocketToken = newToken;
            // Re-register with new token
            if (currentRegistry != null) {
                currentRegistry.addHandler(lobbyWebSocketHandler, "/ws/" + newToken).setAllowedOrigins("*");
            }
        }
    }
    
    public String getCurrentSocketToken() {
        return currentSocketToken;
    }
}