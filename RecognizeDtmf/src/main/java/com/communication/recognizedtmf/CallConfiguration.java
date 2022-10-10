package com.communication.recognizedtmf;

import com.communication.recognizedtmf.EventHandler.EventAuthHandler;

public class CallConfiguration {
    public String connectionString;
    public String sourceIdentity;
    public String sourcePhoneNumber;
    public String appBaseUrl;
    public String audioFileName;
    public String appCallbackUrl;
    public String audioFileUrl;
    public int maxRetryAttemptCount;
    public String SalesAudioFileName;
    public String MarketingAudioFileName;
    public String CustomerCareAudioFileName;
    public String InvalidAudioFileName;

    public CallConfiguration(String connectionString, String sourceIdentity, String sourcePhoneNumber,
            String appBaseUrl, String audioFileName, String maxRetryAttemptCount, String salesAudioFileName,
            String marketingAudioFileName, String customerCareAudioFileName, String invalidAudioFileName) {
        this.connectionString = connectionString;
        this.sourceIdentity = sourceIdentity;
        this.sourcePhoneNumber = sourcePhoneNumber;
        this.appBaseUrl = appBaseUrl;
        this.audioFileName = audioFileName;
        EventAuthHandler eventhandler = EventAuthHandler.getInstance();
        this.appCallbackUrl = appBaseUrl + "/api/recognizedtmf/callback?" + eventhandler.getSecretQuerystring();
        audioFileUrl = appBaseUrl + "/audio/";
        this.maxRetryAttemptCount = Integer.parseInt(maxRetryAttemptCount);
        this.SalesAudioFileName = salesAudioFileName;
        this.MarketingAudioFileName = marketingAudioFileName;
        this.CustomerCareAudioFileName = customerCareAudioFileName;
        this.InvalidAudioFileName = invalidAudioFileName;
    }

}