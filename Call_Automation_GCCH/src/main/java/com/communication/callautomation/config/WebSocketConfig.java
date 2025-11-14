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
                
                // Set keepalive headers to prevent connection drops
                response.getHeaders().add("Connection", "keep-alive");
                response.getHeaders().add("Keep-Alive", "timeout=300, max=1000");
                
                System.out.println("WebSocket handshake from: " + request.getRemoteAddress());
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
        // Enhanced WebSocket endpoint with custom interceptor
        registry.addHandler(logWebSocketHandler, "/websocket/logs")
                .setAllowedOriginPatterns("*")
                .addInterceptors(customHandshakeInterceptor());
        
        // Fallback endpoint with minimal configuration
        registry.addHandler(logWebSocketHandler, "/ws/logs")
                .addInterceptors(customHandshakeInterceptor());
                
        // Additional endpoint for troubleshooting
        registry.addHandler(logWebSocketHandler, "/logs/ws")
                .setAllowedOriginPatterns("*");
    }

    /**
     * Configure Tomcat connector for enhanced WebSocket stability
     */
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        
        // Add connector customizations for WebSocket stability
        tomcat.addConnectorCustomizers(connector -> {
            connector.setProperty("connectionTimeout", "300000"); // 5 minutes
            connector.setProperty("keepAliveTimeout", "300000");  // 5 minutes
            connector.setProperty("maxKeepAliveRequests", "1000");
            connector.setProperty("tcpNoDelay", "true");
            
            // WebSocket specific properties
            connector.setProperty("maxHttpHeaderSize", "65536");  // 64KB for large headers
            connector.setProperty("compression", "on");
            connector.setProperty("compressionMinSize", "1024");
            connector.setProperty("noCompressionUserAgents", "gozilla, traviata");
            
            logger.info("Configured Tomcat connector with WebSocket optimizations - " +
                       "connectionTimeout: 300s, keepAliveTimeout: 300s, tcpNoDelay: true");
        });
        
        return tomcat;
    }
}