package com.communication.callautomation.handler;

import com.azure.communication.callautomation.models.DtmfResult;
import com.azure.communication.callautomation.models.events.*;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.communication.callautomation.core.CallAutomationService;
import com.communication.callautomation.core.model.CallState;
import com.communication.callautomation.core.model.EventInfo;
import com.communication.callautomation.core.model.Prompts;
import com.communication.callautomation.exceptions.AzureCallAutomationException;
import com.communication.callautomation.exceptions.InvalidEventPayloadException;
import com.azure.communication.callautomation.models.DtmfTone;
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

import java.io.*;
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
                                .correlationId(correlationId)
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
        int retryCounter = 0;
        for(CallAutomationEventBase acsEvent : events) {
            try{
                String callConnectionId = acsEvent.getCallConnectionId();
                String correlationId = acsEvent.getCorrelationId();
                String target = targetByCorrelationId.get(correlationId);
                if (correlationId == null || callConnectionId == null) {
                    throw new InvalidEventPayloadException("Missing correlationId in CallConnected event !");
                }
                if (target == null) {
                    throw new RuntimeException("Unknown error when retrieving target from memory cache");
                }
                EventInfo eventInfo = EventInfo.builder()
                        .correlationId(correlationId)
                        .fromId(target)
                        .callConnectionId(callConnectionId)
                        .build();

                if (acsEvent instanceof CallConnected) {
                    //Start Recording
                    CallState.setCallState(CallState.CallStateEnum.STARTED);
                    CallState.resetCount();
                    String recordingId = callAutomationService.startRecording(eventInfo);
                    log.info("Started to record the call, Record ID: {}", recordingId);

                    //PlayAudio
                    String responsePlay = callAutomationService.playAudio(eventInfo, Prompts.RECORDINGSTARTED.getMediafile());
                    log.info("Played Recording Started to caller: {}", responsePlay);
                }
                else if(acsEvent instanceof PlayCompleted) {
                    //Single-digit DTMF recognition
                    if (CallState.getCurrentState() == CallState.CallStateEnum.FINISHED) {
                        callAutomationService.terminateCall(eventInfo);
                        log.info("Call hangup executed");
                    } else {
                        String responseDtmfRec = callAutomationService.singleDigitDtmfRecognitionWithPrompt(eventInfo, Prompts.MAINMENU.getMediafile());
                        CallState.setCallState(CallState.CallStateEnum.STARTED);
                        log.info("Started single digit DTMF Recognition: {}", responseDtmfRec);
                    }
                }
                else if(acsEvent instanceof RecognizeCompleted) {
                    RecognizeCompleted event = (RecognizeCompleted) acsEvent;
                    DtmfResult result = (DtmfResult) event.getRecognizeResult().get();
                    DtmfTone tone = result.getTones().get(0);
                    log.info("Tone received {}", tone.convertToString());
                    switch(tone.convertToString()){
                        case "1":
                            log.info("Playing option 1 based on DMTF received from caller");
                            callAutomationService.playAudio(eventInfo, Prompts.CHOICE1.getMediafile());
                            CallState.setCallState(CallState.CallStateEnum.FINISHED);
                            break;
                        case "2":
                            log.info("Playing option 2 based on DMTF received from caller");
                            callAutomationService.playAudio(eventInfo, Prompts.CHOICE2.getMediafile());
                            CallState.setCallState(CallState.CallStateEnum.FINISHED);
                            break;
                        case "3":
                            log.info("Playing option 3 based on DMTF received from caller");
                            callAutomationService.playAudio(eventInfo, Prompts.CHOICE3.getMediafile());
                            CallState.setCallState(CallState.CallStateEnum.FINISHED);
                            break;
                        default:
                            log.info("Choosen DMTF triggered a retry");
                            if (CallState.getIncrementRetryCount() > 2) {
                                log.info("Retry exceed maximum amount set, ending the call");
                                callAutomationService.playAudio(eventInfo, Prompts.GOODBYE.getMediafile());
                                CallState.setCallState(CallState.CallStateEnum.FINISHED);
                            } else {
                                callAutomationService.singleDigitDtmfRecognitionWithPrompt(eventInfo, Prompts.RETRY.getMediafile());
                                CallState.setCallState(CallState.CallStateEnum.ONRETRY);
                                CallState.incrementRetryCount();
                            }
                            break;
                    }
                }
                else if (acsEvent instanceof RecognizeFailed) {
                    RecognizeFailed event = (RecognizeFailed) acsEvent;
                    String reasonFailed = event.getResultInformation().getMessage();
                    log.error("Recognize failed with following error message: {}", reasonFailed);
                    callAutomationService.terminateCall(eventInfo);
                }
                else if (acsEvent instanceof PlayFailed) {
                    PlayFailed event = (PlayFailed) acsEvent;
                    String reasonFailed = event.getResultInformation().getMessage();
                    log.error("Play audio to participant failed with following error message: {}", reasonFailed);
                    callAutomationService.terminateCall(eventInfo);
                }
                else {
                    log.debug("Received unhandled event");
                }
            } catch (AzureCallAutomationException e) {
                throw new AzureCallAutomationException(e.getMessage(), e.getCause());
            } catch (InvalidEventPayloadException e) {
                throw new InvalidEventPayloadException(e.getMessage(), e.getCause());
            }
        }
        return response;
    }


}
