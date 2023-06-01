package com.communication.callautomation;

import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.communication.identity.implementation.models.CommunicationErrorResponseException;
import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.SystemEventNames;
import com.azure.messaging.eventgrid.systemevents.AcsRecordingFileStatusUpdatedEventData;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationEventData;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationResponse;
import com.sun.jna.platform.win32.Guid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@Slf4j
public class ProgramSample {
    private AppConfig appConfig;
    private CallAutomationClient client;
    private String recordingLocation;
    private String recordingId;

    public ProgramSample(final AppConfig appConfig) {
        this.appConfig = appConfig;
        client = initClient();
        recordingLocation = "";
        recordingId = "";
    }

    @GetMapping(path = "/api/outbound_call")
    public ResponseEntity<String> outboundCall() {
        String callConnectionId = createOutboundCall();
        return ResponseEntity.ok().body("Target participant: "
                + appConfig.getTargetphonenumber() +
                ", CallConnectionId: " + callConnectionId);
    }

    @GetMapping(path = "/download")
    public ResponseEntity<String> callRecordingDownload() {
        return handleCallRecordedMediaDownload();
    }

    @PostMapping(path = "/api/recordingcallback")
    public ResponseEntity<SubscriptionValidationResponse> recordinApiEventGridEvents(@RequestBody final String reqBody) {
        List<EventGridEvent> events = EventGridEvent.fromString(reqBody);
        for (EventGridEvent eventGridEvent : events) {
            if (eventGridEvent.getEventType().equals(SystemEventNames.EVENT_GRID_SUBSCRIPTION_VALIDATION)) {
                return handleSubscriptionValidation(eventGridEvent.getData());
            }
            else if (eventGridEvent.getEventType().equals(SystemEventNames.COMMUNICATION_RECORDING_FILE_STATUS_UPDATED)) {
                return handleCallRecordedFileLocation(eventGridEvent.getData());
            }
            else {
                log.debug("Unhandled event.");
                return ResponseEntity.ok().body(null);
            }
        }
        return ResponseEntity.ok().body(null);
    }

    @PostMapping(path = "/api/callback")
    public ResponseEntity<String> callbackEvents(@RequestBody final String reqBody) {
        List<CallAutomationEventBase> events = CallAutomationEventParser.parseEvents(reqBody);
        for (CallAutomationEventBase event : events) {
            String callConnectionId = event.getCallConnectionId();
            if (event instanceof CallConnected) {
                //start recording
                String callRecordingId = callRecording(callConnectionId);
                log.info("Call recording started with ID: {}", callRecordingId);
                // recognize tones
                recognizeDtmfTones(callConnectionId, "MainMenu.wav");
                log.info("DTMF recognition started.");
            }
            else if (event instanceof RecognizeCompleted) {
                log.info("Recognize Completed event received");
                RecognizeCompleted recognizeEvent = (RecognizeCompleted) event;
                DtmfResult dtmfResult = (DtmfResult) recognizeEvent
                        .getRecognizeResult().get();
                DtmfTone selectedTone = dtmfResult.getTones().get(0);

                switch(selectedTone.convertToString()) {
                    case "1":
                        log.info("Received DTMF tone 1.");
                        playToAll(callConnectionId, "Confirmed.wav");
                        break;

                    case "2":
                        log.info("Received DTMF tone 2.");
                        playToAll(callConnectionId, "Goodbye.wav");
                        break;

                    default:
                        log.info("Unexpected DTMF received: {}", selectedTone.convertToString());
                        playToAll(callConnectionId, "Invalid.wav");
                        break;
                }
            }
            else if(event instanceof PlayCompleted) {
                log.info("Received Play Completed event. Stopping recording and terminating call");
                stopRecording(recordingId);
                hangUp(callConnectionId);
            }
            else if(event instanceof RecognizeFailed || event instanceof PlayFailed) {
                log.error("Received failed event: {}", ((CallAutomationEventBaseWithReasonCode) event)
                        .getResultInformation().getMessage());
                if (((CallAutomationEventBaseWithReasonCode) event)
                        .getResultInformation().getMessage()
                        .contains("Action failed, initial silence timeout reached")) {
                    playToAll(callConnectionId, "Timeout.wav");
                } else {
                    log.error("HangUp due to error event");
                    hangUp(callConnectionId);
                }
            }
        }
        return ResponseEntity.ok().body("");
    }

    private String createOutboundCall() {
        try {
            PhoneNumberIdentifier caller = new PhoneNumberIdentifier(appConfig.getCallerphonenumber());
            PhoneNumberIdentifier target = new PhoneNumberIdentifier(appConfig.getTargetphonenumber());
            CallInvite callInvite = new CallInvite(target, caller);
            CreateCallOptions createCallOptions = new CreateCallOptions(callInvite, appConfig.getCallBackUri());
            Response<CreateCallResult> result = client.createCallWithResponse(createCallOptions, Context.NONE);
            return result.getValue().getCallConnectionProperties().getCallConnectionId();
        } catch (CommunicationErrorResponseException e) {
            log.error("Error when creating call: {} {}",
                    e.getMessage(),
                    e.getCause());
            return "";
        }
    }

    private String callRecording(final String callConnectionId) {
        try {
            log.info("Received Call Connected event, start call recording initiated.");
            ServerCallLocator serverCallLocator = new ServerCallLocator(
                    client.getCallConnection(callConnectionId)
                            .getCallProperties()
                            .getServerCallId());
            StartRecordingOptions startRecordingOptions = new StartRecordingOptions(serverCallLocator);
            Response<RecordingStateResult> response = client.getCallRecording()
                    .startWithResponse(startRecordingOptions, Context.NONE);
            recordingId = response.getValue().getRecordingId();
            return recordingId;
        } catch (Exception e) {
            log.error("An error occurred when starting call recording: {} {}",
                    e.getMessage(),
                    e.getCause());
            return "";
        }
    }

    private void stopRecording(final String recordingId) {
        try {
            client.getCallRecording().stop(recordingId);
            log.info("Call recording stopped");
        } catch (Exception e) {
            log.error("Error when stopping the call recording {} {}",
                    e.getMessage(),
                    e.getCause());
        }
    }

    private void recognizeDtmfTones(final String callConnectionId, final String mediaFile) {
        try {
            PhoneNumberIdentifier target = new PhoneNumberIdentifier(appConfig.getTargetphonenumber());
            CallMediaRecognizeDtmfOptions recognizeDtmfOptions = new CallMediaRecognizeDtmfOptions(target, 1);
            PlaySource playSource = new FileSource()
                    .setUrl(appConfig.getBasecallbackuri() + "/" + mediaFile)
                    .setPlaySourceCacheId(mediaFile);
            recognizeDtmfOptions.setPlayPrompt(playSource)
                    .setInterruptPrompt(true)
                    .setInitialSilenceTimeout(Duration.ofSeconds(15));
            client.getCallConnection(callConnectionId)
                    .getCallMedia()
                    .startRecognizingWithResponse(recognizeDtmfOptions, Context.NONE);
        } catch (Exception e) {
            log.error("Error occurred when attempting to recognize DTMF tones {} {}",
                    e.getMessage(),
                    e.getCause());
        }
    }

    private void playToAll(final String callConnectionId, final String mediaFile) {
        try {
            PlaySource playSource = new FileSource()
                    .setUrl(appConfig.getBasecallbackuri() + "/" + mediaFile)
                    .setPlaySourceCacheId(mediaFile);
            client.getCallConnection(callConnectionId)
                    .getCallMedia().playToAll(playSource);
        } catch (Exception e) {
            log.error("Error occurred when playing media to participant {} {}",
                    e.getMessage(),
                    e.getCause());
        }
    }

    private void hangUp(final String callConnectionId) {
        try {
            client.getCallConnection(callConnectionId).hangUp(true);
            log.info("Terminated call");
        } catch (Exception e) {
            log.error("Error when terminating the call for all participants {} {}",
                    e.getMessage(),
                    e.getCause());
        }
    }

    private ResponseEntity handleCallRecordedMediaDownload() {
        try {
            log.info("Downloading recorded audio from {}",
                    new URI(recordingLocation));

            String uui = UUID.randomUUID().toString();
            client.getCallRecording()
                    .downloadTo(new URI(recordingLocation).toString(),
                            Paths.get("testfile-"+ uui +".wav"));

            return ResponseEntity.ok().body("Downloaded media file at testfile-" + uui);
        } catch (URISyntaxException e) {
            log.error("Error when downloading recording {} {}",
                    e.getMessage(),
                    e.getCause());
            return ResponseEntity.internalServerError().body(null);
        }
    }

    private ResponseEntity handleCallRecordedFileLocation(final BinaryData eventData) {
        try {
            AcsRecordingFileStatusUpdatedEventData fileStatusData = eventData.toObject(AcsRecordingFileStatusUpdatedEventData.class);
            recordingLocation = fileStatusData.getRecordingStorageInfo()
                    .getRecordingChunks()
                    .get(0)
                    .getContentLocation();
            log.info("Received recording location endpoint for call recording");
            return ResponseEntity.ok().body(null);
        } catch (Exception e) {
            log.error("Error getting recording location info {} {}",
                    e.getMessage(),
                    e.getCause());
            return ResponseEntity.ok().body(null);
        }
    }

    private ResponseEntity<SubscriptionValidationResponse> handleSubscriptionValidation(final BinaryData eventData) {
        try {
            log.info("Received Subscription Validation Event from Recording API endpoint");
            SubscriptionValidationEventData subscriptioneventData = eventData.toObject(SubscriptionValidationEventData.class);
            SubscriptionValidationResponse responseData = new SubscriptionValidationResponse();
            responseData.setValidationResponse(subscriptioneventData.getValidationCode());
            return ResponseEntity.ok().body(responseData);
        } catch (Exception e) {
            log.error("Error when responding to file status {} {}",
                    e.getMessage(),
                    e.getCause());
            return ResponseEntity.internalServerError().body(null);
        }
    }

    private CallAutomationClient initClient() {
        CallAutomationClient client;
        try {
            client = new CallAutomationClientBuilder()
                    .connectionString(appConfig.getConnectionString())
                    .buildClient();
            return client;
        } catch (NullPointerException e) {
            log.error("Please verify if Application config is properly set up");
            return null;
        } catch (Exception e) {
            log.error("Error occurred when initializing Call Automation Async Client: {} {}",
                    e.getMessage(),
                    e.getCause());
            return null;
        }
    }
}
