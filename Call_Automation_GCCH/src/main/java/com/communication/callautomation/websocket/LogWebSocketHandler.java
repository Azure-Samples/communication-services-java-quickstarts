package com.communication.callautomation.websocket;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;

import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class LogWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("WebSocket connection established: " + session.getId() + 
                          " from " + session.getRemoteAddress());
        System.out.println("Session URI: " + session.getUri());
        System.out.println("Session attributes: " + session.getAttributes());
        System.out.println("Total active sessions: " + sessions.size());
        
        // Send welcome message with timestamp
        try {
            String welcomeMessage = String.format("WebSocket connection established at %s", 
                                                new java.util.Date());
            session.sendMessage(new TextMessage(welcomeMessage));
        } catch (IOException e) {
            System.err.println("Error sending welcome message: " + e.getMessage());
            sessions.remove(session);
        }
        
        // Start heartbeat for this session
        startHeartbeat(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("WebSocket connection closed: " + session.getId() + 
                          ", Status: " + status.getCode() + " - " + status.getReason());
        System.out.println("Remaining active sessions: " + sessions.size());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("Received message from " + session.getId() + ": " + payload);
        
        // Handle ping/pong for heartbeat
        if ("PING".equals(payload) || "HEARTBEAT".equals(payload)) {
            try {
                session.sendMessage(new TextMessage("PONG"));
            } catch (IOException e) {
                System.err.println("Error sending PONG: " + e.getMessage());
            }
        }
    }
    
    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        System.out.println("Received pong from session: " + session.getId());
    }

    public void broadcastLogMessage(String logMessage) {
        Set<WebSocketSession> sessionsToRemove = new CopyOnWriteArraySet<>();
        
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(logMessage));
                } catch (IOException e) {
                    System.err.println("Error sending message to WebSocket session " + session.getId() + ": " + e.getMessage());
                    sessionsToRemove.add(session);
                }
            } else {
                sessionsToRemove.add(session);
            }
        }
        
        // Clean up closed sessions
        sessions.removeAll(sessionsToRemove);
    }
    
    private void startHeartbeat(WebSocketSession session) {
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (session.isOpen()) {
                try {
                    // Use text message ping for better load balancer compatibility
                    session.sendMessage(new TextMessage("HEARTBEAT"));
                    System.out.println("Sent heartbeat to session: " + session.getId());
                } catch (IOException e) {
                    System.err.println("Error sending ping to session " + session.getId() + ": " + e.getMessage());
                    sessions.remove(session);
                }
            }
        }, 30, 30, TimeUnit.SECONDS); // Send ping every 30 seconds
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String errorMessage = exception.getMessage();
        String sessionId = session.getId();
        
        System.err.println("=== WebSocket Transport Error ===");
        System.err.println("Session ID: " + sessionId);
        System.err.println("Error: " + errorMessage);
        System.err.println("Session state: " + (session.isOpen() ? "OPEN" : "CLOSED"));
        System.err.println("Remote address: " + session.getRemoteAddress());
        System.err.println("Session attributes: " + session.getAttributes());
        
        // Handle specific error types
        if (errorMessage != null && errorMessage.contains("connection was aborted by the software")) {
            System.err.println("DIAGNOSIS: Network-level connection abort detected");
            System.err.println("CAUSE: Usually caused by:");
            System.err.println("  - Client browser closing/refreshing page");
            System.err.println("  - Network interruption or proxy timeout");
            System.err.println("  - Firewall or antivirus interference");
            System.err.println("  - Load balancer timeout in Azure");
            System.err.println("SOLUTION: Client should automatically reconnect");
        } else if (errorMessage != null && errorMessage.contains("Connection reset")) {
            System.err.println("DIAGNOSIS: Connection reset by peer");
            System.err.println("CAUSE: Likely network infrastructure issue");
        } else {
            System.err.println("DIAGNOSIS: Unknown transport error");
            exception.printStackTrace();
        }
        
        // Clean up the session
        sessions.remove(session);
        System.err.println("Session removed. Active sessions: " + sessions.size());
        System.err.println("=== End Transport Error ===");
    }
    
    public void shutdown() {
        heartbeatExecutor.shutdown();
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.close(CloseStatus.GOING_AWAY);
                } catch (IOException e) {
                    System.err.println("Error closing session during shutdown: " + e.getMessage());
                }
            }
        }
        sessions.clear();
    }
}