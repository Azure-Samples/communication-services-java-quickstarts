package com.communication.callautomation;

// import com.azure.communication.callautomation.CallAutomationAsyncClient;
import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.CallConnection;
import com.azure.communication.callautomation.CallMedia;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.json.JSONObject;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationResponse;
import com.azure.messaging.eventgrid.systemevents.AcsRecordingFileStatusUpdatedEventData;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationEventData;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.SystemEventNames;

@RestController
public class ProgramSample {

    private static final Logger log = LoggerFactory.getLogger(ProgramSample.class);
    private CallAutomationClient client;
    // private final CallAutomationAsyncClient asyncClient;
    
    // Configuration state variables
    private AppConfig appConfig;
    private String acsConnectionString = "";
    private String cognitiveServicesEndpoint = "";
    private String acsPhoneNumber = "";
    private String targetPhoneNumber = "";
    private String targetAcsUserId = "";
    private String callbackUriHost = "";
    private String websocketUriHost = "";

    private String callConnectionId = "";
    private String recordingId = "";
    private String recordingLocation = "";
    private String recordingFileFormat = "";

    private String confirmLabel = "Confirm";
    private String cancelLabel = "Cancel";

    public ProgramSample(final AppConfig appConfig) {
        this.appConfig = appConfig;

        // Initialize configuration values from AppConfig
        this.acsConnectionString = appConfig.getConnectionString();
        this.callbackUriHost = appConfig.getCallbackUriHost();
        this.acsPhoneNumber = appConfig.getAcsPhoneNumber();
        this.targetPhoneNumber = appConfig.getTargetPhoneNumber();
        this.cognitiveServicesEndpoint = appConfig.getCognitiveServiceEndpoint();
        this.targetAcsUserId = appConfig.getTargetTeamsUserId();
        
        // Set websocketUriHost to same as callbackUriHost for now
        this.websocketUriHost = appConfig.getCallbackUriHost();
        
        client = initClient();
    }
    @Tag(name = "02. Call Automation Events", description = "CallAutomation Events")
    @PostMapping(path = "/api/callbacks")
    public ResponseEntity<String> callbackEvents(@RequestBody final String reqBody) {
        try {
            List<CallAutomationEventBase> events = CallAutomationEventParser.parseEvents(reqBody);
            for (CallAutomationEventBase event : events) {
                callConnectionId = event.getCallConnectionId();
                log.info(
                        "Received call event callConnectionID: {}, serverCallId: {}, CorrelationId: {}, eventType: {}",
                        callConnectionId,
                        event.getServerCallId(),
                        event.getCorrelationId(),
                        event.getClass().getSimpleName());

                if (event instanceof CallConnected) {
                    log.info("****************************************");
                    log.info("CORRELATION ID: {}", event.getCorrelationId());
                    log.info("****************************************");
                    log.info("CALL CONNECTION ID: {}", event.getCallConnectionId());
                    log.info("****************************************");
                    var mediaStreamingSubscription = client.getCallConnection(callConnectionId).getCallProperties()
                            .getMediaStreamingSubscription();
                    var transcriptionSubscription = client.getCallConnection(callConnectionId).getCallProperties()
                            .getTranscriptionSubscription();
                    log.info("MediaStreaming State: {}", mediaStreamingSubscription.getState());
                    log.info("Transcription State: {}", transcriptionSubscription.getState());
                } else if (event instanceof MediaStreamingStarted) {
                    MediaStreamingStarted acsEvent = (MediaStreamingStarted) event;
                    log.info("Operation Context: {}", acsEvent.getOperationContext());
                    log.info("MediaSteaming Status: {}",
                            acsEvent.getMediaStreamingUpdateResult().getMediaStreamingStatus());

                } else if (event instanceof MediaStreamingStopped) {
                    MediaStreamingStopped acsEvent = (MediaStreamingStopped) event;
                    log.info("Operation Context: {}", acsEvent.getOperationContext());
                    log.info("MediaSteaming Status: {}",
                            acsEvent.getMediaStreamingUpdateResult().getMediaStreamingStatus());

                } else if (event instanceof MediaStreamingFailed) {
                    MediaStreamingFailed acsEvent = (MediaStreamingFailed) event;
                    log.info("Operation Context: {}", acsEvent.getOperationContext());
                    log.info("MediaSteaming Status: {}",
                            acsEvent.getMediaStreamingUpdateResult().getMediaStreamingStatus());
                    log.error("Received failed event: {}", acsEvent
                            .getResultInformation().getMessage());
                } else if (event instanceof TranscriptionStarted) {
                    TranscriptionStarted acsEvent = (TranscriptionStarted) event;
                    log.info("Operation Context: {}", acsEvent.getOperationContext());
                    log.info("Transcription Status: {}",
                            acsEvent.getTranscriptionUpdateResult().getTranscriptionStatus());

                } else if (event instanceof TranscriptionUpdated) {
                    TranscriptionUpdated acsEvent = (TranscriptionUpdated) event;
                    log.info("Operation Context: {}", acsEvent.getOperationContext());
                    log.info("Transcription Status: {}",
                            acsEvent.getTranscriptionUpdateResult().getTranscriptionStatus());

                } else if (event instanceof TranscriptionStopped) {
                    TranscriptionStopped acsEvent = (TranscriptionStopped) event;
                    log.info("Operation Context: {}", acsEvent.getOperationContext());
                    log.info("Transcription Status: {}",
                            acsEvent.getTranscriptionUpdateResult().getTranscriptionStatus());

                } else if (event instanceof TranscriptionFailed) {
                    TranscriptionFailed acsEvent = (TranscriptionFailed) event;
                    log.info("Operation Context: {}", acsEvent.getOperationContext());
                    log.info("Transcription Status: {}",
                            acsEvent.getTranscriptionUpdateResult().getTranscriptionStatus());
                    log.error("Received failed event: {}", acsEvent
                            .getResultInformation().getMessage());
                } else if (event instanceof RecognizeCompleted) {
                    RecognizeCompleted acsEvent = (RecognizeCompleted) event;
                    RecognizeResult recognizeResult = acsEvent.getRecognizeResult().get();
                    if (recognizeResult instanceof DtmfResult) {
                        // Take action on collect tones
                        DtmfResult dtmfResult = (DtmfResult) recognizeResult;
                        List<DtmfTone> tones = dtmfResult.getTones();
                        log.info("Recognition completed, tones=" + tones + ", context="
                                + acsEvent.getOperationContext());
                    } else if (recognizeResult instanceof ChoiceResult) {
                        ChoiceResult collectChoiceResult = (ChoiceResult) recognizeResult;
                        String labelDetected = collectChoiceResult.getLabel();
                        String phraseDetected = collectChoiceResult.getRecognizedPhrase();
                    } else if (recognizeResult instanceof SpeechResult) {
                        SpeechResult speechResult = (SpeechResult) recognizeResult;
                        String text = speechResult.getSpeech();
                        log.info("Recognition completed, text=" + text + ", context=" + acsEvent.getOperationContext());
                    } else {
                        log.info("Recognition completed, result=" + recognizeResult + ", context="
                                + acsEvent.getOperationContext());
                    }
                } else if (event instanceof RecognizeFailed) {

                    RecognizeFailed acsEvent = (RecognizeFailed) event;
                    log.info("Operation Context: {}", acsEvent.getOperationContext());
                    log.info("FailedPlaySourceIndex: {}",
                            acsEvent.getFailedPlaySourceIndex());
                    log.error("Received failed event: {}", acsEvent
                            .getResultInformation().getMessage());

                } else if (event instanceof PlayCompleted) {
                    PlayCompleted acsEvent = (PlayCompleted) event;
                    log.info("Operation Context: {}", acsEvent.getOperationContext());
                } else if (event instanceof PlayFailed) {

                    PlayFailed acsEvent = (PlayFailed) event;
                    log.info("Operation Context: {}", acsEvent.getOperationContext());
                    log.info("FailedPlaySourceIndex: {}",
                            acsEvent.getFailedPlaySourceIndex());
                    log.error("Received failed event: {}", acsEvent
                            .getResultInformation().getMessage());

                } else if (event instanceof RecordingStateChanged) {
                    RecordingStateChanged acsEvent = (RecordingStateChanged) event;
                    log.info("Recording State Changed event received: {}",
                            event.getCallConnectionId());
                    log.info("Recording State: {}", acsEvent.getRecordingState());
                } else if (event instanceof CallTransferAccepted) {
                    CallTransferAccepted acsEvent = (CallTransferAccepted) event;
                    log.info("Operation Context: {}", acsEvent.getOperationContext());
                } else if (event instanceof CallTransferFailed) {

                    CallTransferFailed acsEvent = (CallTransferFailed) event;
                    log.info("Operation Context: {}", acsEvent.getOperationContext());
                    log.error("Received failed event: {}", acsEvent
                            .getResultInformation().getMessage());
                }
            }
            return ResponseEntity.ok().body("");
        } catch (Exception e) {
            log.error("Error processing callback events: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process callback events.");
        }
    }

    private void handleIncomingCall(final BinaryData eventData) {
        JSONObject data = new JSONObject(eventData.toString());
        String callbackUri;
        AnswerCallOptions options;
        String cognitiveServicesUrl;
        String websocketUrl;

        try {
            callbackUri = callbackUriHost + "/api/callbacks";
            // Replace "https://" with "wss://" for WebSocket protocol
            websocketUrl = websocketUriHost;
            System.out.println("WebSocket URL: " + websocketUrl);
            MediaStreamingOptions mediaStreamingOptions = new MediaStreamingOptions(MediaStreamingAudioChannel.UNMIXED);
            mediaStreamingOptions.setTransportUrl(websocketUrl);
            mediaStreamingOptions.setStartMediaStreaming(false);
            mediaStreamingOptions.setEnableDtmfTones(false);
            mediaStreamingOptions.setEnableBidirectional(false);
            mediaStreamingOptions.setAudioFormat(AudioFormat.PCM_16K_MONO);

            TranscriptionOptions transcriptionOptions = new TranscriptionOptions("en-ES");
            transcriptionOptions.setTransportUrl(websocketUriHost);
            transcriptionOptions.setStartTranscription(false);

            options = new AnswerCallOptions(data.getString("incomingCallContext"),
                    callbackUri);
            options.setMediaStreamingOptions(mediaStreamingOptions);
            options.setTranscriptionOptions(transcriptionOptions);

            Response<AnswerCallResult> answerCallResponse = client.answerCallWithResponse(options, Context.NONE);

            log.info("Incoming call answered. Cognitive Services Url: {}\nCallbackUri: {}\nCallConnectionId: {}",
                    cognitiveServicesEndpoint,
                    callbackUri,
                    answerCallResponse.getValue().getCallConnectionProperties().getCallConnectionId());
        } catch (Exception e) {
            log.error("Error getting recording location info {} {}",
                    e.getMessage(),
                    e.getCause());
        }
    }

    private ResponseEntity<SubscriptionValidationResponse> handleSubscriptionValidation(final BinaryData eventData) {
        try {
            log.info("Received Subscription Validation Event from Incoming Call API endpoint");
            SubscriptionValidationEventData subscriptioneventData = eventData
                    .toObject(SubscriptionValidationEventData.class);
            SubscriptionValidationResponse responseData = new SubscriptionValidationResponse();
            responseData.setValidationResponse(subscriptioneventData.getValidationCode());
            return ResponseEntity.ok().body(responseData);
        } catch (Exception e) {
            log.error("Error at subscription validation event {} {}",
                    e.getMessage(),
                    e.getCause());
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @Tag(name = "02. Call Automation Events", description = "CallAutomation Events")
    @PostMapping(path = "/api/incomingCall")
    public ResponseEntity<SubscriptionValidationResponse> recordinApiEventGridEvents(
            @RequestBody final String reqBody) {
        List<EventGridEvent> events = EventGridEvent.fromString(reqBody);
        for (EventGridEvent eventGridEvent : events) {
            if (eventGridEvent.getEventType().equals(SystemEventNames.EVENT_GRID_SUBSCRIPTION_VALIDATION)) {
                return handleSubscriptionValidation(eventGridEvent.getData());
            } else if (eventGridEvent.getEventType().equals(SystemEventNames.COMMUNICATION_INCOMING_CALL)) {
                handleIncomingCall(eventGridEvent.getData());
            }
        }
        return ResponseEntity.ok().body(null);
    }

    @Tag(name = "02. Call Automation Events", description = "CallAutomation Events")
    @PostMapping("/api/recordingFileStatus")
    public ResponseEntity<SubscriptionValidationResponse> handleRecordingFileStatus(@RequestBody String reqBody) {
        List<EventGridEvent> events = EventGridEvent.fromString(reqBody);
        log.info("RECORDING FILE STATUS UPDATED EVENT GRID EVENT RECEIVED.");
        for (EventGridEvent eventGridEvent : events) {
            if (eventGridEvent.getEventType().equals(SystemEventNames.EVENT_GRID_SUBSCRIPTION_VALIDATION)) {
                return handleSubscriptionValidation(eventGridEvent.getData());
            } else if (eventGridEvent.getEventType()
                    .equals(SystemEventNames.COMMUNICATION_RECORDING_FILE_STATUS_UPDATED)) {
                log.info("The event received for recording file status update");
                AcsRecordingFileStatusUpdatedEventData recordingFileStatusUpdatedEventData = eventGridEvent.getData()
                        .toObject(AcsRecordingFileStatusUpdatedEventData.class);
                recordingLocation = recordingFileStatusUpdatedEventData.getRecordingStorageInfo().getRecordingChunks()
                        .get(0).getContentLocation();
                String recordingMetadataLocation = recordingFileStatusUpdatedEventData.getRecordingStorageInfo()
                        .getRecordingChunks()
                        .get(0).getMetadataLocation();
                String recordingDeleteLocation = recordingFileStatusUpdatedEventData.getRecordingStorageInfo()
                        .getRecordingChunks()
                        .get(0).getDeleteLocation();
                log.info("The recording location is : {}", recordingLocation);
                log.info("The recording metadata location is : {}", recordingMetadataLocation);
                log.info("The recording delete location is : {}", recordingDeleteLocation);

            } else {
                log.debug("Unhandled event.");
            }
        }

        return ResponseEntity.ok().build();
    }

    // POST: /outboundCallAsync
    @Tag(name = "03. Outbound Call APIs", description = "Outbound Call APIs")
    @PostMapping("/outboundCallAsync")
    public ResponseEntity<String> outboundCallAsync(@RequestParam String target,
            @RequestParam boolean isPSTN) {

        try {
            String callbackUri = callbackUriHost + "/api/callbacks";
            if (isPSTN) {
                PhoneNumberIdentifier targetParticipant = new PhoneNumberIdentifier(target);
                PhoneNumberIdentifier caller = new PhoneNumberIdentifier(acsPhoneNumber);
                CallInvite callInvite = new CallInvite(targetParticipant, caller);
                CreateCallOptions createCallOptions = new CreateCallOptions(callInvite, callbackUri);
                CallIntelligenceOptions callIntelligenceOptions = new CallIntelligenceOptions();
                createCallOptions.setCallIntelligenceOptions(callIntelligenceOptions);

                log.info("Creating async call to PSTN with target: {}, caller: {}, callbackUri: {}",
                        targetParticipant.getRawId(), caller.getRawId(), callbackUri);
                // Make async call and block to get the result
                Response<CreateCallResult> response = client.createCallWithResponse(createCallOptions, Context.NONE);

                if (response != null && response.getValue() != null) {
                    callConnectionId = response.getValue().getCallConnectionProperties().getCallConnectionId();
                    log.info("Created async pstn call with connection id: " + callConnectionId);
                } else {
                    log.error("Failed to create call. Response or value was null.");
                }
                return ResponseEntity.ok("Created async call with connection id: " + callConnectionId);
            } else {
                CommunicationUserIdentifier targetParticipant = new CommunicationUserIdentifier(target);
                CallInvite callInvite = new CallInvite(targetParticipant);

                CreateCallOptions createCallOptions = new CreateCallOptions(callInvite, callbackUri.toString());
                CallIntelligenceOptions callIntelligenceOptions = new CallIntelligenceOptions();
                createCallOptions.setCallIntelligenceOptions(callIntelligenceOptions);

                Response<CreateCallResult> result = client.createCallWithResponse(createCallOptions, Context.NONE);
                callConnectionId = result.getValue().getCallConnectionProperties().getCallConnectionId();
                log.info("Created async call with connection id: " + callConnectionId);
                return ResponseEntity.ok("Created async call with connection id: " + callConnectionId);
            }
        } catch (Exception e) {
            log.error("Error creating call : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create call.");
        }
    }

    @Tag(name = "03. Outbound Call APIs", description = "Outbound Call APIs")
    @PostMapping("/outboundCall")
    public ResponseEntity<String> outboundCallToPstn(@RequestParam String target,
            @RequestParam boolean isPSTN) {
        try {
            if (isPSTN) {
                PhoneNumberIdentifier targetParticipant = new PhoneNumberIdentifier(target);
                PhoneNumberIdentifier caller = new PhoneNumberIdentifier(acsPhoneNumber);

                URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");
                CallInvite callInvite = new CallInvite(targetParticipant, caller);

                // ✅ Convert URI to String
                CreateCallResult result = client.createCall(callInvite, callbackUri.toString());
                callConnectionId = result.getCallConnectionProperties().getCallConnectionId();
                log.info("Created call with connection id: " + callConnectionId);
                return ResponseEntity.ok("Created async call with connection id: " + callConnectionId);
            } else {
                CommunicationUserIdentifier targetParticipant = new CommunicationUserIdentifier(target);
                CallInvite callInvite = new CallInvite(targetParticipant);
                URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");

                CreateCallResult result = client.createCall(callInvite, callbackUri.toString());
                callConnectionId = result.getCallConnectionProperties().getCallConnectionId();
                log.info("Created call with connection id: " + callConnectionId);
                return ResponseEntity.ok("Created call with connection id: " + callConnectionId);
            }
        } catch (Exception e) {
            log.error("Error creating call : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create call.");
        }
    }

    @Tag(name = "04. Disconnect Call APIs", description = "Disconnect call APIs")
    @PostMapping("/hangupAsync")
    public ResponseEntity<String> hangupAsync(@RequestParam String callConnectionId, @RequestParam boolean isForEveryOne) {
        try {
            CallConnection callConnection = getConnection(callConnectionId);

            callConnection.hangUpWithResponse(isForEveryOne, Context.NONE);
            log.info("Call hangup requested (async) forEveryone={}", isForEveryOne);

            return ResponseEntity.ok("Call hangup requested (async).");
        } catch (Exception e) {
            log.error("Error hanging up call: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to hang up call.");
        }
    }

    @Tag(name = "04. Disconnect Call APIs", description = "Disconnect call APIs")
    @PostMapping("/hangup")
    public ResponseEntity<String> hangup(@RequestParam String callConnectionId, @RequestParam boolean isForEveryOne) {
        try {
            CallConnection callConnection = getConnection(callConnectionId);

            callConnection.hangUp(isForEveryOne);
            log.info("Call hangup requested (sync) forEveryone={}", isForEveryOne);

            return ResponseEntity.ok("Call hangup requested.");
        } catch (Exception e) {
            log.error("Error hanging up call: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to hang up call.");
        }
    }

    @Tag(name = "05. Hold Participant APIs", description = "Hold Participant APIs")
    @PostMapping("/holdParticipantAsync")
    public ResponseEntity<String> holdParticipantAsync(@RequestParam String callConnectionId, @RequestParam String targetParticipant,
            @RequestParam boolean isPSTN, @RequestParam boolean isPlaySource) {
        try {
            CommunicationIdentifier target;
            if (isPSTN) {
                target = new PhoneNumberIdentifier(targetParticipant);
            } else {
                target = new CommunicationUserIdentifier(targetParticipant);
            }
            HoldOptions holdOptions = new HoldOptions(target).setOperationContext("holdUserContext");
            CallMedia callMediaService = getCallMedia(callConnectionId);

            if (isPlaySource) {
                TextSource textSource = new TextSource()
                        .setText("You are on hold. Please wait...")
                        .setVoiceName("en-US-NancyNeural")
                        .setSourceLocale("en-US")
                        .setVoiceKind(VoiceKind.MALE);
                holdOptions.setPlaySource(textSource);
            }

            callMediaService.holdWithResponse(holdOptions, Context.NONE);
            log.info("Held participant asynchronously with playSource = {}", isPlaySource);
            return ResponseEntity.ok("Participant held (async).");
        } catch (Exception e) {
            log.error("Error holding participant asynchronously: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to hold participant asynchronously.");
        }
    }

    @Tag(name = "05. Hold Participant APIs", description = "Hold Participant APIs")
    @PostMapping("/holdParticipant")
    public ResponseEntity<String> holdParticipant(@RequestParam String callConnectionId, @RequestParam String targetParticipant,
            @RequestParam boolean isPSTN, @RequestParam boolean isPlaySource) {
        try {
            CommunicationIdentifier target;
            if (isPSTN) {
                target = new PhoneNumberIdentifier(targetParticipant);
            } else {
                target = new CommunicationUserIdentifier(targetParticipant);
            }
            TextSource textSource = null;
            CallMedia callMediaService = getCallMedia(callConnectionId);

            if (isPlaySource) {
                textSource = new TextSource()
                        .setText("You are on hold. Please wait...")
                        .setVoiceName("en-US-NancyNeural")
                        .setSourceLocale("en-US")
                        .setVoiceKind(VoiceKind.MALE);
            }

            callMediaService.hold(target, textSource);
            log.info("Held participant synchronously with playSource = {}", isPlaySource);
            return ResponseEntity.ok("Participant held.");
        } catch (Exception e) {
            log.error("Error holding participant: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to hold participant.");
        }
    }

    @Tag(name = "05. Hold Participant APIs", description = "Hold Participant APIs")
    @PostMapping("/unholdParticipantAsync")
    public ResponseEntity<String> unholdParticipantAsync(@RequestParam String callConnectionId, @RequestParam String targetParticipant,
            @RequestParam boolean isPSTN) {
        try {
            CommunicationIdentifier target;
            if (isPSTN) {
                target = new PhoneNumberIdentifier(targetParticipant);
            } else {
                target = new CommunicationUserIdentifier(targetParticipant);
            }
            UnholdOptions unholdOptions = new UnholdOptions(target).setOperationContext("unholdUserContext");
            CallMedia callMediaService = getCallMedia(callConnectionId);

            log.info("Unhold participant asynchronously {}", targetParticipant);
            return ResponseEntity.ok("Participant unheld (async).");
        } catch (Exception e) {
            log.error("Error unholding participant asynchronously: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to unhold participant asynchronously.");
        }
    }

    @Tag(name = "05. Hold Participant APIs", description = "Hold Participant APIs")
    @PostMapping("/unholdParticipant")
    public ResponseEntity<String> unholdParticipant(@RequestParam String callConnectionId, @RequestParam String targetParticipant,
            @RequestParam boolean isPSTN) {
        try {
            CommunicationIdentifier target;
            if (isPSTN) {
                target = new PhoneNumberIdentifier(targetParticipant);
            } else {
                target = new CommunicationUserIdentifier(targetParticipant);
            }
            CallMedia callMediaService = getCallMedia(callConnectionId);

            callMediaService.unhold(target);
            log.info("Unhold participant synchronously {}", targetParticipant);
            return ResponseEntity.ok("Participant unheld.");
        } catch (Exception e) {
            log.error("Error unholding participant: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to unhold participant.");
        }
    }

    @Tag(name = "06. Get Participant APIs", description = "Get Participant APIs")
    @PostMapping("/getParticipantAsync")
    public ResponseEntity<String> getParticipantAsync(@RequestParam String callConnectionId, @RequestParam String targetParticipant,
            @RequestParam boolean isPSTN) {
        try {
            CallConnection callConnection = getConnection(callConnectionId);

            CommunicationIdentifier target;
            if (isPSTN) {
                target = new PhoneNumberIdentifier(targetParticipant);
            } else {
                target = new CommunicationUserIdentifier(targetParticipant);
            }
            Response<CallParticipant> response = callConnection.getParticipantWithResponse(
                    target,
                    Context.NONE
            );

            CallParticipant participant = response.getValue();

            if (participant != null) {
                log.info("Participant: --> {}", participant.getIdentifier().getRawId());
                log.info("Is Participant on hold: --> {}", participant.isOnHold());
            } else {
                log.warn("No participant found for identifier: {}", targetParticipant);
            }
            return ResponseEntity.ok("Participant found");
        } catch (Exception e) {
            log.error("Error getting participant asynchronously: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to get participant asynchronously.");
        }
    }

    @Tag(name = "06. Get Participant APIs", description = "Get Participant APIs")
    @PostMapping("/getParticipant")
    public ResponseEntity<String> getParticipant(@RequestParam String callConnectionId, @RequestParam String targetParticipant,
            @RequestParam boolean isPSTN) {
        try {
            CallConnection callConnection = getConnection(callConnectionId);
            CommunicationIdentifier target;
            if (isPSTN) {
                target = new PhoneNumberIdentifier(targetParticipant);
            } else {
                target = new CommunicationUserIdentifier(targetParticipant);
            }
            CallParticipant participant = callConnection.getParticipant(target);

            if (participant != null) {
                log.info("Participant: --> {}", participant.getIdentifier().getRawId());
                log.info("Is Participant on hold: --> {}", participant.isOnHold());
            }
            return ResponseEntity.ok("Participant found");
        } catch (Exception e) {
            log.error("Error getting participant: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to get participant.");
        }
    }

    @Tag(name = "06. Get Participant APIs", description = "Get Participant APIs")
    @PostMapping("/getParticipantListAsync")
    public ResponseEntity<String> getParticipantListAsync(@RequestParam String callConnectionId) {
        try {
            CallConnection callConnection = getConnection(callConnectionId);

            PagedIterable<CallParticipant> participants = callConnection.listParticipants(Context.NONE);

            if (participants != null) {
                StringBuilder participantList = new StringBuilder();
                for (CallParticipant participant : participants) {
                    participantList.append("----------------------------------------------------------------------\n");
                    participantList.append("Participant: --> ").append(participant.getIdentifier().getRawId()).append("\n");
                    participantList.append("Is Participant on hold: --> ").append(participant.isOnHold()).append("\n");
                    participantList.append("----------------------------------------------------------------------\n");
                }
                return ResponseEntity.ok(participantList.toString());
            } else {
                log.warn("No participants returned in the response.");
                return ResponseEntity.ok("No participants found");
            }
        } catch (Exception e) {
            log.error("Error getting participant list asynchronously: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to get participant list asynchronously.");
        }
    }

    @Tag(name = "06. Get Participant APIs", description = "Get Participant APIs")
    @PostMapping("/getParticipantList")
    public ResponseEntity<String> getParticipantList(@RequestParam String callConnectionId) {
        try {
            CallConnection callConnection = getConnection(callConnectionId);

            PagedIterable<CallParticipant> participants = callConnection.listParticipants();

            if (participants != null) {
                for (CallParticipant participant : participants) {
                    log.info("----------------------------------------------------------------------");
                    log.info("Participant: --> {}", participant.getIdentifier().getRawId());
                    log.info("Is Participant on hold: --> {}", participant.isOnHold());
                    log.info("----------------------------------------------------------------------");
                }
                return ResponseEntity.ok("Participant list retrieved");
            } else {
                log.warn("No participants returned in the response.");
                return ResponseEntity.ok("No participants found");
            }
        } catch (Exception e) {
            log.error("Error getting participant list: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Tag(name = "07. Mute Participant APIs", description = "Mute Participant APIs")
    @PostMapping("/muteParticipantAsync")
    public ResponseEntity<String> muteParticipantAsync(@RequestParam String callConnectionId, @RequestParam String targetAcsUserId) {
        try {
            CommunicationIdentifier target = new CommunicationUserIdentifier(targetAcsUserId);
            CallConnection callConnection = getConnection(callConnectionId);

            MuteParticipantOptions options = new MuteParticipantOptions(target)
                    .setOperationContext("muteContext");

            // Assuming you're calling a method like muteParticipantWithResponse(options, context)
            callConnection.muteParticipantWithResponse(options, Context.NONE);

            log.info("Muted participant asynchronously: {}", targetAcsUserId);
            return ResponseEntity.ok("Muted participant (async).");
        } catch (Exception e) {
            log.error("Error muting participant asynchronously: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to mute participant asynchronously.");
        }
    }

    @Tag(name = "07. Mute Participant APIs", description = "Mute Participant APIs")
    @PostMapping("/muteParticipant")
    public ResponseEntity<String> muteParticipant(@RequestParam String callConnectionId, @RequestParam String targetAcsUserId) {
        try {
            CommunicationIdentifier target = new CommunicationUserIdentifier(targetAcsUserId);
            CallConnection callConnection = getConnection(callConnectionId);

            callConnection.muteParticipant(target); // Synchronous mute using options if method is available
            log.info("Muted participant synchronously: {}", targetAcsUserId);
            return ResponseEntity.ok("Muted participant.");
        } catch (Exception e) {
            log.error("Error muting participant: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to mute participant.");
        }
    }

    @Tag(name = "08. Add/Remove Participant APIs", description = "Add/Remove Participant APIs")
    @PostMapping("/addParticipantAsync")
    public ResponseEntity<Object> addParticipantAsync(@RequestParam String callConnectionId, @RequestParam String participant, @RequestParam boolean isPSTN) {
        CallConnection callConnectionService = getConnection(callConnectionId);
        CommunicationIdentifier target;
        CallInvite callInvite;
        if (isPSTN) {
            PhoneNumberIdentifier phoneTarget = new PhoneNumberIdentifier(participant);
            PhoneNumberIdentifier caller = new PhoneNumberIdentifier(acsPhoneNumber);
            callInvite = new CallInvite(phoneTarget, caller);
        } else {
            CommunicationUserIdentifier userTarget = new CommunicationUserIdentifier(participant);
            callInvite = new CallInvite(userTarget);
        }
        AddParticipantOptions options = new AddParticipantOptions(callInvite);
        options.setOperationContext("addPstnUserContext");
        options.setInvitationTimeout(Duration.ofSeconds(15));
        Response<AddParticipantResult> result = callConnectionService.addParticipantWithResponse(options, Context.NONE);
        return ResponseEntity.ok("Invitation Id: " + result.getValue().getInvitationId());
    }

    @Tag(name = "08. Add/Remove Participant APIs", description = "Add/Remove Participant APIs")
    @PostMapping("/addParticipant")
    public ResponseEntity<Object> addParticipant(@RequestParam String callConnectionId, @RequestParam String participant, @RequestParam boolean isPSTN) {
        CallConnection callConnectionService = getConnection(callConnectionId);
        CommunicationIdentifier target;
        CallInvite callInvite;
        if (isPSTN) {
            PhoneNumberIdentifier phoneTarget = new PhoneNumberIdentifier(participant);
            PhoneNumberIdentifier caller = new PhoneNumberIdentifier(acsPhoneNumber);
            callInvite = new CallInvite(phoneTarget, caller);
        } else {
            CommunicationUserIdentifier userTarget = new CommunicationUserIdentifier(participant);
            callInvite = new CallInvite(userTarget);
        }// Replace with actual ACS number
        AddParticipantResult result = callConnectionService.addParticipant(callInvite);
        return ResponseEntity.ok("Invitation Id: " + result.getInvitationId());
    }

    @Tag(name = "08. Add/Remove Participant APIs", description = "Add/Remove Participant APIs")
    @PostMapping("/removeParticipantAsync")
    public ResponseEntity<String> removeParticipantAsync(@RequestParam String callConnectionId, @RequestParam String participant, @RequestParam boolean isPSTN) {
        try {
            RemoveParticipantOptions options;
            if (isPSTN) {
                options = new RemoveParticipantOptions(new PhoneNumberIdentifier(participant));
            } else {
                options = new RemoveParticipantOptions(new CommunicationUserIdentifier(participant));
            }
            options.setOperationContext("removeParticipantContext");
            CallConnection callConnectionService = getConnection(callConnectionId);
            callConnectionService.removeParticipantWithResponse(options, Context.NONE);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error removing participant asynchronously: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Tag(name = "08. Add/Remove Participant APIs", description = "Add/Remove Participant APIs")
    @PostMapping("/removeParticipant")
    public ResponseEntity<String> removeParticipant(@RequestParam String callConnectionId, @RequestParam String participant, @RequestParam boolean isPSTN) {
        try {
            CallConnection callConnectionService = getConnection(callConnectionId);
            RemoveParticipantOptions options;
            if (isPSTN) {
                options = new RemoveParticipantOptions(new PhoneNumberIdentifier(participant));
            } else {
                options = new RemoveParticipantOptions(new CommunicationUserIdentifier(participant));
            }
            callConnectionService.removeParticipantWithResponse(options, Context.NONE);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error removing participant: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to remove participant.");
        }
    }

    @Tag(name = "09. Transfer Call APIs", description = "APIs for transferring calls to participants")
    @PostMapping("/transferCallToParticipantAsync")
    public ResponseEntity<String> transferCallToParticipantAsync(@RequestParam String callConnectionId, @RequestParam String transferTarget, @RequestParam String transferee, @RequestParam boolean isPSTN) {
        try {
            CommunicationIdentifier target;
            if (isPSTN) {
                target = new PhoneNumberIdentifier(transferTarget);
                TransferCallToParticipantOptions options = new TransferCallToParticipantOptions(target)
                        .setOperationContext("TransferCallContext")
                        .setSourceCallerIdNumber(new PhoneNumberIdentifier(transferee));
                client.getCallConnection(callConnectionId)
                        .transferCallToParticipantWithResponse(options, Context.NONE);
                log.info("Call transferred asynchronously to participant: {}", transferTarget);
                return ResponseEntity.ok("Transfer call request sent asynchronously.");
            } else {
                target = new CommunicationUserIdentifier(transferTarget);
                TransferCallToParticipantOptions options = new TransferCallToParticipantOptions(target)
                        .setOperationContext("TransferCallContext");
                client.getCallConnection(callConnectionId)
                        .transferCallToParticipantWithResponse(options, Context.NONE);
                log.info("Call transferred asynchronously to participant: {}", transferTarget);
                return ResponseEntity.ok("Transfer call request sent asynchronously.");
            }
        } catch (Exception e) {
            log.error("Error transferring call asynchronously: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to transfer call asynchronously.");
        }
    }

    @Tag(name = "09. Transfer Call APIs", description = "APIs for transferring calls to participants")
    @PostMapping("/transferCallToParticipant")
    public ResponseEntity<String> transferCallToParticipant(@RequestParam String callConnectionId, @RequestParam String transferTarget, @RequestParam String transferee, @RequestParam boolean isPSTN) {
        try {
            CommunicationIdentifier targetParticipant;
            if (isPSTN) {
                targetParticipant = new PhoneNumberIdentifier(transferTarget);
                TransferCallToParticipantOptions options = new TransferCallToParticipantOptions(targetParticipant)
                        .setOperationContext("TransferCallContext")
                        .setSourceCallerIdNumber(new PhoneNumberIdentifier(transferee));

                // Transfer the call 
                client.getCallConnection(callConnectionId)
                        .transferCallToParticipantWithResponse(options, Context.NONE);
                

                log.info("Call transferred to participant: {}", targetParticipant);
                return ResponseEntity.ok("Transfer call request sent.");
            } else {
                targetParticipant = new CommunicationUserIdentifier(transferTarget);
                // Synchronous transfer using CommunicationIdentifier
                client.getCallConnection(callConnectionId)
                        .transferCallToParticipant(targetParticipant);
                log.info("Call transferred to participant: {}", transferTarget);
                return ResponseEntity.ok("Transfer call request sent.");
            }
        } catch (Exception e) {
            log.error("Error transferring call : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to transfer call.");
        }
    }
        
    @Tag(name = "10. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playFileSourceToTargetAsync")
    public ResponseEntity<String> playFileSourceToTargetAsync(@RequestParam String callConnectionId,
    @RequestParam String participant,
    @RequestParam boolean isPSTN) {
        return playFile(callConnectionId, participant, isPSTN, true, false, false);
    }

        // 4.  - Sync
    @Tag(name = "10. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playFileSourceToTarget")
    public ResponseEntity<String> playFileSourceToTarget(@RequestParam String callConnectionId, @RequestParam String participant, @RequestParam boolean isPSTN) {
            // Use the correct argument list for playFile: (callConnectionId, target, isPSTN, async, isPlayToAll)
            return playFile(callConnectionId, participant, isPSTN, false, false, false);
        }
       
    @Tag(name = "10. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playFileSourceToAllAsync")
    public ResponseEntity<String> playFileSourceToAllAsync(@RequestParam String callConnectionId) {
        return playFile(callConnectionId, null, false, true, false, true);
        }

    // 8. All - Sync
    @Tag(name = "10. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playFileSourceToAll")
    public ResponseEntity<String> playFileSourceToAll(@RequestParam String callConnectionId) {
        return playFile(callConnectionId, null, false, false, false, true);
    }

    // 9. Barge-In - Async
    @Tag(name = "10. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playFileSourceBargeInAsync")
    public ResponseEntity<String> playFileSourceBargeInAsync(@RequestParam String callConnectionId) {
        return playFile(callConnectionId, null, false, true, true, true);
    }
    
    @Tag(name = "11. Start Recognition APIs", description = "Start Recognition APIs")
    @PostMapping("/recognizeDTMFAsync")
    public ResponseEntity<String> recognizeDTMFAsync(@RequestParam String callConnectionId, @RequestParam String participant, @RequestParam boolean isPSTN) {
        return startDtmfRecognition(callConnectionId, participant, isPSTN, true);
    }

    @Tag(name = "11. Start Recognition APIs", description = "Start Recognition APIs")
    @PostMapping("/recognizeDTMF")
    public ResponseEntity<String> recognizeDTMF(@RequestParam String callConnectionId, @RequestParam String participant, @RequestParam boolean isPSTN) {
        return startDtmfRecognition(callConnectionId, participant, isPSTN, false);
    }

    // Async Equivalent: /sendDTMFTonesAsync (C#)
    @Tag(name = "12. Send or Start DTMF APIs", description = "Send or Start DTMF APIs")
    @PostMapping("/sendDTMFTonesAsync")
    public ResponseEntity<String> sendDTMFTonesAsync(@RequestParam String callConnectionId, @RequestParam String participant, @RequestParam boolean isPSTN) {
        try {
            CommunicationIdentifier target;
            if(isPSTN) {
                target = new PhoneNumberIdentifier(participant);
            } else {
                target = new CommunicationUserIdentifier(participant);
            }
            List<DtmfTone> tones = Arrays.asList(DtmfTone.ZERO, DtmfTone.ONE);
            CallMedia callMediaService = getCallMedia(callConnectionId);
            callMediaService.sendDtmfTones(tones, target); // .block() internally

            log.info("Async DTMF tones sent to {}", targetAcsUserId);
            return ResponseEntity.ok("DTMF tones sent (async simulation).");
        } catch (Exception e) {
            log.error("Error sending DTMF tones to {}: {}", targetAcsUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error sending DTMF tones");
        }
    }

    // Sync Equivalent: /sendDTMFTones (C#)
    @Tag(name = "12. Send or Start DTMF APIs", description = "Send or Start DTMF APIs")
    @PostMapping("/sendDTMFTones")
    public ResponseEntity<String> sendDTMFTones(@RequestParam String callConnectionId, @RequestParam String participant, @RequestParam boolean isPSTN) {
        try {
            CommunicationIdentifier target;
            if(isPSTN) {
                target = new PhoneNumberIdentifier(participant);
            } else {
                target = new CommunicationUserIdentifier(participant);
            }
            List<DtmfTone> tones = Arrays.asList(DtmfTone.ZERO, DtmfTone.ONE);
            CallMedia callMediaService = getCallMedia(callConnectionId);
            callMediaService.sendDtmfTones(tones, target);

            log.info("DTMF tones sent to {}", targetAcsUserId);
            return ResponseEntity.ok("DTMF tones sent.");
        } catch (Exception e) {
            log.error("Error sending DTMF tones to {}: {}", targetAcsUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error sending DTMF tones");
        }
    }

    // Async Equivalent: /startContinuousDTMFTonesAsync (C#)
    @Tag(name = "12. Send or Start DTMF APIs", description = "Send or Start DTMF APIs")
    @PostMapping("/startContinuousDTMFTonesAsync")
    public ResponseEntity<String> startContinuousDTMFTonesAsync(@RequestParam String callConnectionId, @RequestParam String participant, @RequestParam boolean isPSTN) {
        try {
            CommunicationIdentifier target;
            if(isPSTN) {
                target = new PhoneNumberIdentifier(participant);
            } else {
                target = new CommunicationUserIdentifier(participant);
            }
            CallMedia callMediaService = getCallMedia(callConnectionId);
            callMediaService.startContinuousDtmfRecognition(target); // .block() internally

            log.info("Async continuous DTMF started for {}", participant);
            return ResponseEntity.ok("Started continuous DTMF recognition (async simulation).");
        } catch (Exception e) {
            log.error("Error starting continuous DTMF recognition for {}: {}", participant, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error starting continuous DTMF recognition");
        }
    }

    // Sync Equivalent: /startContinuousDTMFTones (C#)
    @Tag(name = "12. Send or Start DTMF APIs", description = "Send or Start DTMF APIs")
    @PostMapping("/startContinuousDTMFTones")
    public ResponseEntity<String> startContinuousDTMFTones(@RequestParam String callConnectionId, @RequestParam String participant, @RequestParam boolean isPSTN) {
        try {
            CommunicationIdentifier target;
            if(isPSTN) {
                target = new PhoneNumberIdentifier(participant);
            } else {
                target = new CommunicationUserIdentifier(participant);
            }
            CallMedia callMediaService = getCallMedia(callConnectionId);
            callMediaService.startContinuousDtmfRecognition(target);

            log.info("Started continuous DTMF for {}", participant);
            return ResponseEntity.ok("Started continuous DTMF recognition.");
        } catch (Exception e) {
            log.error("Error starting continuous DTMF recognition for {}: {}", participant, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error starting continuous DTMF recognition");
        }
    }

    // Async Equivalent: /stopContinuousDTMFTonesAsync (C#)
    @Tag(name = "12. Send or Start DTMF APIs", description = "Send or Start DTMF APIs")
    @PostMapping("/stopContinuousDTMFTonesAsync")
    public ResponseEntity<String> stopContinuousDTMFTonesAsync(@RequestParam String callConnectionId, @RequestParam String participant, @RequestParam boolean isPSTN) {
        try {
            CommunicationIdentifier target;
            if(isPSTN) {
                target = new PhoneNumberIdentifier(participant);
            } else {
                target = new CommunicationUserIdentifier(participant);
            }
            CallMedia callMediaService = getCallMedia(callConnectionId);
            callMediaService.stopContinuousDtmfRecognition(target); // .block() internally

            log.info("Async stop continuous DTMF for {}", participant);
            return ResponseEntity.ok("Stopped continuous DTMF recognition (async simulation).");
        } catch (Exception e) {
            log.error("Error stopping continuous DTMF recognition for {}: {}", participant, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error stopping continuous DTMF recognition");
        }
    }

    // Sync Equivalent: /stopContinuousDTMFTones (C#)
    @Tag(name = "12. Send or Start DTMF APIs", description = "Send or Start DTMF APIs")
    @PostMapping("/stopContinuousDTMFTones")
    public ResponseEntity<String> stopContinuousDTMFTones(@RequestParam String callConnectionId, @RequestParam String participant, @RequestParam boolean isPSTN) {
        try {
            CommunicationIdentifier target;
            if(isPSTN) {
                target = new PhoneNumberIdentifier(participant);
            } else {
                target = new CommunicationUserIdentifier(participant);
            }
            CallMedia callMediaService = getCallMedia(callConnectionId);
            callMediaService.stopContinuousDtmfRecognition(target);

            log.info("Stopped continuous DTMF for {}", participant);
            return ResponseEntity.ok("Stopped continuous DTMF recognition.");
        } catch (Exception e) {
            log.error("Error stopping continuous DTMF recognition for {}: {}", participant, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error stopping continuous DTMF recognition");
        }
    }

    @Tag(name = "13. Recording APIs", description = "Recording APIs")
    @PostMapping("/startRecordingAsync")
    public ResponseEntity<String> startRecordingAsync(@RequestParam String callConnectionId, @RequestParam boolean isAudioVideo,
                                                      @RequestParam String recordingFormat, @RequestParam boolean isMixed,
                                                      @RequestParam boolean isRecordingWithCallConnectionId, @RequestParam boolean isPauseOnStart) {
        return startRecording(callConnectionId, isAudioVideo, recordingFormat, isMixed, isRecordingWithCallConnectionId, isPauseOnStart, true);
    }

    @Tag(name = "13. Recording APIs", description = "Recording APIs")
    @PostMapping("/startRecording")
    public ResponseEntity<String> startRecording(@RequestParam String callConnectionId, @RequestParam boolean isAudioVideo,
                                                      @RequestParam String recordingFormat, @RequestParam boolean isMixed,
                                                      @RequestParam boolean isRecordingWithCallConnectionId, @RequestParam boolean isPauseOnStart) {
        return startRecording(callConnectionId, isAudioVideo, recordingFormat, isMixed, isRecordingWithCallConnectionId, isPauseOnStart, false);
    }

    @Tag(name = "13. Recording APIs", description = "Recording APIs")
    @PostMapping("/pauseRecordingAsync")
    public ResponseEntity<String> pauseRecordingAsync(@RequestParam String recordingId) {
        try {
            client.getCallRecording().pauseWithResponse(recordingId, null);
            log.info("Paused recording for {}", recordingId);
            return ResponseEntity.ok("Recording paused successfully.");
        } catch (Exception e) {
            log.error("Error pausing recording for {}: {}", recordingId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error pausing recording");
        }
    }

    @Tag(name = "13. Recording APIs", description = "Recording APIs")
    @PostMapping("/pauseRecording")
    public ResponseEntity<String> pauseRecording(@RequestParam String recordingId   ) {
        try {
            client.getCallRecording().pause(recordingId);
            log.info("Paused recording for {}", recordingId);
            return ResponseEntity.ok("Recording paused successfully.");
        } catch (Exception e) {
            log.error("Error pausing recording for {}: {}", recordingId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error pausing recording");
        }
    }

    @Tag(name = "13. Recording APIs", description = "Recording APIs")
    @PostMapping("/resumeRecordingAsync")
    public ResponseEntity<String> resumeRecordingAsync(@RequestParam String recordingId) {
        try {
            client.getCallRecording().resumeWithResponse(recordingId, null);
            log.info("Resumed recording for {}", recordingId);
            return ResponseEntity.ok("Recording resumed successfully.");
        } catch (Exception e) {
            log.error("Error resuming recording for {}: {}", recordingId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error resuming recording");
        }
    }

    @Tag(name = "13. Recording APIs", description = "Recording APIs")
    @PostMapping("/resumeRecording")
    public ResponseEntity<String> resumeRecording(@RequestParam String recordingId) {
        try {
            client.getCallRecording().resume(recordingId);
            log.info("Resumed recording for {}", recordingId);
            return ResponseEntity.ok("Recording resumed successfully.");
        } catch (Exception e) {
            log.error("Error resuming recording for {}: {}", recordingId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error resuming recording");
        }
    }

    @Tag(name = "13. Recording APIs", description = "Recording APIs")
    @PostMapping("/stopRecordingAsync")
    public ResponseEntity<String> stopRecordingAsync(@RequestParam String recordingId) {
        try {
            client.getCallRecording().stopWithResponse(recordingId, null);
            log.info("Stopped recording for {}", recordingId);
            return ResponseEntity.ok("Recording stopped successfully.");
        } catch (Exception e) {
            log.error("Error stopping recording for {}: {}", recordingId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error stopping recording");
        }
    }

    @Tag(name = "13. Recording APIs", description = "Recording APIs")
    @PostMapping("/stopRecording")
    public ResponseEntity<String> stopRecording(@RequestParam String recordingId) {
        try {
            client.getCallRecording().stop(recordingId);
            log.info("Stopped recording for {}", recordingId);
            return ResponseEntity.ok("Recording stopped successfully.");
        } catch (Exception e) {
            log.error("Error stopping recording for {}: {}", recordingId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error stopping recording");
        }
    }

    @Tag(name = "13. Recording APIs", description = "Recording APIs")
    @GetMapping("/downloadRecording")
    public ResponseEntity<String> downloadRecording() {
        try {
            if (recordingLocation != null && !recordingLocation.isEmpty()) {
                String downloadsPath = System.getProperty("user.home") + "/Downloads";
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String fileName = "Recording_" + timestamp + "." + recordingFileFormat;
                String filePath = downloadsPath + "/" + fileName;

                try (OutputStream outputStream = new FileOutputStream(filePath)) {
                    client.getCallRecording().downloadTo(recordingLocation, outputStream);
                    log.info("Recording downloaded to: {}", filePath);
                } catch (IOException e) {
                    log.error("Error while downloading recording", e);
                }
            } else {
                log.error("Recording is not available");
            }
            return ResponseEntity.ok("Recording downloaded successfully.");
        } catch (Exception e) {
            log.error("Error while downloading recording", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error downloading recording");
        }
    }

    @Tag(name = "14. Cancel All Media Operation APIs", description = "Cancel All Media Operation APIs")
    @PostMapping("/cancelAllMediaOperationAsync")
    public ResponseEntity<String> cancelAllMediaOperationAsync(@RequestParam String callConnectionId) {
        try {
            CallMedia callMedia = getCallMedia(callConnectionId);
            // Simulate async operation in Java (can use CompletableFuture or similar if truly async)
            CompletableFuture.runAsync(() -> {
                try {
                    callMedia.cancelAllMediaOperationsWithResponse(Context.NONE); // If reactive
                } catch (Exception e) {
                    log.error("Failed to cancel media operations asynchronously", e);
                }
            });

            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            log.error("Error during async cancel", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Tag(name = "14. Cancel All Media Operation APIs", description = "Cancel All Media Operation APIs")
    @PostMapping("/cancelAllMediaOperation")
    public ResponseEntity<String> cancelAllMediaOperation(@RequestParam String callConnectionId) {
        try {
            CallMedia callMedia = getCallMedia(callConnectionId);
            callMedia.cancelAllMediaOperations(); // synchronous method
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            log.error("Error during cancel", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Tag(name = "15. Media Streaming APIs", description = "Media Streaming APIs")
    @PostMapping("/createCallWithMediaStreamingAsync")
    public ResponseEntity<String> createCallWithMediaStreamingAsync(@RequestParam String targetParticipant, @RequestParam boolean isPSTN) {
        try {
            CallInvite callInvite;
            if(isPSTN) {
                PhoneNumberIdentifier target = new PhoneNumberIdentifier(targetParticipant);
                callInvite = new CallInvite(target, new PhoneNumberIdentifier(acsPhoneNumber));
            } else {
                CommunicationUserIdentifier target = new CommunicationUserIdentifier(targetParticipant);
                callInvite = new CallInvite(target);
            }
            
            URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");
            String websocketUri = websocketUriHost.replace("https", "wss") + "/ws";

            CreateCallOptions createCallOptions = new CreateCallOptions(callInvite, callbackUri.toString());
            CallIntelligenceOptions callIntelligenceOptions = new CallIntelligenceOptions();
                
            MediaStreamingOptions mediaStreamingOptions = new MediaStreamingOptions(MediaStreamingAudioChannel.UNMIXED, StreamingTransport.WEBSOCKET);
            createCallOptions.setCallIntelligenceOptions(callIntelligenceOptions);
            createCallOptions.setMediaStreamingOptions(mediaStreamingOptions);

            Response<CreateCallResult> result = client.createCallWithResponse(createCallOptions, Context.NONE);
            callConnectionId = result.getValue().getCallConnectionProperties().getCallConnectionId();
            log.info("Created async call with connection id: " + callConnectionId);
            return ResponseEntity.ok("Created async call with connection id: " + callConnectionId);
        } catch (Exception e) {
            log.error("Error creating call : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create call.");
        }
    }

    @Tag(name = "15. Media Streaming APIs", description = "Media Streaming APIs")
    @PostMapping("/startMediaStreamingAsync")
    public ResponseEntity<String> startMediaStreamingAsync(@RequestParam String callConnectionId) {
        try {
            StartMediaStreamingOptions mediaStreamingOptions = new StartMediaStreamingOptions();

            CallMedia callMedia = getCallMedia(callConnectionId);
            callMedia.startMediaStreamingWithResponse(mediaStreamingOptions, Context.NONE);

            log.info("Started media streaming asynchronously for call: {}", callConnectionId);
            return ResponseEntity.ok("Media streaming started successfully.");
        } catch (Exception e) {
            log.error("Error starting media streaming asynchronously: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to start media streaming.");
        }
    }

    @Tag(name = "15. Media Streaming APIs", description = "Media Streaming APIs")
    @PostMapping("/startMediaStreaming")
    public ResponseEntity<String> startMediaStreaming(@RequestParam String callConnectionId) {
        try {
            CallMedia callMedia = getCallMedia(callConnectionId);
            callMedia.startMediaStreaming();

            log.info("Started media streaming asynchronously for call: {}", callConnectionId);
            return ResponseEntity.ok("Media streaming started successfully.");
        } catch (Exception e) {
            log.error("Error starting media streaming asynchronously: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to start media streaming.");
        }
    }
        
    @Tag(name = "15. Media Streaming APIs", description = "Media Streaming APIs")
    @PostMapping("/stopMediaStreamingAsync")
    public ResponseEntity<String> stopMediaStreamingAsync(@RequestParam String callConnectionId) {
        try {
            StopMediaStreamingOptions stopOptions = new StopMediaStreamingOptions();
            CallMedia callMedia = getCallMedia(callConnectionId);
            callMedia.stopMediaStreamingWithResponse(stopOptions, Context.NONE);

            log.info("Stopped media streaming asynchronously for call: {}", callConnectionId);
            return ResponseEntity.ok("Media streaming stopped successfully.");
        } catch (Exception e) {
            log.error("Error stopping media streaming asynchronously: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to stop media streaming.");
        }
    }

    @Tag(name = "15. Media Streaming APIs", description = "Media Streaming APIs")
    @PostMapping("/stopMediaStreaming")
    public ResponseEntity<String> stopMediaStreaming(@RequestParam String callConnectionId) {
        try {
            CallMedia callMedia = getCallMedia(callConnectionId);
            callMedia.stopMediaStreaming();

            log.info("Stopped media streaming for call: {}", callConnectionId);
            return ResponseEntity.ok("Media streaming stopped successfully.");
        } catch (Exception e) {
            log.error("Error stopping media streaming: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to stop media streaming.");
        }
    }
    
    // 🔄 Shared Method
    private CallMedia getCallMedia(String callConnectionId) {
        if (callConnectionId == null || callConnectionId.isEmpty()) {
            throw new IllegalArgumentException("Call connection id is empty");
        }
        return client.getCallConnection(callConnectionId).getCallMedia();
    }

    private CallConnection getConnection(String callConnectionId) {
        if (callConnectionId == null || callConnectionId.isEmpty()) {
            throw new IllegalArgumentException("Call connection id is empty");
        }
        return client.getCallConnection(callConnectionId);
    }

    private CallConnectionProperties getCallConnectionProperties(String callConnectionId) {
        if (callConnectionId == null || callConnectionId.isEmpty()) {
            throw new IllegalArgumentException("Call connection id is empty");
        }
        return client.getCallConnection(callConnectionId).getCallProperties();
    }

    private static final String FILE_SOURCE_URI = "https://sample-videos.com/audio/mp3/crowd-cheering.mp3"; // replace with actual URI

    private ResponseEntity<String> playFile(String callConnectionId, String target, boolean isPSTN, boolean async, boolean bargeIn, boolean isPlayToAll) {
        try {
            FileSource fileSource = new FileSource().setUrl(FILE_SOURCE_URI);
            String context = bargeIn ? "playBargeInContext" : "playContext";
            CallMedia mediaService = getCallMedia(callConnectionId);
    
            if (isPlayToAll) {
                PlayToAllOptions options = new PlayToAllOptions(fileSource);
                options.setOperationContext(context);
                options.setInterruptCallMediaOperation(bargeIn);
    
                if (async) {
                    mediaService.playToAll(Collections.singletonList(fileSource));
                } else {
                    mediaService.playToAllWithResponse(options, Context.NONE);
                }
            } else {
                List<CommunicationIdentifier> playTo;
                 if(isPSTN){
                    playTo = List.of(new PhoneNumberIdentifier(target));
                 } else {
                    playTo = List.of(new CommunicationUserIdentifier(target));
                 }
    
                PlayOptions options = new PlayOptions(fileSource, playTo);
                options.setOperationContext(context);
    
                if (async) {
                    mediaService.play(fileSource, playTo);
                } else {
                    mediaService.playWithResponse(options, Context.NONE);
                }
            }
            log.info("Successfully played file source to target: {}", target);
            return ResponseEntity.ok("Successfully played file source.");
        } catch (Exception ex) {
            log.error("Error playing file source: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<String> startDtmfRecognition(String callConnectionId, String target, boolean isPSTN, boolean async) {
        try {
            CallMedia callMedia = getCallMedia(callConnectionId);
            TextSource prompt = new TextSource()
                .setText("Hi, this is recognize test. Please provide input. Thanks!")
                .setVoiceName("en-US-NancyNeural")
                .setSourceLocale("en-US")
                .setVoiceKind(VoiceKind.MALE); // Optional: if enum NEURAL is available

            CommunicationIdentifier participant;
            if (isPSTN) {
                participant = new PhoneNumberIdentifier(target);
            } else {
                participant = new CommunicationUserIdentifier(target);
            }
    
            CallMediaRecognizeDtmfOptions options = new CallMediaRecognizeDtmfOptions(participant, 4)
                .setInterruptPrompt(false)
                .setInterToneTimeout(Duration.ofSeconds(5))
                .setInitialSilenceTimeout(Duration.ofSeconds(15))
                .setPlayPrompt(prompt)
                .setOperationContext("DtmfContext");
    
            if (async) {
                callMedia.startRecognizingWithResponse(options, Context.NONE); // async version
            } else {
                callMedia.startRecognizing(options); // sync version
            }
            log.info("Started DTMF recognition for target: {}", target);
            return ResponseEntity.ok("Started DTMF recognition.");
        } catch (Exception ex) {
            log.error("Error starting DTMF recognition: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private ResponseEntity<String> startRecording(String callConnectionId, boolean isAudioVideo,
                                                      String recordingFormat, boolean isMixed,
                                                      boolean isRecordingWithCallConnectionId, boolean isPauseOnStart, boolean async) {
        try {
                CallConnectionProperties properties = getCallConnectionProperties(callConnectionId);
                CallLocator locator = new ServerCallLocator(properties.getServerCallId());
                String eventCallbackUri = callbackUriHost + "/api/callbacks";
                if(isRecordingWithCallConnectionId){
                StartRecordingOptions options = new StartRecordingOptions(callConnectionId);

                options.setRecordingContent(isAudioVideo?RecordingContent.AUDIO_VIDEO:RecordingContent.AUDIO);
                if(recordingFormat.equalsIgnoreCase("mp3")){
                    options.setRecordingFormat(RecordingFormat.MP3);
                } else if(recordingFormat.equalsIgnoreCase("wav")){
                    options.setRecordingFormat(RecordingFormat.WAV);
                } else {
                    options.setRecordingFormat(RecordingFormat.MP4);
                }
                options.setRecordingFormat(RecordingFormat.MP4);
                if(isMixed){
                    options.setRecordingChannel(RecordingChannel.MIXED);
                } else {
                    options.setRecordingChannel(RecordingChannel.UNMIXED);
                }
                options.setRecordingStateCallbackUrl(eventCallbackUri);
                options.setPauseOnStart(isPauseOnStart);
                recordingFileFormat = recordingFormat.toLowerCase();

                if(async){
                    Response<RecordingStateResult> response = client.getCallRecording()
                .startWithResponse(options, Context.NONE);
                recordingId = response.getValue().getRecordingId();
                } else {
                    recordingId = client.getCallRecording().start(options).getRecordingId();
                }
                log.info("Recording started. RecordingId: {}", recordingId);
                return ResponseEntity.ok("Recording started successfully.");
            } else {
                StartRecordingOptions options = new StartRecordingOptions(locator);

                options.setRecordingContent(isAudioVideo?RecordingContent.AUDIO_VIDEO:RecordingContent.AUDIO);
                if(recordingFormat.equalsIgnoreCase("mp3")){
                    options.setRecordingFormat(RecordingFormat.MP3);
                } else if(recordingFormat.equalsIgnoreCase("wav")){
                    options.setRecordingFormat(RecordingFormat.WAV);
                } else {
                    options.setRecordingFormat(RecordingFormat.MP4);
                }
                options.setRecordingFormat(RecordingFormat.MP4);
                if(isMixed){
                    options.setRecordingChannel(RecordingChannel.MIXED);
                } else {
                    options.setRecordingChannel(RecordingChannel.UNMIXED);
                }
                options.setRecordingStateCallbackUrl(eventCallbackUri);
                options.setPauseOnStart(isPauseOnStart);
                recordingFileFormat = recordingFormat.toLowerCase();

                if(async){
                    Response<RecordingStateResult> response = client.getCallRecording()
                .startWithResponse(options, Context.NONE);
                recordingId = response.getValue().getRecordingId();
                } else {
                    recordingId = client.getCallRecording().start(options).getRecordingId();
                }
                log.info("Recording started. RecordingId: {}", recordingId);
                return ResponseEntity.ok("Recording started successfully.");
            }
        } catch (Exception e) {
            log.error("Error starting recording for {}: {}", targetAcsUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error starting recording");
        }
    }
    
    private CallAutomationClient initClient() {
        try {
            var client = new CallAutomationClientBuilder()
                    .connectionString(appConfig.getConnectionString())
                    .buildClient();
            log.info("Call Automation Client initialized successfully.");
            return client;
        } catch (NullPointerException e) {
            log.error("Please verify if Application config is properly set up");
            return null;
        } catch (Exception e) {
            log.error("Error occurred when initializing Call Automation Client: {} {}", e.getMessage(), e.getCause());
            return null;
        }
    }
}
