package com.communication.quickstart.callingserver.controllers;

import com.azure.communication.callingserver.EventHandler;
import com.azure.communication.callingserver.models.events.AcsEventType;
import com.azure.communication.callingserver.models.events.AddParticipantsFailedEvent;
import com.azure.communication.callingserver.models.events.AddParticipantsSucceededEvent;
import com.azure.communication.callingserver.models.events.CallConnectedEvent;
import com.azure.communication.callingserver.models.events.CallDisconnectedEvent;
import com.azure.communication.callingserver.models.events.CallingServerBaseEvent;
import com.azure.communication.callingserver.models.events.IncomingCallEvent;
import com.azure.communication.callingserver.models.events.ParticipantsUpdatedEvent;
import com.azure.communication.callingserver.models.events.SubscriptionValidationEvent;

import com.communication.quickstart.callingserver.CallAutomationClient;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.util.List;

public class CallController {

    public Object onIncomingCall(Request req, Response res){
        try {
            List<CallingServerBaseEvent> events = EventHandler.parseEventList(req.body());
            for (CallingServerBaseEvent event : events) {
                if (event.getType() == AcsEventType.SUBSCRIPTION_VALIDATION_EVENT) {
                    res.status(HttpStatus.OK_200);
                    return "{\"validationResponse\": \"" + ((SubscriptionValidationEvent)event).getValidationCode() + "\"}";
                }
                else if (event.getType() == AcsEventType.INCOMING_CALL_EVENT) {
                    String callbackUri = "https://juntuchen.ngrok.io/events";

                    System.out.println("-----Phone is ringing...------");
                    synchronized (this) {
                        this.wait(7000);
                    }
                    System.out.println("------Someone came and is going to pick it up...------");

                    // Answering the incoming call
                    String incomingCallContext = ((IncomingCallEvent)event).getIncomingCallContext();
                    String callConnectionId = CallAutomationClient
                            .getCallAutomationClient()
                            .answerCall(incomingCallContext, callbackUri)
                            .getCallConnectionId();
                    System.out.println("Call answered, callConnectionId: " + callConnectionId);
                    return "";
                }
                else {
                    System.out.println("Empty event grid event");
                }
            }
            return "";
        } catch (Exception e) {
            System.out.println(new RuntimeException(e));
            throw new RuntimeException(e);
        }
    }

    public Object onCloudEvents(Request req, Response res){
        try {
            CallingServerBaseEvent event = EventHandler.parseEvent(req.body());

            if (event == null) {
                System.out.println("Empty cloud event");
                return "";
            }

            System.out.println("<============ " + event.getType() + " ==============>");

            if (event.getType() == AcsEventType.CALL_CONNECTED) {
                CallConnectedEvent temp = (CallConnectedEvent) event;
                System.out.println("callConnectionId: " + temp.getCallConnectionId());
                System.out.println("correlationId: " + temp.getCorrelationId());
            } else if (event.getType() == AcsEventType.CALL_DISCONNECTED) {
                CallDisconnectedEvent temp = (CallDisconnectedEvent) event;
                System.out.println("callConnectionId: " + temp.getCallConnectionId());
                System.out.println("correlationId: " + temp.getCorrelationId());
            } else if (event.getType() == AcsEventType.ADD_PARTICIPANTS_SUCCEEDED) {
                AddParticipantsSucceededEvent temp = (AddParticipantsSucceededEvent) event;
                System.out.println("callConnectionId: " + temp.getCallConnectionId());
                System.out.println("correlationId: " + temp.getCorrelationId());
                temp.getParticipants().forEach(participant -> System.out.println(participant.toString()));
            } else if (event.getType() == AcsEventType.ADD_PARTICIPANTS_FAILED) {
                AddParticipantsFailedEvent temp = (AddParticipantsFailedEvent) event;
                System.out.println("callConnectionId: " + temp.getCallConnectionId());
                System.out.println("correlationId: " + temp.getCorrelationId());
                temp.getParticipants().forEach(participant -> System.out.println(participant.toString()));
            } else if (event.getType() == AcsEventType.PARTICIPANTS_UPDATED) {
                ParticipantsUpdatedEvent temp = (ParticipantsUpdatedEvent) event;
                System.out.println("callConnectionId: " + temp.getCallConnectionId());
                System.out.println("correlationId: " + temp.getCorrelationId());
                temp.getParticipants().forEach(participant -> System.out.println(participant.toString()));
            }

            System.out.println("<========== Event done ============>");
            return "";
        } catch (Exception e) {
            System.out.println(new RuntimeException(e));
            return new RuntimeException(e);
        }
    }

    public CallController() {

    }
}
