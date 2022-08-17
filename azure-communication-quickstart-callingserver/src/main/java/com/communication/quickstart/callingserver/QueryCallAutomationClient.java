package com.communication.quickstart.callingserver;

import com.azure.communication.callingserver.CallAutomationClientBuilder;
import com.azure.communication.callingserver.CallAutomationClient;

public class QueryCallAutomationClient {
    private static CallAutomationClient callAutomationClient;

    private QueryCallAutomationClient() {
    }

    public static void initializeCallAutomationClient() {
        if (callAutomationClient == null) {
            String acsConnectionString = "";
            String devPMAEndPoint = "";
            String xPMAEndPoint = "";

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
}
