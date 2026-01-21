package com.communication.callautomation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import lombok.Getter;

@ConfigurationProperties(prefix = "acs")
@Getter
public class AppConfig {
    private final String connectionString;
    private final String callbackUriHost;
    private final String acsPhoneNumber;
    private final String targetPhoneNumber;
    private final String cognitiveServiceEndpoint;
    private final String targetTeamsUserId;

    @ConstructorBinding
    AppConfig(final String connectionString,
              final String callbackUriHost,
              final String acsPhoneNumber,
              final String targetPhoneNumber,
              final String cognitiveServiceEndpoint,
              final String targetTeamsUserId) {
        this.connectionString = connectionString;
        this.callbackUriHost = callbackUriHost;
        this.acsPhoneNumber = acsPhoneNumber;
        this.targetPhoneNumber = targetPhoneNumber;
        this.cognitiveServiceEndpoint = cognitiveServiceEndpoint;
        this.targetTeamsUserId = targetTeamsUserId;
    }

    public String getCallBackUri() {
        return callbackUriHost + "/api/callback";
    }

    public String getCallBackUriForRecordingApis() {
        return callbackUriHost + "/api/recordingcallback";
    }

    public String getCallbackUriHost() {
        return this.callbackUriHost;
    }

    public String getAcsPhoneNumber() {
        return this.acsPhoneNumber;
    }

    public String getTargetPhoneNumber() {
        return this.targetPhoneNumber;
    }

    public String getCognitiveServiceEndpoint() {
        return this.cognitiveServiceEndpoint;
    }

    public String getConnectionString() {
        return this.connectionString;
    }

    public String getTargetTeamsUserId() {
        return this.targetTeamsUserId;
    }
}
