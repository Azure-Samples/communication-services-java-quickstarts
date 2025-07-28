package com.communication.callautomation;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "acs")
@Getter
public class ConfigurationRequest {
    private String acsConnectionString;
    private String cognitiveServiceEndpoint;
    private String callbackUriHost;
    private String pmaEndpoint;
    private String acsGeneratedId;
    private String webSocketToken;

    // Getters and Setters
    public void setAcsConnectionString(String acsConnectionString) {
        this.acsConnectionString = acsConnectionString;
    }

    public void setCognitiveServiceEndpoint(String cognitiveServiceEndpoint) {
        this.cognitiveServiceEndpoint = cognitiveServiceEndpoint;
    }

    public void setCallbackUriHost(String callbackUriHost) {
        this.callbackUriHost = callbackUriHost;
    }

    public void setPmaEndpoint(String pmaEndpoint) {
        this.pmaEndpoint = pmaEndpoint;
    }

    public void setAcsGeneratedId(String acsGeneratedId) {
        this.acsGeneratedId = acsGeneratedId;
    }

    public void setWebSocketToken(String webSocketToken) {
        this.webSocketToken = webSocketToken;
    }
}
