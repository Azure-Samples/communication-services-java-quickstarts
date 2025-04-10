package com.communication.callautomation;

import java.util.Map;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "acs")
@Getter
public class ConfigurationRequest {
    private String acsConnectionString;
    private String cognitiveServiceEndpoint;
    private String acsPhoneNumber;
    private String callbackUriHost;
    private Map<String, String> botRouting;
    private String defaultBotId;

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

    public String getCallbackUriHost() {
        return callbackUriHost;
    }

    public void setCallbackUriHost(String callbackUriHost) {
        this.callbackUriHost = callbackUriHost;
    }

    public Map<String, String> getBotRouting() {
        return botRouting;
    }

    public void setBotRouting(Map<String, String> botRouting) {
        this.botRouting = botRouting;
    }

    public String getDefaultBotId() {
        return defaultBotId;
    }

    public void setDefaultBotId(String defaultBotId) {
        this.defaultBotId = defaultBotId;
    }
}
