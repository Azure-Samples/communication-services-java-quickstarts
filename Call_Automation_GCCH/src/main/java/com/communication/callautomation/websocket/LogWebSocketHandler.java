package com.communication.callautomation.websocket;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class LogWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("WebSocket connection established: " + session.getId() + 
                          " from " + session.getRemoteAddress());
        
        // Send a welcome message to confirm connection
        try {
            session.sendMessage(new TextMessage("WebSocket connection established successfully"));
        } catch (IOException e) {
            System.err.println("Error sending welcome message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("WebSocket connection closed: " + session.getId() + 
                          ", Status: " + status.getCode() + " - " + status.getReason());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Handle incoming messages from clients if needed
        System.out.println("Received message: " + message.getPayload());
    }

    public void broadcastLogMessage(String logMessage) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(logMessage));
                } catch (IOException e) {
                    System.err.println("Error sending message to WebSocket session: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket transport error for session " + session.getId() + ": " + exception.getMessage());
        exception.printStackTrace();
        sessions.remove(session);
    }
}