package com.communication.outboundcallreminder.EventHandler;

import com.azure.communication.callingserver.models.events.*;
import com.azure.core.models.CloudEvent;
import com.azure.core.util.BinaryData;
import java.util.*;

public class EventDispatcher {
    private static EventDispatcher instance = null;
    private Hashtable<String, NotificationCallback> NotificationCallback;

    EventDispatcher() {
        NotificationCallback = new Hashtable<String, NotificationCallback>();
    }

    /// <summary>
    /// Get istace of EventDispatcher
    /// </summary>
    public static EventDispatcher GetInstance() {
        if (instance == null) {
            instance = new EventDispatcher();
        }
        return instance;
    }

    public Boolean Subscribe(String eventType, String eventKey, NotificationCallback notificationCallback) {
        String eventId = BuildEventKey(eventType, eventKey);
        synchronized (this) {
            return (NotificationCallback.put(eventId, notificationCallback) == null);
        }
    }

    public void Unsubscribe(String eventType, String eventKey) {
        String eventId = BuildEventKey(eventType, eventKey);
        synchronized (this) {
            NotificationCallback.remove(eventId);
        }
    }

    public String BuildEventKey(String eventType, String eventKey) {
        return (eventType + "-" + eventKey);
    }

    public void ProcessNotification(String request) {
        CallingServerEventBase callEvent = this.ExtractEvent(request);

        if (callEvent != null) {
            synchronized (this) {
                final NotificationCallback notificationCallback = NotificationCallback.get(GetEventKey(callEvent));
                if (notificationCallback != null) {
                    new Thread(() -> {
                        notificationCallback.Callback(callEvent);
                    }).start();
                }
            }
        }
    }

    public String GetEventKey(CallingServerEventBase callEventBase) {
        if (callEventBase.getClass() == CallConnectionStateChangedEvent.class) {
            String callLegId = ((CallConnectionStateChangedEvent) callEventBase).getCallConnectionId();
            return BuildEventKey(CallingServerEventType.CALL_CONNECTION_STATE_CHANGED_EVENT.toString(), callLegId);
        } else if (callEventBase.getClass() == ToneReceivedEvent.class) {
            String callLegId = ((ToneReceivedEvent) callEventBase).getCallConnectionId();
            return BuildEventKey(CallingServerEventType.TONE_RECEIVED_EVENT.toString(), callLegId);
        } else if (callEventBase.getClass() == PlayAudioResultEvent.class) {
            String operationContext = ((PlayAudioResultEvent) callEventBase).getOperationContext();
            return BuildEventKey(CallingServerEventType.PLAY_AUDIO_RESULT_EVENT.toString(), operationContext);
        } else if (callEventBase.getClass() == InviteParticipantResultEvent.class) {
            String operationContext = ((InviteParticipantResultEvent) callEventBase).getOperationContext();
            return BuildEventKey(CallingServerEventType.INVITE_PARTICIPANT_RESULT_EVENT.toString(), operationContext);
        }
        
        return null;
    }

    public CallingServerEventBase ExtractEvent(String content) {
        try {
            List<CloudEvent> cloudEvents = CloudEvent.fromString(content);
            CloudEvent cloudEvent = cloudEvents.get(0);
            BinaryData eventData = cloudEvent.getData();

            if (cloudEvent.getType().equals(CallingServerEventType.CALL_CONNECTION_STATE_CHANGED_EVENT.toString())) {
                return CallConnectionStateChangedEvent.deserialize(eventData);
            } else if (cloudEvent.getType().equals(CallingServerEventType.TONE_RECEIVED_EVENT.toString())) {
                return ToneReceivedEvent.deserialize(eventData);
            } else if (cloudEvent.getType().equals(CallingServerEventType.PLAY_AUDIO_RESULT_EVENT.toString())) {
                return PlayAudioResultEvent.deserialize(eventData);
            } else if (cloudEvent.getType().equals(CallingServerEventType.INVITE_PARTICIPANT_RESULT_EVENT.toString())) {
                return InviteParticipantResultEvent.deserialize(eventData);
            }
        } catch (Exception ex) {
            System.out.println("Failed to parse request content Exception: " + ex.getMessage());
        }

        return null;
    }

}
