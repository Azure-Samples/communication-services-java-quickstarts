package com.communication.callautomation;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class LobbyWebSocketHandler extends TextWebSocketHandler {
    private final List<WebSocketSession> sessions = new ArrayList<>();
    private static final Logger log = LoggerFactory.getLogger(LobbyWebSocketHandler.class);
    
    // Reference to ProgramSample for handling messages
    private ProgramSample programSample;
    
    public void setProgramSample(ProgramSample programSample) {
        this.programSample = programSample;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket connection established: " + session.getId());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        log.info("Received WebSocket message: " + message.getPayload());
        
        // Delegate to ProgramSample if available
        if (programSample != null) {
            programSample.handleWebSocketMessage(session, message);
        } else {
            log.warn("ProgramSample not available, cannot process message");
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        log.error("WebSocket transport error: " + exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull org.springframework.web.socket.CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("WebSocket connection closed: " + session.getId());
    }
}
