package com.communication.callautomation;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "acs")
@Getter
public class ConfigurationRequest {
    private String acsConnectionString;
    private String cognitiveServiceEndpoint;
    private String acsPhoneNumber;
    private String targetPhoneNumber;
    private String targetTeamsUserId;
    private String targetAcsUserId;
    private String callbackUriHost;
    private String websocketUriHost;

    // Getters and Setters
    public void setAcsConnectionString(String acsConnectionString) {
        this.acsConnectionString = acsConnectionString;
    }

    public void setCognitiveServiceEndpoint(String cognitiveServiceEndpoint) {
        this.cognitiveServiceEndpoint = cognitiveServiceEndpoint;
    }

    public void setAcsPhoneNumber(String acsPhoneNumber) {
        this.acsPhoneNumber = acsPhoneNumber;
    }

    public void setTargetAcsUserId(String targetAcsUserId) {
        this.targetAcsUserId = targetAcsUserId;
    }

    public void setTargetPhoneNumber(String targetPhoneNumber) {
        this.targetPhoneNumber = targetPhoneNumber;
    }

    public void setTargetTeamsUserId(String targetTeamsUserId) {
        this.targetTeamsUserId = targetTeamsUserId;
    }

    public void setCallbackUriHost(String callbackUriHost) {
        this.callbackUriHost = callbackUriHost;
    }
    public void setWebsocketUriHost(String websocketUriHost) {
        this.websocketUriHost = websocketUriHost;
    }
}
