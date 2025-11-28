package com.communication.callautomation;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "acs")
@Getter
public class ConfigurationRequest {
    private String acsConnectionString;
    private String cognitiveServiceEndpoint;
    private String callbackUriHost;
    private String acsGeneratedIdForTargetCallSender;
    private String acsPMAEndpoint;

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

    public void setAcsGeneratedIdForTargetCallSender(String acsGeneratedIdForTargetCallSender) {
        this.acsGeneratedIdForTargetCallSender = acsGeneratedIdForTargetCallSender;
    }

    public void setAcsPMAEndpoint(String acsPMAEndpoint) {
        this.acsPMAEndpoint = acsPMAEndpoint;
    }
}
