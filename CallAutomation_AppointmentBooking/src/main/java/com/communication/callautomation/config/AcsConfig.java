package com.communication.callautomation.config;

import com.azure.core.implementation.util.EnvironmentConfiguration;
import com.azure.core.util.Configuration;
import com.communication.callautomation.controller.ApiVersion;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.UUID;

@ConfigurationProperties(prefix = "acs")
@Getter
public class AcsConfig {
    private final String connectionString;
    private final String basecallbackuri;
    private final String timeoutMs;
    private final String mediarecordingstarted;
    private final String mediamainmenu;
    private final String mediaretry;
    private final String mediagoodbye;
    private final String mediachoice1;
    private final String mediachoice2;
    private final String mediachoice3;

    @ConstructorBinding
    AcsConfig(final String connectionString,
              final String basecallbackuri,
              final String timeoutMs,
              final String mediarecordingstarted,
              final String mediamainmenu,
              final String mediaretry,
              final String mediagoodbye,
              final String mediachoice1,
              final String mediachoice2,
              final String mediachoice3) {
        this.connectionString = connectionString;
        this.basecallbackuri = basecallbackuri;
        this.timeoutMs = timeoutMs;
        this.mediarecordingstarted = mediarecordingstarted;
        this.mediamainmenu = mediamainmenu;
        this.mediaretry = mediaretry;
        this.mediagoodbye = mediagoodbye;
        this.mediachoice1 = mediachoice1;
        this.mediachoice2 = mediachoice2;
        this.mediachoice3 = mediachoice3;
        EnvironmentConfiguration.getGlobalConfiguration().put(Configuration.PROPERTY_AZURE_REQUEST_RESPONSE_TIMEOUT, timeoutMs);
    }

    public String getCallbackUri(String callerId){
        return basecallbackuri + String.format("%s/calls/ongoing/%s?callerId=%s", ApiVersion.CURRENT, UUID.randomUUID(), callerId);
    }

    public String getMediaUri(String filename){
        return basecallbackuri + String.format("%s/calls/media/%s", ApiVersion.CURRENT, filename);
    }
}
