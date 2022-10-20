package com.communication.outboundcallreminder.EventHandler;

import com.azure.communication.callautomation.models.events.CallAutomationEventBase;

public interface NotificationCallback {
    void callback(CallAutomationEventBase callEvent);
}