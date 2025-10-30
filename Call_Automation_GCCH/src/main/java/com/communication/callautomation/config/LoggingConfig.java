package com.communication.callautomation.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import com.communication.callautomation.logging.WebSocketLogAppender;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class LoggingConfig {

    @Autowired
    private WebSocketLogAppender webSocketLogAppender;

    @PostConstruct
    public void configureLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        // Create encoder
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();
        
        // Set encoder to appender
        webSocketLogAppender.setEncoder(encoder);
        webSocketLogAppender.setContext(context);
        webSocketLogAppender.start();
        
        // Get root logger and add our appender
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(webSocketLogAppender);
        rootLogger.setLevel(Level.INFO);
        
        // Also add to our specific package logger
        Logger packageLogger = context.getLogger("com.communication.callautomation");
        packageLogger.addAppender(webSocketLogAppender);
        packageLogger.setLevel(Level.INFO);
    }
}