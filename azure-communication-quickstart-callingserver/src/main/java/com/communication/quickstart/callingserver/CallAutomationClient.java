package com.communication.quickstart.callingserver;

import com.azure.communication.callingserver.CallingServerClient;
import com.azure.communication.callingserver.CallingServerClientBuilder;

public class CallAutomationClient {
    private static CallingServerClient callingServerClient;

    private CallAutomationClient() {
    }

    public static void initializeCallAutomationClient() {
        if (callingServerClient == null) {
            String acsConnectionString = "";
            String devPMAEndPoint = "";
            String xPMAEndPoint = "";

            callingServerClient = new CallingServerClientBuilder()
                    .connectionString(acsConnectionString)
                    .endpoint(xPMAEndPoint)
                    .buildClient();
        }
    }

    public static CallingServerClient getCallAutomationClient() {
        if (callingServerClient == null) {
            initializeCallAutomationClient();
        }

        return callingServerClient;
    }
}
