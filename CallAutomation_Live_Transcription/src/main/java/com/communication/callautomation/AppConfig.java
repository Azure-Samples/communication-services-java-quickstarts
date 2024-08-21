package com.communication.callautomation;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "acs")
@Getter
public class AppConfig {
    private final String connectionString;
    private final String basecallbackuri;
    private final String cognitiveServicesUrl;
    private final String agentPhoneNumber;
    private final String acsPhoneNumber;
    private final String transportUrl;
    private final String locale;

    @ConstructorBinding
    AppConfig(final String connectionString,
              final String basecallbackuri,
              final String cognitiveServicesUrl,
              final String agentPhoneNumber,
              final String acsPhoneNumber,
              final String transportUrl,
              final String locale) {
        this.connectionString = connectionString;
        this.basecallbackuri = basecallbackuri;
        this.cognitiveServicesUrl = cognitiveServicesUrl;
        this.agentPhoneNumber = agentPhoneNumber;
        this.acsPhoneNumber = acsPhoneNumber;
        this.transportUrl = transportUrl;
        this.locale = locale;
    }

    public String getCallBackUri() {
        return basecallbackuri + "/api/callback";
    }

    public String getCallBackUriForRecordingApis() {
        return basecallbackuri + "/api/recordingcallback";
    }
}
