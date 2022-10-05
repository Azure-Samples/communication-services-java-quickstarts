package com.communication.CallPlayAudio.EventHandler;

import com.azure.communication.callingserver.models.events.CallingServerEventBase;

public interface NotificationCallback {
    void callback(CallingServerEventBase callEvent);
}