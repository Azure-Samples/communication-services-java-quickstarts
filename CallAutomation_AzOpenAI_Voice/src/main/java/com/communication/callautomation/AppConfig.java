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
    private final String azureOpenAiServiceKey;
    private final String azureOpenAiServiceEndpoint;
    private final String openAiModelName;
    private final String agentPhoneNumber;

    @ConstructorBinding
    AppConfig(final String connectionString,
              final String basecallbackuri,
              final String cognitiveServicesUrl,
              final String azureOpenAiServiceKey,
              final String azureOpenAiServiceEndpoint,
              final String openAiModelName,
              final String agentPhoneNumber) {
        this.connectionString = connectionString;
        this.basecallbackuri = basecallbackuri;
        this.cognitiveServicesUrl = cognitiveServicesUrl;
        this.azureOpenAiServiceKey = azureOpenAiServiceKey;
        this.azureOpenAiServiceEndpoint = azureOpenAiServiceEndpoint;
        this.openAiModelName = openAiModelName;
        this.agentPhoneNumber = agentPhoneNumber;
    }

    public String getCallBackUri() {
        return basecallbackuri + "/api/callback";
    }

    public String getCognitiveServicesUrl() {
        return cognitiveServicesUrl;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public String getAzureOpenAiServiceKey() {
        return azureOpenAiServiceKey;
    }

    public String getAzureOpenAiServiceEndpoint() {
        return azureOpenAiServiceEndpoint;
    }
}
