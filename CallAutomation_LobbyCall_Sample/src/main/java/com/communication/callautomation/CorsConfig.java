package com.communication.callautomation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**") // Allow all endpoints
                        // .allowedOrigins("http://localhost:8080", "https://localhost:8080", "http://8kvlj5f1.inc1.devtunnels.ms:8080", "https://8kvlj5f1.inc1.devtunnels.ms:8080") // Allow all origins
                        // .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allow all HTTP methods
                        // .allowedHeaders("*") // Allow all headers
                        // .allowCredentials(true); // Enable credentials for security
                        .allowedOrigins("*") // Allow all origins
                        .allowedMethods("*") // Allow all HTTP methods
                        .allowedHeaders("*") // Allow all headers
                        .allowCredentials(false); // Disable credentials for security
            }
        };
    }
}