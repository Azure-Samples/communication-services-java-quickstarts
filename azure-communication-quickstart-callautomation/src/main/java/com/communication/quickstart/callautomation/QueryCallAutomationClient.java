package com.communication.quickstart.callautomation;

import com.azure.communication.callautomation.CallAutomationAsyncClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationClient;

public class QueryCallAutomationClient {
    private static CallAutomationClient callAutomationClient;
    private static String callbackUrl;

    private QueryCallAutomationClient() {
    }

    public static void initializeCallAutomationClient() {
        if (callAutomationClient == null) {
            String acsConnectionString = "";
            String devPMAEndPoint = "";
            String xPMAEndPoint = "";
            callbackUrl = "";

            callAutomationClient = new CallAutomationClientBuilder()
                    .connectionString(acsConnectionString)
                    .buildClient();
        }
    }

    public static CallAutomationClient getCallAutomationClient() {
        if (callAutomationClient == null) {
            initializeCallAutomationClient();
        }

        return callAutomationClient;
    }

    public static String getCallbackUrl() {
        return callbackUrl;
    }
}
