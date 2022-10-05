package com.communication.CallPlayAudio.EventHandler;

import com.azure.communication.callingserver.models.events.AddParticipantResultEvent;
import com.azure.communication.callingserver.models.events.CallConnectionStateChangedEvent;
import com.azure.communication.callingserver.models.events.CallingServerEventBase;
import com.azure.communication.callingserver.models.events.CallingServerEventType;
import com.azure.communication.callingserver.models.events.PlayAudioResultEvent;
import com.azure.communication.callingserver.models.events.ToneReceivedEvent;
import com.azure.core.models.CloudEvent;
import com.azure.core.util.BinaryData;
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
        CallingServerEventBase callEvent = this.extractEvent(request);

        if (callEvent != null) {
            synchronized (this) {
                final NotificationCallback notificationCallback = notificationCallbacks.get(getEventKey(callEvent));
                if (notificationCallback != null) {
                    new Thread(() -> notificationCallback.callback(callEvent)).start();
                }
            }
        }
    }

    public String getEventKey(CallingServerEventBase callEventBase) {
        if (callEventBase.getClass() == CallConnectionStateChangedEvent.class) {
            String callLegId = ((CallConnectionStateChangedEvent) callEventBase).getCallConnectionId();
            return buildEventKey(CallingServerEventType.CALL_CONNECTION_STATE_CHANGED_EVENT.toString(), callLegId);
        } else if (callEventBase.getClass() == ToneReceivedEvent.class) {
            String callLegId = ((ToneReceivedEvent) callEventBase).getCallConnectionId();
            return buildEventKey(CallingServerEventType.TONE_RECEIVED_EVENT.toString(), callLegId);
        } else if (callEventBase.getClass() == PlayAudioResultEvent.class) {
            String operationContext = ((PlayAudioResultEvent) callEventBase).getOperationContext();
            return buildEventKey(CallingServerEventType.PLAY_AUDIO_RESULT_EVENT.toString(), operationContext);
        } else if (callEventBase.getClass() == AddParticipantResultEvent.class) {
            String operationContext = ((AddParticipantResultEvent) callEventBase).getOperationContext();
            return buildEventKey(CallingServerEventType.ADD_PARTICIPANT_RESULT_EVENT.toString(), operationContext);
        }
        
        return null;
    }

    public CallingServerEventBase extractEvent(String content) {
        try {
            List<CloudEvent> cloudEvents = CloudEvent.fromString(content);
            CloudEvent cloudEvent = cloudEvents.get(0);
            BinaryData eventData = cloudEvent.getData();

            if (cloudEvent.getType().equalsIgnoreCase(CallingServerEventType.CALL_CONNECTION_STATE_CHANGED_EVENT.toString())) {
                return CallConnectionStateChangedEvent.deserialize(eventData);
            } else if (cloudEvent.getType().equalsIgnoreCase(CallingServerEventType.TONE_RECEIVED_EVENT.toString())) {
                return ToneReceivedEvent.deserialize(eventData);
            } else if (cloudEvent.getType().equalsIgnoreCase(CallingServerEventType.PLAY_AUDIO_RESULT_EVENT.toString())) {
                return PlayAudioResultEvent.deserialize(eventData);
            } else if (cloudEvent.getType().equalsIgnoreCase(CallingServerEventType.ADD_PARTICIPANT_RESULT_EVENT.toString())) {
                return AddParticipantResultEvent.deserialize(eventData);
            }
        } catch (Exception ex) {
            System.out.println("Failed to parse request content Exception: " + ex.getMessage());
        }

        return null;
    }

}
