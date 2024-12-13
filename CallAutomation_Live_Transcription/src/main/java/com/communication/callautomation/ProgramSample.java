package com.communication.callautomation;

import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.CallConnection;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.SystemEventNames;
import com.azure.messaging.eventgrid.systemevents.AcsRecordingFileStatusUpdatedEventData;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationEventData;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationResponse;

import lombok.val;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@Slf4j
public class ProgramSample {
    private final AppConfig appConfig;
    private final CallAutomationClient client;
    Set<String> recognizeFails = new HashSet<>() {
    };

    private final String helpIVRPrompt = "Welcome to the Contoso Utilities. To access your account, we need to verify your identity. Please enter your date of birth in the format DDMMYYYY using the keypad on your phone. Once we’ve validated your identity we will connect you to the next available agent. Please note this call will be recorded!";
    private final String addAgentPrompt = "Thank you for verifying your identity. We are now connecting you to the next available agent. Please hold the line and we will be with you shortly. Thank you for your patience.";
    private final String incorrectDobPrompt = "Sorry, we were unable to verify your identity based on the date of birth you entered. Please try again. Remember to enter your date of birth in the format DDMMYYYY using the keypad on your phone. Once you've entered your date of birth, press the pound key. Thank you!";
    private final String addParticipantFailurePrompt = "We're sorry, we were unable to connect you to an agent at this time, we will get the next available agent to call you back as soon as possible.";
    private final String goodbyePrompt = "Thank you for calling Contoso Utilities. We hope we were able to assist you today. Goodbye";
    private final String timeoutSilencePrompt = "I’m sorry, I didn’t receive any input. Please type your date of birth in the format of DDMMYYYY.";
    private final String goodbyeContext = "Goodbye";
    private final String addAgentContext = "AddAgent";
    private final String incorrectDobContext = "IncorrectDob";
    private final String addParticipantFailureContext = "FailedToAddParticipant";
    private static final String INCOMING_CALL_CONTEXT = "incomingCallContext";
    private final String DobRegex = "^(0[1-9]|[12][0-9]|3[01])(0[1-9]|1[012])[12][0-9]{3}$";
    Boolean isTranscriptionActive = false;
    int maxTimeout = 2;

    private String recordingId = "";
    private String recordingLocation = "";

    private CallConnection answerCallConnection;

    public ProgramSample(final AppConfig appConfig) {
        this.appConfig = appConfig;
        client = initClient();
    }

    @GetMapping(path = "/")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok().body("Hello! ACS CallAutomation Live Transcription Sample!");
    }

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

    @PostMapping(path = "/api/callback/{contextId}")
    public ResponseEntity<String> callbackEvents(@RequestBody final String reqBody,
            @PathVariable final String contextId,
            @RequestParam final String callerId) {
        List<CallAutomationEventBase> events = CallAutomationEventParser.parseEvents(reqBody);
        for (CallAutomationEventBase event : events) {
            String callConnectionId = event.getCallConnectionId();

            log.info("Received call correlationId: {}, callConnectionID: {}, serverCallId: {}",
                    event.getCorrelationId(),
                    event.getCallConnectionId(),
                    event.getServerCallId());
            if (event instanceof CallConnected) {
                /* Start the recording */
                CallLocator callLocator = new ServerCallLocator(event.getServerCallId());
                RecordingStateResult recordingResult = client.getCallRecording()
                        .start(new StartRecordingOptions(callLocator));
                recordingId = recordingResult.getRecordingId();

                var properties = client.getCallConnection(callConnectionId)
                        .getCallProperties();
                log.info("Transcription subscription id---> "
                        + properties.getTranscriptionSubscription().getId());
                log.info("Transcription State---> "
                        + properties.getTranscriptionSubscription().getState());

                /* Start the Transcription */
                initiateTranscription(callConnectionId);

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {

                }

                pauseOrStopTranscriptionAndRecording(callConnectionId, false, recordingId);

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {

                }

                handleRecognizeRequest(helpIVRPrompt, callConnectionId, callerId, "hellocontext");
            } else if (event instanceof PlayCompleted) {
                log.info("Received Play Completed event");
                if (!event.getOperationContext().isEmpty() && event.getOperationContext().equals(addAgentContext)) {
                    System.out.println("Add Agent: " + appConfig.getAgentPhoneNumber());
                    // Add Agent
                    CallInvite callInvite = new CallInvite(new PhoneNumberIdentifier(appConfig.getAgentPhoneNumber()),
                            new PhoneNumberIdentifier(appConfig.getAcsPhoneNumber()));
                    val addParticipantOptions = new AddParticipantOptions(callInvite)
                            .setOperationContext(addAgentContext);
                    Response<AddParticipantResult> addParticipantResult = answerCallConnection
                            .addParticipantWithResponse(addParticipantOptions, Context.NONE);
                    log.info("Adding agent to the call: {}", addParticipantResult.getValue().getInvitationId());
                }
                if (!event.getOperationContext().isEmpty() && (event.getOperationContext().equals(goodbyeContext) ||
                        event.getOperationContext().equals(addParticipantFailureContext))) {
                    pauseOrStopTranscriptionAndRecording(callConnectionId, true, recordingId);
                    hangUp(callConnectionId);
                }
            } else if (event instanceof RecognizeCompleted) {
                log.info("Recognize Completed event received");
                RecognizeCompleted recognizeEvent = (RecognizeCompleted) event;
                RecognizeResult recognizeResult = recognizeEvent.getRecognizeResult().get();
                if (recognizeResult instanceof DtmfResult) {
                    // Take action on collect tones
                    DtmfResult dtmfResult = (DtmfResult) recognizeResult;
                    String tones = dtmfResult.convertToString();
                    Pattern pattern = Pattern.compile(DobRegex);
                    Matcher match = pattern.matcher(tones);
                    if (match.find()) {
                        resumeTranscriptionAndRecording(callConnectionId, recordingId);
                        handlePlayTo(addAgentPrompt, addAgentContext, callConnectionId, callerId);
                    } else {
                        handleRecognizeRequest(incorrectDobPrompt, callConnectionId, callerId, incorrectDobContext);
                    }
                }
            } else if (event instanceof AddParticipantFailed) {
                AddParticipantFailed addParticipantFailedEvent = (AddParticipantFailed) event;
                log.error("Received Add Participants Failed Message: {}, Subcode: {}",
                        addParticipantFailedEvent.getResultInformation().getMessage(),
                        addParticipantFailedEvent.getResultInformation().getSubCode());
                handlePlayTo(addParticipantFailurePrompt, addParticipantFailureContext, callConnectionId, callerId);
            } else if (event instanceof RecognizeFailed) {
                RecognizeFailed recognizeFailedEvent = (RecognizeFailed) event;
                log.error("Received Recognized Failed Message: {}, Subcode: {}",
                        recognizeFailedEvent.getResultInformation().getMessage(),
                        recognizeFailedEvent.getResultInformation().getSubCode());
                if (recognizeFails.contains(callConnectionId)) {
                    log.error("No input was recognized, hanging up call: {}", callConnectionId);
                    handlePlayTo(goodbyePrompt, goodbyeContext, callConnectionId, callerId);
                } else {
                    recognizeFails.add(callConnectionId);
                    if ((recognizeFailedEvent.getResultInformation().getSubCode().toString())
                            .equals(ReasonCode.Recognize.INITIAL_SILENCE_TIMEOUT.toString())) {
                        log.info("Retrying recognize...");
                        handleRecognizeRequest(timeoutSilencePrompt, callConnectionId, callerId, "retryContext");
                    }
                }
            } else if (event instanceof PlayFailed) {
                PlayFailed playFailedEvent = (PlayFailed) event;
                log.info("Received Play Failed event Message: {}, Subcode: {}",
                        playFailedEvent.getResultInformation().getMessage(),
                        playFailedEvent.getResultInformation().getSubCode());
                pauseOrStopTranscriptionAndRecording(callConnectionId, true, recordingId);
                recognizeFails.remove(callConnectionId);
                hangUp(callConnectionId);
            } else if (event instanceof CallDisconnected) {
                log.info("Received Call Disconnected event for Call Connection ID: {}", callConnectionId);
            } else if (event instanceof TranscriptionStarted) {
                log.info("Received transcription started event");
                TranscriptionStarted acsEvent = (TranscriptionStarted) event;
                log.info("Transcription Status --> "
                        + acsEvent.getTranscriptionUpdateResult().getTranscriptionStatus());
                log.info("Transcription Status Details --> "
                        + acsEvent.getTranscriptionUpdateResult()
                                .getTranscriptionStatusDetails());
                log.info("Operation Context --> " + acsEvent.getOperationContext());
            } else if (event instanceof TranscriptionStopped) {
                isTranscriptionActive = false;
                log.info("Received transcription stopped event");
                TranscriptionStopped acsEvent = (TranscriptionStopped) event;
                log.info("Transcription Status --> "
                        + acsEvent.getTranscriptionUpdateResult().getTranscriptionStatus());
                log.info("Transcription Status Details --> "
                        + acsEvent.getTranscriptionUpdateResult()
                                .getTranscriptionStatusDetails());
                log.info("Operation Context --> " + acsEvent.getOperationContext());
            } else if (event instanceof TranscriptionUpdated) {
                log.info("Transcription Updated....");
                TranscriptionUpdated acsEvent = (TranscriptionUpdated) event;
                log.info("Transcription Status --> "
                        + acsEvent.getTranscriptionUpdateResult().getTranscriptionStatus());
                log.info("Transcription Status Details --> "
                        + acsEvent.getTranscriptionUpdateResult()
                                .getTranscriptionStatusDetails());
                log.info("Operation Context --> " + acsEvent.getOperationContext());

            } else if (event instanceof TranscriptionFailed) {
                TranscriptionFailed playFailedEvent = (TranscriptionFailed) event;
                log.info("Received transcription Failed event Message: {}, Subcode: {}",
                        playFailedEvent.getResultInformation().getMessage(),
                        playFailedEvent.getResultInformation().getSubCode());
            }
        }

        return ResponseEntity.ok().body("");
    }

    @PostMapping("/api/recordingFileStatus")
    public ResponseEntity<SubscriptionValidationResponse> handleRecordingFileStatus(@RequestBody String reqBody) {
        List<EventGridEvent> events = EventGridEvent.fromString(reqBody);
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
                log.info("The recording location is : {}", recordingLocation);

            } else {
                log.debug("Unhandled event.");
            }
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/download")
    public ResponseEntity<Void> callRecordingDownload() {
        try {
            client.getCallRecording().downloadTo(recordingLocation, new FileOutputStream("testfile.wav"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok().build();
    }

    private void handleIncomingCall(final BinaryData eventData) {
        JSONObject data = new JSONObject(eventData.toString());
        String callbackUri;
        AnswerCallOptions options;
        String cognitiveServicesUrl;
        String websocketUrl;

        try {
            callbackUri = String.format("%s/%s?callerId=%s",
                    appConfig.getCallBackUri(),
                    UUID.randomUUID(),
                    data.getJSONObject("from").getString("rawId"));
                    String callbackHostUri = appConfig.getCallBackUri(); // Example: https://3tkj9v9n-8080.inc1.devtunnels.ms/api/callback

                    // Extract the base host and port by removing the path after the host
                    String baseUri = callbackHostUri.replaceFirst("(https://[^/]+).*", "$1");
                    
                    // Replace "https://" with "wss://" for WebSocket protocol
                    websocketUrl = baseUri.replaceFirst("^https://", "wss://") + "/ws";
                    
                    System.out.println("WebSocket URL: " + websocketUrl);
            cognitiveServicesUrl = new URI(appConfig.getCognitiveServicesUrl()).toString();
            CallIntelligenceOptions callIntelligenceOptions = new CallIntelligenceOptions()
                    .setCognitiveServicesEndpoint(appConfig.getCognitiveServicesUrl());
            TranscriptionOptions transcriptionOptions = new TranscriptionOptions(
                    websocketUrl,
                    TranscriptionTransport.WEBSOCKET,
                    appConfig.getLocale(),
                    false);
            options = new AnswerCallOptions(data.getString(INCOMING_CALL_CONTEXT),
                    callbackUri).setCallIntelligenceOptions(callIntelligenceOptions)
                    .setTranscriptionOptions(transcriptionOptions);
            Response<AnswerCallResult> answerCallResponse = client.answerCallWithResponse(options, Context.NONE);

            answerCallConnection = answerCallResponse.getValue().getCallConnection();

            log.info("Incoming call answered. Cognitive Services Url: {}\nCallbackUri: {}\nCallConnectionId: {}",
                    cognitiveServicesUrl,
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

    private void handleRecognizeRequest(
            final String message,
            final String callConnectionId,
            final String callerId,
            final String context) {
        String targetParticipant = callerId.replaceAll("\\s", "+");
        int maxTonesToCollect = 8;
        TextSource textSource = new TextSource()
                .setText(message)
                .setVoiceName("en-US-NancyNeural");
        CallMediaRecognizeDtmfOptions options = new CallMediaRecognizeDtmfOptions(
                CommunicationIdentifier.fromRawId(targetParticipant), maxTonesToCollect)
                .setInterruptPrompt(false)
                .setPlayPrompt(textSource)
                .setOperationContext(context)
                .setInitialSilenceTimeout(Duration.ofSeconds(15));
        client.getCallConnection(callConnectionId)
                .getCallMedia()
                .startRecognizing(options);
    }

    private void handlePlayTo(final String textToPlay,
            final String context,
            final String callConnectionId,
            final String callerId) {

        String tParticipant = callerId.replaceAll("\\s", "+");
        CommunicationIdentifier targetParticipant = CommunicationIdentifier.fromRawId(tParticipant);
        PlaySource playSource = new TextSource()
                .setText(textToPlay)
                .setVoiceName("en-US-NancyNeural");
        PlayOptions playOptions = new PlayOptions(playSource, new ArrayList<>(List.of(targetParticipant)))
                .setOperationContext(context);

        try {
            client.getCallConnection(callConnectionId)
                    .getCallMedia()
                    .playWithResponse(playOptions, Context.NONE);
        } catch (Exception e) {
            log.error("Error occurred when playing media to participant {} {}",
                    e.getMessage(),
                    e.getCause());
        }
    }

    private void initiateTranscription(final String callConnectionId) {
        if (!isTranscriptionActive) {
            StartTranscriptionOptions startTranscriptionOptions = new StartTranscriptionOptions()
                    .setLocale(appConfig.getLocale())
                    .setOperationContext("StartTranscription");
            client.getCallConnection(callConnectionId)
                    .getCallMedia()
                    .startTranscriptionWithResponse(startTranscriptionOptions, Context.NONE);
            isTranscriptionActive = true;
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

    private void pauseOrStopTranscriptionAndRecording(String callConnectionId, Boolean stopRecording,
            String recordingId) {
        if (isTranscriptionActive) {
            client.getCallConnection(callConnectionId).getCallMedia().stopTranscription();
            log.info("Transcription stopped.");
        }

        if (stopRecording) {
            client.getCallRecording().stop(recordingId);
            log.info("Recording stopped. RecordingId: {}", recordingId);
        } else {
            client.getCallRecording().pause(recordingId);
            log.info("Recording paused. RecordingId: {}", recordingId);
        }
    }

    private void resumeTranscriptionAndRecording(String callConnectionId, String recordingId) {
        initiateTranscription(callConnectionId);
        log.info("Transcription reinitiated");

        client.getCallRecording().resume(recordingId);
        log.info("Recording resumed. RecordingId: {}", recordingId);
    }
}
