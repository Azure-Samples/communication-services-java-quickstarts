package com.communication.callautomation.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.communication.callautomation.AppConfig;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AppConfig appConfig; // Inject AppConfig to access allowedOrigins

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Allow all APIs to accept CORS requests from your frontend
        registry.addMapping("/**") // Apply to all endpoints
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allowed HTTP methods
                .allowedHeaders("*") // Allow all headers
                .maxAge(3600); // Cache the pre-flight response for 1 hour
    }

}
