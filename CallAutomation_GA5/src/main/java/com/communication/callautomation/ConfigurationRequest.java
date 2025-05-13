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
    private String callbackUriHost;
    private String websocketUriHost;

    // Getters and Setters
    public String getAcsConnectionString() {
        return acsConnectionString;
    }

    public void setAcsConnectionString(String acsConnectionString) {
        this.acsConnectionString = acsConnectionString;
    }

    public String getCognitiveServiceEndpoint() {
        return cognitiveServiceEndpoint;
    }

    public void setCognitiveServiceEndpoint(String cognitiveServiceEndpoint) {
        this.cognitiveServiceEndpoint = cognitiveServiceEndpoint;
    }

    public String getAcsPhoneNumber() {
        return acsPhoneNumber;
    }

    public void setAcsPhoneNumber(String acsPhoneNumber) {
        this.acsPhoneNumber = acsPhoneNumber;
    }

    public String getTargetPhoneNumber() {
        return targetPhoneNumber;
    }

    public void setTargetPhoneNumber(String targetPhoneNumber) {
        this.targetPhoneNumber = targetPhoneNumber;
    }

    public String getTargetTeamsUserId() {
        return targetTeamsUserId;
    }

    public void setTargetTeamsUserId(String targetTeamsUserId) {
        this.targetTeamsUserId = targetTeamsUserId;
    }

    public String getCallbackUriHost() {
        return callbackUriHost;
    }

    public void setCallbackUriHost(String callbackUriHost) {
        this.callbackUriHost = callbackUriHost;
    }

    public String getWebsocketUriHost() {
        return websocketUriHost;
    }

    public void setWebsocketUriHost(String websocketUriHost) {
        this.websocketUriHost = websocketUriHost;
    }
}
