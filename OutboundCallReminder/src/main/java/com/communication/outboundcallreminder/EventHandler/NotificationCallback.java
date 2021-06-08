package com.communication.outboundcallreminder.EventHandler;

import com.azure.communication.callingserver.models.events.CallingServerEventBase;

public interface NotificationCallback {
    public void Callback(CallingServerEventBase callEvent);
}