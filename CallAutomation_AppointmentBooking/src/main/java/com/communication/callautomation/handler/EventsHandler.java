package com.communication.callautomation.handler;

import com.azure.communication.callautomation.models.events.CallAutomationEventBase;
import com.azure.core.implementation.util.StreamUtil;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.communication.callautomation.core.CallAutomationService;
import com.communication.callautomation.core.model.EventInfo;
import com.communication.callautomation.core.model.Prompts;
import com.communication.callautomation.exceptions.AzureCallAutomationException;
import com.communication.callautomation.exceptions.InvalidEventPayloadException;
import com.azure.communication.callautomation.models.events.CallConnected;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.communication.callautomation.exceptions.MediaLoadingException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Slf4j
public class EventsHandler {
    private final CallAutomationService callAutomationService;
    private final Map<String, String> targetByCorrelationId = new HashMap<>();

    //constants
    public static final String VALIDATION_CODE = "validationCode";
    public static final String INCOMING_CALL_CONTEXT = "incomingCallContext";
    public static final String CORRELATION_ID = "correlationId";

    @Autowired
    public EventsHandler(final CallAutomationService callAutomationService) {
        this.callAutomationService = callAutomationService;
    }

    public ResponseEntity<String> handleIncomingEvents(final List<EventGridEvent> events) {
        JSONObject response = new JSONObject();
        for(EventGridEvent eventGridEvent : events) {
            JSONObject eventData = null;
            eventData = new JSONObject(eventGridEvent.getData().toString());
            String eventType = eventGridEvent.getEventType();

            try{
                log.info("Received: {}", eventType);
                switch (eventType){
                    case "Microsoft.EventGrid.SubscriptionValidationEvent":
                        response = handleSubscriptionValidationEvent(EventInfo.builder()
                              .validationCode(getFromEventData(eventData, VALIDATION_CODE, eventType))
                              .build());
                        break;

                    case "Microsoft.Communication.IncomingCall":
                        String callerId = "";
                        String incomingCallContext = "";
                        String correlationId = "";
                        try {
                            callerId = eventData.getJSONObject("from").getString("rawId");
                            incomingCallContext = eventData.getString(INCOMING_CALL_CONTEXT);
                            correlationId = eventData.getString(CORRELATION_ID);

                            targetByCorrelationId.put(correlationId, callerId);
                        } catch (JSONException e) {
                            throw new InvalidEventPayloadException(String.format(Locale.ROOT, "%s, %s",eventType ,e.getMessage()));
                        }
                        response = handleIncomingCallEvent(EventInfo.builder()
                                .incomingCallContext(incomingCallContext)
                                .fromId(callerId)
                                .build());
                        break;

                    default:
                        log.debug("Unknown event type: {}", eventType);
                        return ResponseEntity.status(HttpStatus.OK).body(response.put("message", "Not Implemented").toString());
                }
            } catch (AzureCallAutomationException e) {
                throw new AzureCallAutomationException(String.format(Locale.ROOT,
                        "%s", "%s",
                        eventType, e.getMessage(), e.getCause()));
            }
        }
        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }

    public ResponseEntity<Object> handleMediaLoadingEvents(final String filename) {
        String filePath = "src/main/java/com/communication/callautomation/mediafiles/" + filename;
        File file = new File(filePath);
        InputStreamResource resource = null;

        try{
            resource = new InputStreamResource(new FileInputStream(file));
        } catch (FileNotFoundException ex) {
            throw new MediaLoadingException(ex.getMessage(), ex);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");

        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("audio/x-wav"))
                .body(resource);
    }

    public ResponseEntity<String> handleOngoingEvents(final String reqBody) {
        JSONObject response = new JSONObject();
        List<CallAutomationEventBase> events = CallAutomationEventParser.parseEvents(reqBody);
        response = handleAppointmentBookingScript(events);

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }

    private JSONObject handleSubscriptionValidationEvent(final EventInfo eventInfo) {
        JSONObject response = new JSONObject();
        log.info("Subscription Validation Event received");
        response.put("ValidationResponse", eventInfo.getValidationCode());
        log.info("Subscription Validation Event successful");

        return response;
    }

    private JSONObject handleIncomingCallEvent(final EventInfo eventInfo) {
        JSONObject response = new JSONObject();
        log.info("Received Incoming Call event");
        callAutomationService.answerCall(eventInfo);

        return response;
    }

    private String getFromEventData(final JSONObject data, final String field, final String eventType) throws InvalidEventPayloadException {
        try {
            return data.getString(field);

        } catch (JSONException e) {
            throw new InvalidEventPayloadException(String.format(Locale.ROOT, "%s, %s", eventType, e.getMessage()), e.getCause());
        }
    }

    private JSONObject handleAppointmentBookingScript(final List<CallAutomationEventBase> events) {
        JSONObject response = new JSONObject();

        for(CallAutomationEventBase acsEvent : events) {
            try{
                if (acsEvent instanceof CallConnected) {
                    CallConnected event = (CallConnected) acsEvent;
                    String callConnectionId = event.getCallConnectionId();
                    String correlationId = event.getCorrelationId();
                    String target = targetByCorrelationId.get(correlationId);
                    if (correlationId == null) {
                        throw new InvalidEventPayloadException("Missing correlationId in CallConnected event !");
                    }

                    //Start Recording
                    String recordingId = callAutomationService.startRecording(callConnectionId);
                    log.info("Started to record the call, Record ID: {}", recordingId);

                    //PlayAudio
                    String responsePlay = callAutomationService.playAudio(callConnectionId, target, Prompts.RECORDINGSTARTED.getMediafile());
                    log.info("Played Recording Started to caller: {}", responsePlay);

                    //Single-digit DTMF recognition
                    String responseDtmfRec = callAutomationService.singleDigitDtmfRecognitionWithPrompt(callConnectionId, target, Prompts.MAINMENU.getMediafile());
                    log.info("Started single digit DTMF Recognition: {}", responseDtmfRec);

                    //Play response audio or repeat menu once if no input received.

                    //Terminate call

                }
            } catch (AzureCallAutomationException e) {
                throw new AzureCallAutomationException(String.format(Locale.ROOT,
                        "%s", e.getMessage(), e.getCause()));
            } catch (InvalidEventPayloadException e) {
                throw new InvalidEventPayloadException(String.format(Locale.ROOT,
                        "%s", e.getMessage(), e.getCause()));
            }
        }
        return response;
    }


}
