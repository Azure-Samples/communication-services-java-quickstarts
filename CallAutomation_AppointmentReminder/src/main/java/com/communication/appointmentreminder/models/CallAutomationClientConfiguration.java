package com.communication.appointmentreminder.models;

import com.communication.appointmentreminder.utitilities.Constants;

public class CallAutomationClientConfiguration {
    private final String connectionString;
    private final String sourceIdentity;
    private final String sourcePhoneNumber;
    private final String appBaseUrl;
    private final String appCallbackUrl;

    public CallAutomationClientConfiguration(String connectionString, String sourceIdentity, String sourcePhoneNumber,
                             String appBaseUrl) {
        this.connectionString = connectionString;
        this.sourceIdentity = sourceIdentity;
        this.sourcePhoneNumber = sourcePhoneNumber;
        this.appBaseUrl = appBaseUrl;
        appCallbackUrl = appBaseUrl + Constants.CALLBACK_PATH;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public String getSourceIdentity() {
        return sourceIdentity;
    }

    public String getSourcePhoneNumber() {
        return sourcePhoneNumber;
    }

    public String getAppBaseUrl() {
        return appBaseUrl;
    }

    public String getAppCallbackUrl() {
        return appCallbackUrl;
    }
}