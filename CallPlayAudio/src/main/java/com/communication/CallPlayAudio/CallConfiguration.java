package com.communication.CallPlayAudio;

import com.communication.CallPlayAudio.EventHandler.EventAuthHandler;

public class CallConfiguration {
    public String connectionString;
    public String sourceIdentity;
    public String sourcePhoneNumber;
    public String appBaseUrl;
    public String audioFileName;
    public String appCallbackUrl;
    public String audioFileUrl;
    public int maxRetryAttemptCount;

    public CallConfiguration(String connectionString, String sourceIdentity, String sourcePhoneNumber,
            String appBaseUrl, String audioFileName, String maxRetryAttemptCount) {
        this.connectionString = connectionString;
        this.sourceIdentity = sourceIdentity;
        this.sourcePhoneNumber = sourcePhoneNumber;
        this.appBaseUrl = appBaseUrl;
        this.audioFileName = audioFileName;
        EventAuthHandler eventhandler = EventAuthHandler.getInstance();
        this.appCallbackUrl = appBaseUrl + "/api/outboundcall/callback?" + eventhandler.getSecretQuerystring();
        audioFileUrl = appBaseUrl + "/audio/" + audioFileName;
        this.maxRetryAttemptCount = Integer.parseInt(maxRetryAttemptCount);


    }

}