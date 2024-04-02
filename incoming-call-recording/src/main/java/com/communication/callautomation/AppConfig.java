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
    private final String isPauseOnStart;
    private final String isByos;
    private final String bringYourOwnStorageUrl;
    private final String teamsComplianceUserId;
    private final String acsPhonenumber;
    private final String targetPhonenumber;
    private final String isRejectCall;
    private final String isCancelAddParticipant;

    @ConstructorBinding
    AppConfig(final String connectionString,
              final String basecallbackuri,
              final String cognitiveServicesUrl,
              final String agentPhoneNumber,
              final String isPauseOnStart,
              final String isByos,
              final String bringYourOwnStorageUrl,
              final String teamsComplianceUserId,
              final String acsPhonenumber,
               final String targetPhonenumber,
                final String isRejectCall,
                final String isCancelAddParticipant) {
        this.connectionString = connectionString;
        this.basecallbackuri = basecallbackuri;
        this.cognitiveServicesUrl = cognitiveServicesUrl;
        this.agentPhoneNumber = agentPhoneNumber;
        this.isPauseOnStart = isPauseOnStart;
        this.isByos = isByos;
        this.bringYourOwnStorageUrl = bringYourOwnStorageUrl;
        this.teamsComplianceUserId = teamsComplianceUserId;
        this.acsPhonenumber = acsPhonenumber;
        this.targetPhonenumber = targetPhonenumber;
        this.isRejectCall=isRejectCall;
        this.isCancelAddParticipant = isCancelAddParticipant;
    }
    
    public String getCallBackUri() {
        return basecallbackuri + "/api/callback";
    }

    public String getCallBackUriForRecordingApis() {
        return basecallbackuri + "/api/recordingcallback";
    }
}
