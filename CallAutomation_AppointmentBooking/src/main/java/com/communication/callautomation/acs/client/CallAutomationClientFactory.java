package com.communication.callautomation.acs.client;

import com.azure.communication.callautomation.CallAutomationClient;

public interface CallAutomationClientFactory {
    CallAutomationClient getCallAutomationClient();
}
