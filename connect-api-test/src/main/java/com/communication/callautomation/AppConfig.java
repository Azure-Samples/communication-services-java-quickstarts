package com.communication.callautomation;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "acs")
@Getter
public class AppConfig {
    private final String connectionString;
    private final String basecallbackuri;
    private final String callerphonenumber;
    private final String targetphonenumber;
    private final String cognitiveServiceEndpoint;
    private final String targetTeamsUserId;

    @ConstructorBinding
    AppConfig(final String connectionString,
              final String basecallbackuri,
              final String callerphonenumber,
              final String targetphonenumber,
              final String cognitiveServiceEndpoint,
              final String targetTeamsUserId) {
        this.connectionString = connectionString;
        this.basecallbackuri = basecallbackuri;
        this.callerphonenumber = callerphonenumber;
        this.targetphonenumber = targetphonenumber;
        this.cognitiveServiceEndpoint = cognitiveServiceEndpoint;
        this.targetTeamsUserId = targetTeamsUserId;
    }

    public String getCallBackUri() {
        return basecallbackuri + "/api/callback";
    }

    public String getCallBackUriForRecordingApis() {
        return basecallbackuri + "/api/recordingcallback";
    }
}
