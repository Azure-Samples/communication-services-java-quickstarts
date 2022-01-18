package com.communication.incomingcallsample.utils;

public class CallConfiguration {
    public String connectionString;
    public String appBaseUrl;
    public String appCallbackUrl;
    public String audioFileUrl;
    public String targetParticipant;
    public String[] allowedRecipientList;

    public CallConfiguration(String connectionString, String appBaseUrl, String audioFileUrl, String targetParticipant, String[] allowedRecipientList, String queryString) {
        this.connectionString = connectionString;
        this.appBaseUrl = appBaseUrl;
        this.appCallbackUrl = this.appBaseUrl + "/CallingServerAPICallBacks?" + queryString;
        this.audioFileUrl = audioFileUrl;
        this.targetParticipant = targetParticipant;
        this.allowedRecipientList = allowedRecipientList;
    }

    public static CallConfiguration GetCallConfiguration(ConfigurationManager configurationManager, String queryString) {
        return new CallConfiguration(
            configurationManager.getAppSettings("Connectionstring"),
            configurationManager.getAppSettings("AppCallBackUri"),
            configurationManager.getAppSettings("AudiFileUri"),
            configurationManager.getAppSettings("TargetParticipant"),
            configurationManager.getAppSettings("AllowedRecipientList").split(";"),
            queryString);
    }
}
