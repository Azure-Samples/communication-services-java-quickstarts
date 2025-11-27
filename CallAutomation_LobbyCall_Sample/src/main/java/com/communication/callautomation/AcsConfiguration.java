package com.communication.callautomation;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Azure Communication Services
 * Maps to the 'acs' prefix in application.yml
 */
@ConfigurationProperties(prefix = "acs")
@Component
@Getter
@Setter
public class AcsConfiguration {
    private String acsConnectionString;
    private String cognitiveServiceEndpoint;
    private String callbackUriHost;
    private String acsGeneratedId;
    
    /**
     * Validates that all required configuration properties are set
     * @throws IllegalArgumentException if any required property is missing
     */
    public void validate() {
        if (isEmpty(acsConnectionString)) {
            throw new IllegalArgumentException("ACS Connection String is required");
        }
        if (isEmpty(cognitiveServiceEndpoint)) {
            throw new IllegalArgumentException("Cognitive Service Endpoint is required");
        }
        if (isEmpty(callbackUriHost)) {
            throw new IllegalArgumentException("Callback URI Host is required");
        }
        if (isEmpty(acsGeneratedId)) {
            throw new IllegalArgumentException("ACS Generated ID is required");
        }
    }
    
    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
