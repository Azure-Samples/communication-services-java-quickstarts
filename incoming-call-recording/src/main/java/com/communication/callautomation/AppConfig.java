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
    private final String acsPhonenumber1;
    private final String acsPhonenumber2;
    private final String targetPhonenumber;
    private final String isRejectCall;
    private final String isRedirectCall;
    private final String isCancelAddParticipant;
    private final String webSocketUrl;


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
              final String acsPhonenumber1,
              final String acsPhonenumber2,
              final String targetPhonenumber,
              final String isRejectCall,
              final String isRedirectCall,
              final String webSocketUrl,
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
        this.acsPhonenumber1 = acsPhonenumber1;
        this.acsPhonenumber2 = acsPhonenumber2;
        this.targetPhonenumber = targetPhonenumber;
        this.isRejectCall=isRejectCall;
        this.isRedirectCall = isRedirectCall;
        this.isCancelAddParticipant = isCancelAddParticipant;
        this.webSocketUrl = webSocketUrl;
    }
    
    public String getCallBackUri() {
        return basecallbackuri + "/api/callback";
    }

    public String getCallBackUriForRecordingApis() {
        return basecallbackuri + "/api/recordingcallback";
    }
}
