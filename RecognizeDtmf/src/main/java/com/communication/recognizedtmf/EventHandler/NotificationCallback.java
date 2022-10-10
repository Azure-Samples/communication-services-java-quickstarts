package com.communication.recognizedtmf.EventHandler;

import com.azure.communication.callautomation.models.events.CallAutomationEventBase;

public interface NotificationCallback {
    void callback(CallAutomationEventBase callEvent);
}