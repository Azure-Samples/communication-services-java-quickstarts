package com.communication.callautomation.logging;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.communication.callautomation.websocket.LogWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class WebSocketLogAppender extends AppenderBase<ILoggingEvent> {

    private static LogWebSocketHandler logWebSocketHandler;
    private PatternLayoutEncoder encoder;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Autowired
    public void setLogWebSocketHandler(LogWebSocketHandler handler) {
        WebSocketLogAppender.logWebSocketHandler = handler;
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (logWebSocketHandler != null) {
            try {
                String formattedMessage = formatLogMessage(event);
                logWebSocketHandler.broadcastLogMessage(formattedMessage);
            } catch (Exception e) {
                System.err.println("Error broadcasting log message: " + e.getMessage());
            }
        }
    }

    private String formatLogMessage(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(LocalDateTime.now().format(formatter)).append("] ");
        sb.append("[").append(event.getLevel()).append("] ");
        sb.append("[").append(event.getLoggerName()).append("] ");
        sb.append(event.getFormattedMessage());
        
        if (event.getThrowableProxy() != null) {
            sb.append(" - ").append(event.getThrowableProxy().getMessage());
        }
        
        return sb.toString();
    }

    @Override
    public void start() {
        if (this.encoder == null) {
            addError("No encoder set for the appender named [" + name + "].");
            return;
        }
        
        this.encoder.start();
        super.start();
    }

    @Override
    public void stop() {
        if (this.encoder != null) {
            this.encoder.stop();
        }
        super.stop();
    }

    public void setEncoder(PatternLayoutEncoder encoder) {
        this.encoder = encoder;
    }
}