package com.communication.callautomation;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class LobbyWebSocketHandler extends TextWebSocketHandler {
    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private static final Logger log = LoggerFactory.getLogger(LobbyWebSocketHandler.class);

    // Reference to ProgramSample for handling messages
    private ProgramSample programSample;

    public void setProgramSample(ProgramSample programSample) {
        this.programSample = programSample;
    }

    /**
     * Send a message to all connected WebSocket sessions
     * 
     * @param message The message to send
     */
    public void sendMessageToAll(String message) {
        log.info("Sending message to {} connected sessions: {}", sessions.size(), message);

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                    log.info("Message sent successfully to session: {}", session.getId());
                } catch (IOException e) {
                    log.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
                    // Remove invalid session
                    sessions.remove(session);
                }
            } else {
                // Remove closed sessions
                sessions.remove(session);
                log.info("Removed closed session: {}", session.getId());
            }
        }
    }

    /**
     * Check if there are any active WebSocket connections
     * 
     * @return true if there are active connections
     */
    public boolean hasActiveConnections() {
        // Clean up closed sessions first
        sessions.removeIf(session -> !session.isOpen());
        return !sessions.isEmpty();
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket connection established: {} (Total active: {})", session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        String clientMessage = message.getPayload().trim();
        log.info("Received WebSocket message from {}: {}", session.getId(), clientMessage);

        // Delegate to ProgramSample if available
        if (programSample != null) {
            programSample.handleWebSocketMessage(session, message);
        } else {
            log.warn("ProgramSample not available, cannot process message");
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session,
            @NonNull org.springframework.web.socket.CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("WebSocket connection closed: {} with status: {} (Remaining active: {})",
                session.getId(), status, sessions.size());
    }
}
