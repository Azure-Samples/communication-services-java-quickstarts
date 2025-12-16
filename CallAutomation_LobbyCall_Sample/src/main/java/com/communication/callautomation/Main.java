package com.communication.callautomation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main application entry point for the Lobby Call Sample.
 * Enables configuration properties from application.yml via AcsConfiguration.
 */
@SpringBootApplication
@EnableConfigurationProperties(AcsConfiguration.class)
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}