package com.communication.callautomation.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import com.communication.callautomation.websocket.LogWebSocketHandler;

import java.util.Map;
import java.util.logging.Logger;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final Logger logger = Logger.getLogger(WebSocketConfig.class.getName());

    @Autowired
    private LogWebSocketHandler logWebSocketHandler;

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
    
    @Bean
    public HandshakeInterceptor customHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                         WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                // Add connection metadata for debugging
                attributes.put("connectionTime", System.currentTimeMillis());
                attributes.put("remoteAddress", request.getRemoteAddress());
                attributes.put("userAgent", request.getHeaders().getFirst("User-Agent"));
                attributes.put("origin", request.getHeaders().getFirst("Origin"));
                attributes.put("host", request.getHeaders().getFirst("Host"));
                
                // Enhanced headers for deployed environments
                response.getHeaders().add("Connection", "keep-alive");
                response.getHeaders().add("Keep-Alive", "timeout=600, max=1000");
                
                // CORS headers for WebSocket handshake (no credentials to avoid conflicts)
                String origin = request.getHeaders().getFirst("Origin");
                if (origin != null) {
                    response.getHeaders().add("Access-Control-Allow-Origin", origin);
                } else {
                    response.getHeaders().add("Access-Control-Allow-Origin", "*");
                }
                response.getHeaders().add("Access-Control-Allow-Credentials", "false");
                response.getHeaders().add("Access-Control-Allow-Headers", "*");
                response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                
                // Add headers to help with load balancer compatibility
                response.getHeaders().add("X-Content-Type-Options", "nosniff");
                response.getHeaders().add("Cache-Control", "no-cache, no-store, must-revalidate");
                
                System.out.println("WebSocket handshake from: " + request.getRemoteAddress() + 
                                 ", Origin: " + origin + ", Host: " + request.getHeaders().getFirst("Host"));
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                     WebSocketHandler wsHandler, Exception exception) {
                if (exception != null) {
                    System.err.println("WebSocket handshake failed: " + exception.getMessage());
                } else {
                    System.out.println("WebSocket handshake completed successfully");
                }
            }
        };
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Primary WebSocket endpoint with enhanced CORS and interceptor
        registry.addHandler(logWebSocketHandler, "/websocket/logs")
                .setAllowedOrigins("*") // Allow all origins for deployed environment
                .setAllowedOriginPatterns("*") // Backup pattern matching
                .addInterceptors(customHandshakeInterceptor())
                .withSockJS() // Enable SockJS fallback for better compatibility
                    .setHeartbeatTime(25000) // Heartbeat every 25 seconds
                    .setDisconnectDelay(30000) // Wait 30 seconds before disconnect
                    .setHttpMessageCacheSize(1000)
                    .setStreamBytesLimit(128 * 1024); // 128KB stream limit
        
        // Direct WebSocket endpoint without SockJS (for native WebSocket clients)
        registry.addHandler(logWebSocketHandler, "/ws/logs")
                .setAllowedOrigins("*")
                .setAllowedOriginPatterns("*")
                .addInterceptors(customHandshakeInterceptor());
                
        // Additional troubleshooting endpoint
        registry.addHandler(logWebSocketHandler, "/logs/ws")
                .setAllowedOrigins("*")
                .setAllowedOriginPatterns("*")
                .addInterceptors(customHandshakeInterceptor());
                
        logger.info("Registered WebSocket handlers with enhanced CORS and SockJS support");
    }

    /**
     * Configure Tomcat connector for enhanced WebSocket stability in deployed environments
     */
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        
        // Add connector customizations for WebSocket stability
        tomcat.addConnectorCustomizers(connector -> {
            // Connection timeouts - increased for deployed environments
            connector.setProperty("connectionTimeout", "600000"); // 10 minutes
            connector.setProperty("keepAliveTimeout", "600000");  // 10 minutes
            connector.setProperty("maxKeepAliveRequests", "1000");
            connector.setProperty("tcpNoDelay", "true");
            
            // WebSocket specific properties
            connector.setProperty("maxHttpHeaderSize", "65536");  // 64KB for large headers
            connector.setProperty("compression", "on");
            connector.setProperty("compressionMinSize", "1024");
            connector.setProperty("noCompressionUserAgents", "gozilla, traviata");
            
            // Additional properties for load balancer compatibility
            connector.setProperty("socket.soReuseAddress", "true");
            connector.setProperty("socket.soKeepAlive", "true");
            connector.setProperty("socket.soTimeout", "600000"); // 10 minutes socket timeout
            
            // Protocol specific settings
            connector.setProperty("maxThreads", "200");
            connector.setProperty("minSpareThreads", "10");
            connector.setProperty("acceptCount", "100");
            
            logger.info("Configured Tomcat connector with enhanced WebSocket optimizations - " +
                       "connectionTimeout: 600s, keepAliveTimeout: 600s, tcpNoDelay: true, soKeepAlive: true");
        });
        
        return tomcat;
    }
}