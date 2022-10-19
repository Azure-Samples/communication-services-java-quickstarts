package com.communication.CallPlayAudio.EventHandler;

import com.azure.communication.callautomation.EventHandler;
import com.azure.communication.callautomation.models.events.CallAutomationEventBase;
import java.util.*;

public class EventDispatcher {
    private static EventDispatcher instance = null;
    private final Hashtable<String, NotificationCallback> notificationCallbacks;

    EventDispatcher() {
        notificationCallbacks = new Hashtable<>();
    }

    /// <summary>
    /// Get instace of EventDispatcher
    /// </summary>
    public static EventDispatcher getInstance() {
        if (instance == null) {
            instance = new EventDispatcher();
        }
        return instance;
    }

    public boolean subscribe(String eventType, String eventKey, NotificationCallback notificationCallback) {
        String eventId = buildEventKey(eventType, eventKey);
        synchronized (this) {
            return (notificationCallbacks.put(eventId, notificationCallback) == null);
        }
    }

    public void unsubscribe(String eventType, String eventKey) {
        String eventId = buildEventKey(eventType, eventKey);
        synchronized (this) {
            notificationCallbacks.remove(eventId);
        }
    }

    public String buildEventKey(String eventType, String eventKey) {
        return (eventType + "-" + eventKey);
    }

    public void processNotification(String request) {
        CallAutomationEventBase callEvent = EventHandler.parseEvent(request);
        if (callEvent != null) {
            synchronized (this) {
                final NotificationCallback notificationCallback = notificationCallbacks.
                    get(buildEventKey(callEvent.getClass().getName(), callEvent.getCallConnectionId()));
                if (notificationCallback != null) {
                    new Thread(() -> notificationCallback.callback(callEvent)).start();
                }
            }
        }
    }
}
