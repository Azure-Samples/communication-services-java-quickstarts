package com.communication.outboundcallreminder;

import com.communication.outboundcallreminder.EventHandler.EventAuthHandler;

public class CallConfiguration {
    public String ConnectionString;
    public String SourceIdentity;
    public String SourcePhoneNumber;
    public String AppBaseUrl;
    public String AudioFileName;
    public String AppCallbackUrl;
    public String AudioFileUrl;

    public CallConfiguration(String connectionString, String sourceIdentity, String sourcePhoneNumber,
            String appBaseUrl, String audioFileName) {
        this.ConnectionString = connectionString;
        this.SourceIdentity = sourceIdentity;
        this.SourcePhoneNumber = sourcePhoneNumber;
        this.AppBaseUrl = appBaseUrl;
        this.AudioFileName = audioFileName;
        EventAuthHandler eventhandler = EventAuthHandler.getInstance();
        this.AppCallbackUrl = AppBaseUrl + "/api/outboundcall/callback?" + eventhandler.getSecretQuerystring();
        AudioFileUrl = AppBaseUrl + "/audio/" + AudioFileName;
    }

}