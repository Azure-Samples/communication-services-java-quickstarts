package com.communication.quickstart.callingserver.controllers;

import com.azure.communication.callingserver.EventHandler;
import com.azure.communication.callingserver.models.events.AddParticipantsFailedEvent;
import com.azure.communication.callingserver.models.events.AddParticipantsSucceededEvent;
import com.azure.communication.callingserver.models.events.CallAutomationEventBase;
import com.azure.communication.callingserver.models.events.CallConnectedEvent;
import com.azure.communication.callingserver.models.events.CallDisconnectedEvent;
import com.azure.communication.callingserver.models.events.ParticipantsUpdatedEvent;

import com.azure.messaging.eventgrid.EventGridEvent;
import com.communication.quickstart.callingserver.QueryCallAutomationClient;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.Objects;

public class CallController {

    public Object onIncomingCall(Request req, Response res){
        try {
            List<EventGridEvent> eventGridEvents = EventGridEvent.fromString(req.body());
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            for (EventGridEvent event : eventGridEvents) {
                String data = event.getData().toString();
                JsonNode eventData = mapper.readTree(data);

                if (Objects.equals(event.getEventType(), "Microsoft.EventGrid.SubscriptionValidationEvent")) {
                    res.status(HttpStatus.OK_200);
                    return "{\"validationResponse\": \"" + mapper.convertValue(eventData.get("validationCode"), String.class) + "\"}";
                }
                else if (Objects.equals(event.getEventType(), "Microsoft.Communication.IncomingCall")) {
                    String callbackUri = "https://juntuchen.ngrok.io/events";

                    System.out.println("-----Phone is ringing...------");
                    synchronized (this) {
                        this.wait(5000);
                    }
                    System.out.println("------Someone came and is going to pick it up...------");

                    // Answering the incoming call
                    String incomingCallContext = mapper.convertValue(eventData.get("incomingCallContext"), String.class);
                    String callConnectionId = QueryCallAutomationClient
                            .getCallAutomationClient()
                            .answerCall(incomingCallContext, callbackUri)
                            .getCallConnectionProperties()
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
            CallAutomationEventBase event = EventHandler.parseEvent(req.body());

            if (event == null) {
                System.out.println("Empty Cloud event");
                return "";
            } else {
                System.out.println(req.body());
            }

            if (event.getClass() == CallConnectedEvent.class) {
                CallConnectedEvent temp = (CallConnectedEvent) event;
                System.out.println("callConnectionId: " + temp.getCallConnectionId());
                System.out.println("correlationId: " + temp.getCorrelationId());
            } else if (event.getClass() == CallDisconnectedEvent.class) {
                CallDisconnectedEvent temp = (CallDisconnectedEvent) event;
                System.out.println("callConnectionId: " + temp.getCallConnectionId());
                System.out.println("correlationId: " + temp.getCorrelationId());
            } else if (event.getClass() == AddParticipantsSucceededEvent.class) {
                AddParticipantsSucceededEvent temp = (AddParticipantsSucceededEvent) event;
                System.out.println("callConnectionId: " + temp.getCallConnectionId());
                System.out.println("correlationId: " + temp.getCorrelationId());
                temp.getParticipants().forEach(participant -> System.out.println(participant.toString()));
            } else if (event.getClass() == AddParticipantsFailedEvent.class) {
                AddParticipantsFailedEvent temp = (AddParticipantsFailedEvent) event;
                System.out.println("callConnectionId: " + temp.getCallConnectionId());
                System.out.println("correlationId: " + temp.getCorrelationId());
                temp.getParticipants().forEach(participant -> System.out.println(participant.toString()));
            } else if (event.getClass() == ParticipantsUpdatedEvent.class) {
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
