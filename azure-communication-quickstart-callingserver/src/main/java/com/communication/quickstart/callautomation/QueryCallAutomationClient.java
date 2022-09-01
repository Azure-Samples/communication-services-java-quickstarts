package com.communication.quickstart.callautomation;

import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationClient;

public class QueryCallAutomationClient {
    private static CallAutomationClient callAutomationClient;

    private QueryCallAutomationClient() {
    }

    public static void initializeCallAutomationClient() {
        if (callAutomationClient == null) {
            String acsConnectionString = "endpoint=https://acstestappjuntu.communication.azure.com/;accesskey=pObSPL0lo5yica6qpe7tZ0kElcIFQUoQqZkM09DavShqvBArEMdK/G9MBuDRuftwPSjqYEX/yvmU9Z0WyVtEwA==";
            String devPMAEndPoint = "https://pma-dev-juntuchen.plat-dev.skype.net";
            String xPMAEndPoint = "https://x-pma-euno-01.plat.skype.com";

            callAutomationClient = new CallAutomationClientBuilder()
                    .connectionString(acsConnectionString)
                    //.endpoint(devPMAEndPoint)
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
