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
import com.azure.communication.common.MicrosoftTeamsUserIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.systemevents.AcsIncomingCallEventData;
import com.azure.messaging.eventgrid.systemevents.AcsRecordingFileStatusUpdatedEventData;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationEventData;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
// import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@Slf4j
public class ProgramSample {

    private final ConfigurationRequest appConfig;
    private CallAutomationClient client;
    // private final CallAutomationAsyncClient asyncClient;
    // Configuration state variables
    private String acsConnectionString = "";
    private String cognitiveServicesEndpoint = "";
    private String acsPhoneNumber = "";
    private String callbackUriHost = "";

    private String callConnectionId = "";
    private String recordingId = "";
    private String recordingLocation = "";
    private String recordingFileFormat = "";

    private String callerId = "";

    private ConfigurationRequest configuration = new ConfigurationRequest();
    private String confirmLabel = "Confirm";
    private String cancelLabel = "Cancel";
    public ProgramSample(final ConfigurationRequest appConfig) {
        this.appConfig = appConfig;
        client = initClient();
        // asyncClient =  initAsyncClient();
    }

    @Tag(name = "01. Set Configuration", description = "Set Configuration")
    @PostMapping("/api/setConfigurations")
    public ResponseEntity<String> setConfigurations(@RequestBody ConfigurationRequest configurationRequest) {
        // Reset variables
        acsConnectionString = "";
        cognitiveServicesEndpoint = "";
        acsPhoneNumber = "";
        callbackUriHost = "";

        if (configurationRequest != null) {
            configuration.setAcsConnectionString(
                Optional.ofNullable(configurationRequest.getAcsConnectionString())
                    .filter(s -> !s.isEmpty())
                    .orElseThrow(() -> new IllegalArgumentException("AcsConnectionString is required"))
            );

            configuration.setCognitiveServiceEndpoint(
                Optional.ofNullable(configurationRequest.getCognitiveServiceEndpoint())
                    .filter(s -> !s.isEmpty())
                    .orElseThrow(() -> new IllegalArgumentException("CognitiveServiceEndpoint is required"))
            );

            configuration.setAcsPhoneNumber(
                Optional.ofNullable(configurationRequest.getAcsPhoneNumber())
                    .filter(s -> !s.isEmpty())
                    .orElseThrow(() -> new IllegalArgumentException("AcsPhoneNumber is required"))
            );

            configuration.setCallbackUriHost(
                Optional.ofNullable(configurationRequest.getCallbackUriHost())
                    .filter(s -> !s.isEmpty())
                    .orElseThrow(() -> new IllegalArgumentException("CallbackUriHost is required"))
            );
        }

        // Assign to global variables
        acsConnectionString = configuration.getAcsConnectionString();
        cognitiveServicesEndpoint = configuration.getCognitiveServiceEndpoint();
        acsPhoneNumber = configuration.getAcsPhoneNumber();
        callbackUriHost = configuration.getCallbackUriHost();

        client = new CallAutomationClientBuilder()
                     .connectionString(acsConnectionString)
                     .buildClient();

        log.info("Initialized call automation client.");
        return ResponseEntity.ok("Configuration set successfully. Initialized call automation client.");
    }

    @Tag(name = "02. Call Automation Events", description = "CallAutomation Events")
    @PostMapping(path = "/api/callback")
    public ResponseEntity<String> callbackEvents(@RequestBody final String reqBody) {
        List<CallAutomationEventBase> events = CallAutomationEventParser.parseEvents(reqBody);
        for (CallAutomationEventBase event : events) {
            String callConnectionId = event.getCallConnectionId();
            log.info(
                    "Received call event callConnectionID: {}, serverCallId: {}",
                    callConnectionId,
                    event.getServerCallId());

            if (event instanceof CallConnected) {
                // handle CallConnected
            } else if (event instanceof RecognizeCompleted) {
                // handle RecognizeCompleted
            } else if (event instanceof RecognizeFailed) {
                // handle RecognizeFailed
            } else if (event instanceof PlayCompleted || event instanceof PlayFailed) {
                // handle PlayCompleted or PlayFailed
            }
        }
        return ResponseEntity.ok().body("");
    }

    @Tag(name = "02. Call Automation Events", description = "CallAutomation Events")
    @PostMapping("/api/events")
    public ResponseEntity<Object> handleEvents(@RequestBody EventGridEvent[] eventGridEvents) {
        try {
            for (EventGridEvent eventGridEvent : eventGridEvents) {
                log.info("Recording event received: {}", eventGridEvent.getEventType());

                // Try to parse system event data
                Object eventData = eventGridEvent.getData().toObject(Object.class);

                // SubscriptionValidationEventData
                if ("Microsoft.EventGrid.SubscriptionValidationEvent".equals(eventGridEvent.getEventType())
                        && eventData instanceof SubscriptionValidationEventData) {

                    SubscriptionValidationEventData validationData =
                            (SubscriptionValidationEventData) eventData;

                    Map<String, String> responseData = Map.of("validationResponse", validationData.getValidationCode());
                    return ResponseEntity.ok(responseData);
                }

                // AcsIncomingCallEventData
                if ("Microsoft.Communication.IncomingCall".equals(eventGridEvent.getEventType())
                        && eventData instanceof AcsIncomingCallEventData) {

                    AcsIncomingCallEventData incomingCallEventData =
                            (AcsIncomingCallEventData) eventData;

                    callerId = incomingCallEventData.getFromCommunicationIdentifier().getRawId();
                    System.out.println("Caller Id--> " + callerId);

                    URI callbackUri = new URI(callbackUriHost + "/api/callbacks");
                    log.info("Incoming call - correlationId: {}, Callback url: {}",
                            incomingCallEventData.getCorrelationId(), callbackUri);

                    AnswerCallOptions options = new AnswerCallOptions(
                        incomingCallEventData.getIncomingCallContext(),
                        callbackUri.toString()
                    );
                    
                    options.setCallIntelligenceOptions(
                            new CallIntelligenceOptions()
                            .setCognitiveServicesEndpoint(cognitiveServicesEndpoint
                            )
                    );

                    // Call client to answer call
                    AnswerCallResult result = client.answerCallWithResponse(options, Context.NONE).getValue();
                    result.getCallConnection().getCallMedia();
                }

                // AcsRecordingFileStatusUpdatedEventData
                if ("Microsoft.Communication.RecordingFileStatusUpdated".equals(eventGridEvent.getEventType())
                        && eventData instanceof AcsRecordingFileStatusUpdatedEventData) {

                    AcsRecordingFileStatusUpdatedEventData statusUpdated =
                            (AcsRecordingFileStatusUpdatedEventData) eventData;

                    recordingLocation = statusUpdated
                            .getRecordingStorageInfo()
                            .getRecordingChunks()
                            .get(0)
                            .getContentLocation();

                            log.info("The recording location is : {}", recordingLocation);
                }
            }

            return ResponseEntity.ok("Processed");

        } catch (Exception ex) {
            log.error("Error processing events", ex);
            return ResponseEntity.status(500).body("Failed to process events");
        }
    }

    @Tag(name = "04. Inbound Call APIs", description = "APIs for answering incoming calls")
    @PostMapping("/answerCallAsync")
    public ResponseEntity<String> answerCallAsync() {
        try {
            // Construct the callback URI
            String callbackUri = callbackUriHost + "/api/callbacks";

            String incomingCallContext = "IncomingCallContext";
            // Create AnswerCallOptions
            AnswerCallOptions options = new AnswerCallOptions(incomingCallContext, callbackUri);
            options.setCallIntelligenceOptions(
                    new CallIntelligenceOptions().setCognitiveServicesEndpoint(cognitiveServicesEndpoint)
            );

            // Answer the call asynchronously
            Response<AnswerCallResult> result = client.answerCallWithResponse(options, Context.NONE);
            if (result.getStatusCode() == 200) {
                log.info("Answered call asynchronously. Connection ID: {}", result.getValue().getCallConnectionProperties().getCallConnectionId());
            } else {
                log.error("Failed to answer call asynchronously. Status code: {}", result.getStatusCode());
            }

            return ResponseEntity.ok("Answer call request sent asynchronously.");
        } catch (Exception e) {
            log.error("Error answering call asynchronously: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to answer call asynchronously.");
        }
    }

    @Tag(name = "04. Inbound Call APIs", description = "APIs for answering incoming calls")
    @PostMapping("/answerCall")
    public ResponseEntity<String> answerCall() {
        try {
            // Construct the callback URI
            String callbackUri = callbackUriHost + "/api/callbacks";

            String incomingCallContext = "IncomingCallContext";
            // Create AnswerCallOptions
            AnswerCallOptions options = new AnswerCallOptions(incomingCallContext, callbackUri);
            options.setCallIntelligenceOptions(
                    new CallIntelligenceOptions().setCognitiveServicesEndpoint(cognitiveServicesEndpoint)
            );

            // Answer the call 
            AnswerCallResult result = client.answerCall(incomingCallContext, callbackUri);
            if (result != null) {
                log.info("Answered call. Connection ID: {}", result.getCallConnectionProperties().getCallConnectionId());
            } else {
                log.error("Failed to answer call.");
            }

            return ResponseEntity.ok("Answer call request sent.");
        } catch (Exception e) {
            log.error("Error answering call: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to answer call.");
        }
    }

    @Tag(name = "04. Inbound Call APIs", description = "APIs for answering incoming calls")
    @PostMapping("/rejectCallAsync")
    public ResponseEntity<String> rejectCallAsync() {
        try {
            String incomingCallContext = "IncomingCallContext";
            // Create RejectCallOptions
            RejectCallOptions options = new RejectCallOptions(incomingCallContext);

            // Reject the call asynchronously
            client.rejectCallWithResponse(options, Context.NONE);
            log.info("Rejected call asynchronously.");
            return ResponseEntity.ok("Reject call request sent asynchronously.");
        } catch (Exception e) {
            log.error("Error rejecting call asynchronously: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to reject call asynchronously.");
        }
    }

    @Tag(name = "04. Inbound Call APIs", description = "APIs for answering incoming calls")
    @PostMapping("/rejectCall")
    public ResponseEntity<String> rejectCall() {
        try {
            String incomingCallContext = "IncomingCallContext";
            // Reject the call 
            client.rejectCall(incomingCallContext);
            log.info("Rejected call.");
            return ResponseEntity.ok("Reject call request sent.");
        } catch (Exception e) {
            log.error("Error rejecting call : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to reject call.");
        }
    }

    // // POST: /outboundCallToPstnAsync
    // @Tag(name = "Outbound Call APIs", description = "Outbound Call APIs")
    // @PostMapping("/outboundCallToPstnAsync")
    // public void outboundCallToPstnAsync(@RequestParam String targetPhoneNumber) {
    //     PhoneNumberIdentifier target = new PhoneNumberIdentifier(targetPhoneNumber);
    //     PhoneNumberIdentifier caller = new PhoneNumberIdentifier(acsPhoneNumber);

    //     //URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");

    //     CallInvite callInvite = new CallInvite(target, caller);
    //     CreateCallOptions createCallOptions = new CreateCallOptions(callInvite, callbackUriHost);

    //     // Make async call and block to get the result
    //     Response<CreateCallResult> response = asyncClient.createCallWithResponse(createCallOptions).block();

    //     if (response != null && response.getValue() != null) {
    //         String connectionId = response.getValue().getCallConnectionProperties().getCallConnectionId();
    //         log.info("Created async pstn call with connection id: " + connectionId);
    //     } else {
    //         log.error("Failed to create call. Response or value was null.");
    //     }
    // }

    // @Tag(name = "Outbound Call APIs", description = "Outbound Call APIs")
    // @PostMapping("/outboundCallToPstn")
    // public void outboundCallToPstn(@RequestParam String targetPhoneNumber) {
    //     PhoneNumberIdentifier target = new PhoneNumberIdentifier(targetPhoneNumber);
    //     PhoneNumberIdentifier caller = new PhoneNumberIdentifier(acsPhoneNumber);

    //     URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");
    //     CallInvite callInvite = new CallInvite(target, caller);

    //     // ✅ Convert URI to String
    //     CreateCallResult result = client.createCall(callInvite, callbackUri.toString());
    //     String connectionId = result.getCallConnectionProperties().getCallConnectionId();
    //     log.info("Created call with connection id: " + connectionId);  
    // }

    @Tag(name = "03. Outbound Call APIs", description = "Outbound Call APIs")
    @PostMapping("/createCallAsync")
    public ResponseEntity<String> createCallAsync() {
        CommunicationUserIdentifier target = new CommunicationUserIdentifier(acsPhoneNumber);
        CallInvite callInvite = new CallInvite(target);
        URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");

        CreateCallOptions createCallOptions = new CreateCallOptions(callInvite, callbackUri.toString());
        CallIntelligenceOptions callIntelligenceOptions = new CallIntelligenceOptions()
            .setCognitiveServicesEndpoint(cognitiveServicesEndpoint);
        createCallOptions.setCallIntelligenceOptions(callIntelligenceOptions);

        Response<CreateCallResult> result = client.createCallWithResponse(createCallOptions, Context.NONE);
        String connectionId = result.getValue().getCallConnectionProperties().getCallConnectionId();
        log.info("Created async call with connection id: " + connectionId);
        return ResponseEntity.ok("Created async call with connection id: " + connectionId);
    }

    @Tag(name = "03. Outbound Call APIs", description = "Outbound Call APIs")
    @PostMapping("/createCall")
    public ResponseEntity<String> createCall() {
        CommunicationUserIdentifier target = new CommunicationUserIdentifier(acsPhoneNumber);
        CallInvite callInvite = new CallInvite(target);
        URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");

        CreateCallResult result = client.createCall(callInvite, callbackUri.toString());
        String connectionId = result.getCallConnectionProperties().getCallConnectionId();
        log.info("Created call with connection id: " + connectionId);
        return ResponseEntity.ok("Created call with connection id: " + connectionId);
    }

    // @Tag(name = "Outbound Call APIs", description = "Outbound Call APIs")
    // @PostMapping("/outboundCallToTeamsAsync")
    // public void outboundCallToTeamsAsync(@RequestParam String teamsObjectId) {
    //     MicrosoftTeamsUserIdentifier target = new MicrosoftTeamsUserIdentifier(teamsObjectId);
    //     URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");

    //     CallInvite callInvite = new CallInvite(target);
    //     CreateCallOptions createCallOptions = new CreateCallOptions(callInvite, callbackUri.toString());

    //     // CallIntelligenceOptions callIntelligenceOptions = new CallIntelligenceOptions()
    //     //     .setCognitiveServicesEndpoint(cognitiveServicesEndpoint);
    //     // createCallOptions.setCallIntelligenceOptions(callIntelligenceOptions);

    //     Response<CreateCallResult> result = client.createCallWithResponse(createCallOptions, Context.NONE);
    //     String connectionId = result.getValue().getCallConnectionProperties().getCallConnectionId();
    //     log.info("Created async Teams call with connection id: " + connectionId);
    // }

    // @Tag(name = "Outbound Call APIs", description = "Outbound Call APIs")
    // @PostMapping("/outboundCallToTeams")
    // public void outboundCallToTeams(@RequestParam String teamsObjectId) {
    //     MicrosoftTeamsUserIdentifier target = new MicrosoftTeamsUserIdentifier(teamsObjectId);
    //     URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");

    //     CallInvite callInvite = new CallInvite(target);
    //     CreateCallResult result = client.createCall(callInvite, callbackUri.toString());
    //     String connectionId = result.getCallConnectionProperties().getCallConnectionId();
    //     log.info("Created Teams call with connection id: " + connectionId);
    // }

    @Tag(name = "05. Disconnect Call APIs", description = "Disconnect call APIs")
    @PostMapping("/hangupAsync")
    public ResponseEntity<String> hangupAsync(@RequestParam boolean isForEveryOne) {
        CallConnection callConnection = getConnection();

        callConnection.hangUpWithResponse(isForEveryOne, Context.NONE);
        log.info("Call hangup requested (async) forEveryone={}", isForEveryOne);

        return ResponseEntity.ok("Call hangup requested (async).");
    }

    @Tag(name = "05. Disconnect Call APIs", description = "Disconnect call APIs")
    @PostMapping("/hangup")
    public ResponseEntity<String> hangup(@RequestParam boolean isForEveryOne) {
        CallConnection callConnection = getConnection();

        callConnection.hangUp(isForEveryOne);
        log.info("Call hangup requested (sync) forEveryone={}", isForEveryOne);

        return ResponseEntity.ok("Call hangup requested.");
    }

    @Tag(name = "06. Hold Participant APIs", description = "Hold Participant APIs")
    @PostMapping("/holdParticipantAsync")
    public ResponseEntity<String> holdParticipantAsync(@RequestParam boolean isPlaySource) {
        CommunicationIdentifier target = new CommunicationUserIdentifier(acsPhoneNumber);
        HoldOptions holdOptions = new HoldOptions(target).setOperationContext("holdUserContext");
        CallMedia callMediaService = getCallMedia();

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
    }

    @Tag(name = "06. Hold Participant APIs", description = "Hold Participant APIs")
    @PostMapping("/holdParticipant")
    public ResponseEntity<String> holdParticipant(@RequestParam boolean isPlaySource) {
        CommunicationIdentifier target = new CommunicationUserIdentifier(acsPhoneNumber);
        TextSource textSource = null;
        CallMedia callMediaService = getCallMedia();

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
    }

    // @Tag(name = "Hold Participant APIs", description = "Hold Participant APIs")
    // @PostMapping("/interruptAudioAndAnnounceAsync")
    // public ResponseEntity<String> interruptAudioAndAnnounceAsync(@RequestParam String targetParticipant) {
    //     CommunicationIdentifier target = new CommunicationUserIdentifier(targetParticipant);
    //     CallMedia callMediaService = getCallMedia();

    //     TextSource textSource = new TextSource()
    //             .setText("Hi, this is interrupt audio and announcement test.")
    //             .setVoiceName("en-US-NancyNeural")
    //             .setSourceLocale("en-US")
    //             .setVoiceKind(VoiceKind.MALE);

    //     InterruptAudioAndAnnounceOptions options = new InterruptAudioAndAnnounceOptions(textSource, target)
    //             .setOperationContext("interruptContext");

    //     callMediaService.interruptAudioAndAnnounceWithResponse(options, Context.NONE);
    //     log.info("InterruptAudioAndAnnounce (async) sent to {}", targetParticipant);
    //     return ResponseEntity.ok("Interrupt audio and announce sent (async).");
    // }

    // @Tag(name = "Hold Participant APIs", description = "Hold Participant APIs")
    // @PostMapping("/interruptAudioAndAnnounce")
    // public ResponseEntity<String> interruptAudioAndAnnounce(@RequestParam String targetParticipant) {
    //     CommunicationIdentifier target = new PhoneNumberIdentifier(targetParticipant);
    //     CallMedia callMediaService = getCallMedia();

    //     TextSource textSource = new TextSource()
    //             .setText("Hi, this is interrupt audio and announcement test.")
    //             .setVoiceName("en-US-NancyNeural")
    //             .setSourceLocale("en-US")
    //             .setVoiceKind(VoiceKind.MALE);

    //     callMediaService.interruptAudioAndAnnounce(textSource, target);
    //     log.info("InterruptAudioAndAnnounce (sync) sent to {}", targetParticipant);
    //     return ResponseEntity.ok("Interrupt audio and announce sent.");
    // }

    @Tag(name = "06. Hold Participant APIs", description = "Hold Participant APIs")
    @PostMapping("/unholdParticipantAsync")
    public ResponseEntity<String> unholdParticipantAsync() {
        CommunicationIdentifier target = new CommunicationUserIdentifier(acsPhoneNumber);
        UnholdOptions unholdOptions = new UnholdOptions(target).setOperationContext("unholdUserContext");
        CallMedia callMediaService = getCallMedia();

        callMediaService.unholdWithResponse(unholdOptions, Context.NONE);
        log.info("Unhold participant asynchronously {}", acsPhoneNumber);
        return ResponseEntity.ok("Participant unheld (async).");
    }

    @Tag(name = "06. Hold Participant APIs", description = "Hold Participant APIs")
    @PostMapping("/unholdParticipant")
    public ResponseEntity<String> unholdParticipant() {
        CommunicationIdentifier target = new CommunicationUserIdentifier(acsPhoneNumber);
        CallMedia callMediaService = getCallMedia();

        callMediaService.unhold(target);
        log.info("Unhold participant synchronously {}", acsPhoneNumber);
        return ResponseEntity.ok("Participant unheld.");
    }

    @Tag(name = "06. Hold Participant APIs", description = "Hold Participant APIs")
    @PostMapping("/interruptHoldWithPlayAsync")
    public ResponseEntity<String> interruptHoldWithPlayAsync() {
        CommunicationIdentifier target = new CommunicationUserIdentifier(acsPhoneNumber);
        CallMedia callMediaService = getCallMedia();

        TextSource textSource = new TextSource()
                .setText("Hi, this is interrupt hold and play test.")
                .setVoiceName("en-US-NancyNeural")
                .setSourceLocale("en-US")
                .setVoiceKind(VoiceKind.MALE);

        List<CommunicationIdentifier> playTo = Collections.singletonList(target);
        PlayOptions playOptions = new PlayOptions(textSource, playTo)
                .setOperationContext("playToContext");
                //.setInterruptHoldAudio(true);

        callMediaService.playWithResponse(playOptions, Context.NONE);
        log.info("Interrupt hold with play sent (async) to {}", acsPhoneNumber);
        return ResponseEntity.ok("Interrupt hold with play sent (async).");
    }

    @Tag(name = "06. Hold Participant APIs", description = "Hold Participant APIs")
    @PostMapping("/interruptHoldWithPlay")
    public ResponseEntity<String> interruptHoldWithPlay() {
        CommunicationIdentifier target = new CommunicationUserIdentifier(acsPhoneNumber);
        CallMedia callMediaService = getCallMedia();

        TextSource textSource = new TextSource()
                .setText("Hi, this is interrupt hold and play test.")
                .setVoiceName("en-US-NancyNeural")
                .setSourceLocale("en-US")
                .setVoiceKind(VoiceKind.MALE);

        List<CommunicationIdentifier> playTo = Collections.singletonList(target);
        callMediaService.play(textSource, playTo);

        log.info("Interrupt hold with play sent (sync) to {}", acsPhoneNumber);
        return ResponseEntity.ok("Interrupt hold with play sent.");
    }

    // @Tag(name = "Get Participant APIs", description = "Get Participant APIs")
    // @PostMapping("/getPstnParticipantAsync")
    // public ResponseEntity<Void> getPstnParticipantAsync(@RequestParam String targetParticipant) {
    //     CallConnection callConnection = getConnection();
        
    //     Response<CallParticipant> response = callConnection.getParticipantWithResponse(
    //         new PhoneNumberIdentifier(targetParticipant),
    //         Context.NONE
    //     );
    
    //     CallParticipant participant = response.getValue();
    
    //     if (participant != null) {
    //         log.info("Participant: --> {}", participant.getIdentifier().getRawId());
    //         log.info("Is Participant on hold: --> {}", participant.isOnHold());
    //     }
    
    //     return ResponseEntity.ok().build();
    // }
    
    // @Tag(name = "Get Participant APIs", description = "Get Participant APIs")
    // @PostMapping("/getPstnParticipant")
    // public ResponseEntity<Void> getPstnParticipant(@RequestParam String targetParticipant) {
    //     CallConnection callConnection = getConnection();
    //     CallParticipant participant = callConnection.getParticipant(new PhoneNumberIdentifier(targetParticipant));

    //     if (participant != null) {
    //         log.info("Participant: --> {}", participant.getIdentifier().getRawId());
    //         log.info("Is Participant on hold: --> {}", participant.isOnHold());
    //     }
    //     return ResponseEntity.ok().build();
    // }

    @Tag(name = "09. Get Participant APIs", description = "Get Participant APIs")
    @PostMapping("/getParticipantAsync")
    public ResponseEntity<Void> getParticipantAsync() {
        CallConnection callConnection = getConnection();
    
        Response<CallParticipant> response = callConnection.getParticipantWithResponse(
            new CommunicationUserIdentifier(acsPhoneNumber),
            Context.NONE
        );
    
        CallParticipant participant = response.getValue();
    
        if (participant != null) {
            log.info("Participant: --> {}", participant.getIdentifier().getRawId());
            log.info("Is Participant on hold: --> {}", participant.isOnHold());
        } else {
            log.warn("No participant found for identifier: {}", acsPhoneNumber);
        }
    
        return ResponseEntity.ok().build();
    }
    
    @Tag(name = "09. Get Participant APIs", description = "Get Participant APIs")
    @PostMapping("/getParticipant")
    public ResponseEntity<Void> getParticipant() {
        CallConnection callConnection = getConnection();
        CallParticipant participant = callConnection.getParticipant(new CommunicationUserIdentifier(acsPhoneNumber));

        if (participant != null) {
            log.info("Participant: --> {}", participant.getIdentifier().getRawId());
            log.info("Is Participant on hold: --> {}", participant.isOnHold());
        }
        return ResponseEntity.ok().build();
    }

    // @Tag(name = "Get Participant APIs", description = "Get Participant APIs")
    // @PostMapping("/getTeamsParticipantAsync")
    // public ResponseEntity<Void> getTeamsParticipantAsync(@RequestParam String teamsObjectId) {
    //     CallConnection callConnection = getConnection();
    
    //     // Call the method correctly and extract the participant from the response
    //     Response<CallParticipant> response = callConnection.getParticipantWithResponse(
    //         new MicrosoftTeamsUserIdentifier(teamsObjectId),
    //         Context.NONE
    //     );
    
    //     CallParticipant participant = response.getValue();
    
    //     if (participant != null) {
    //         log.info("Participant: --> {}", participant.getIdentifier().getRawId());
    //         log.info("Is Participant on hold: --> {}", participant.isOnHold());
    //     } else {
    //         log.warn("No participant found for Teams Object ID: {}", teamsObjectId);
    //     }
    
    //     return ResponseEntity.ok().build();
    // }
    
    // @Tag(name = "Get Participant APIs", description = "Get Participant APIs")
    // @PostMapping("/getTeamsParticipant")
    // public ResponseEntity<Void> getTeamsParticipant(@RequestParam String teamsObjectId) {
    //     CallConnection callConnection = getConnection();
    //     CallParticipant participant = callConnection.getParticipant(new MicrosoftTeamsUserIdentifier(teamsObjectId));

    //     if (participant != null) {
    //         log.info("Participant: --> {}", participant.getIdentifier().getRawId());
    //         log.info("Is Participant on hold: --> {}", participant.isOnHold());
    //     }
    //     return ResponseEntity.ok().build();
    // }

    @Tag(name = "09. Get Participant APIs", description = "Get Participant APIs")
    @PostMapping("/getParticipantListAsync")
    public ResponseEntity<Void> getParticipantListAsync() {
        CallConnection callConnection = getConnection();

        PagedIterable<CallParticipant> participants = callConnection.listParticipants(Context.NONE);

        if (participants != null) {
            for (CallParticipant participant : participants) {
                log.info("----------------------------------------------------------------------");
                log.info("Participant: --> {}", participant.getIdentifier().getRawId());
                log.info("Is Participant on hold: --> {}", participant.isOnHold());
                log.info("----------------------------------------------------------------------");
            }
        } else {
            log.warn("No participants returned in the response.");
        }

        return ResponseEntity.ok().build();
    }

    @Tag(name = "09. Get Participant APIs", description = "Get Participant APIs")
    @PostMapping("/getParticipantList")
    public ResponseEntity<Void> getParticipantList() {
        CallConnection callConnection = getConnection();

        PagedIterable<CallParticipant> participants = callConnection.listParticipants();

        if (participants != null) {
            for (CallParticipant participant : participants) {
                log.info("----------------------------------------------------------------------");
                log.info("Participant: --> {}", participant.getIdentifier().getRawId());
                log.info("Is Participant on hold: --> {}", participant.isOnHold());
                log.info("----------------------------------------------------------------------");
            }
        } else {
            log.warn("No participants returned in the response.");
        }

        return ResponseEntity.ok().build();
    }

    @Tag(name = "08. Mute Participant APIs", description = "Mute Participant APIs")
    @PostMapping("/muteParticipantAsync")
    public ResponseEntity<String> muteParticipantAsync() {
        CommunicationIdentifier target = new CommunicationUserIdentifier(acsPhoneNumber);
        CallConnection callConnection = getConnection();

        MuteParticipantOptions options = new MuteParticipantOptions(target)
                .setOperationContext("muteContext");

        // Assuming you're calling a method like muteParticipantWithResponse(options, context)
        callConnection.muteParticipantWithResponse(options, Context.NONE);

        log.info("Muted participant asynchronously: {}", acsPhoneNumber);
        return ResponseEntity.ok("Muted participant (async).");
    }

    @Tag(name = "08. Mute Participant APIs", description = "Mute Participant APIs")
    @PostMapping("/muteParticipant")
    public ResponseEntity<String> muteParticipant() {
        CommunicationIdentifier target = new CommunicationUserIdentifier(acsPhoneNumber);
        CallConnection callConnection = getConnection();

        callConnection.muteParticipant(target); // Synchronous mute using options if method is available
        log.info("Muted participant synchronously: {}", acsPhoneNumber);
        return ResponseEntity.ok("Muted participant.");
    }

    // @Tag(name = "Add/Remove Participant APIs", description = "Add/Remove Participant APIs")
    // @PostMapping("/addPstnParticipantAsync")
    // public ResponseEntity<Object> addPstnParticipantAsync(@RequestParam String pstnParticipant) {
    //     CallConnection  callConnectionService = getConnection();
    //     CallInvite callInvite = new CallInvite(
    //             new PhoneNumberIdentifier(pstnParticipant),
    //             new PhoneNumberIdentifier("acsPhoneNumber")); // Replace with actual ACS number
    //     AddParticipantOptions options = new AddParticipantOptions(callInvite);
    //     options.setOperationContext("addPstnUserContext");
    //     options.setInvitationTimeout(Duration.ofSeconds(15));
    //     Object result = callConnectionService.addParticipantWithResponse(options,Context.NONE);
    //     return ResponseEntity.ok(result);
    // }

    // @Tag(name = "Add/Remove Participant APIs", description = "Add/Remove Participant APIs")
    // @PostMapping("/addPstnParticipant")
    // public ResponseEntity<Object> addPstnParticipant(@RequestParam String pstnParticipant) {
    //     CallConnection  callConnectionService = getConnection();
    //     CallInvite callInvite = new CallInvite(
    //             new PhoneNumberIdentifier(pstnParticipant),
    //             new PhoneNumberIdentifier(acsPhoneNumber)).setSourceCallerIdNumber(new PhoneNumberIdentifier(acsPhoneNumber)); // Replace with actual ACS number
    //     Object result = callConnectionService.addParticipant(callInvite);
    //     return ResponseEntity.ok(result);
    // }

    @Tag(name = "07. Add/Remove Participant APIs", description = "Add/Remove Participant APIs")
    @PostMapping("/addParticipantAsync")
    public ResponseEntity<String> addParticipantAsync(@RequestParam String targetParticipant) {
        CallInvite callInvite = new CallInvite(new CommunicationUserIdentifier(targetParticipant));
        AddParticipantOptions options = new AddParticipantOptions(callInvite);
        options.setOperationContext("addUserContext");
        options.setInvitationTimeout(Duration.ofSeconds(15));
       
        CallConnection  callConnectionService = getConnection();
        Response<AddParticipantResult> result = callConnectionService.addParticipantWithResponse(options, Context.NONE);
        return ResponseEntity.ok("Invitation Id: " + result.getValue().getInvitationId());
    }

    @Tag(name = "07. Add/Remove Participant APIs", description = "Add/Remove Participant APIs")
    @PostMapping("/addParticipant")
    public ResponseEntity<String> addParticipant(@RequestParam String targetParticipant) {
        CallInvite callInvite = new CallInvite(new CommunicationUserIdentifier(targetParticipant));
        CallConnection  callConnectionService = getConnection();
        AddParticipantResult result = callConnectionService.addParticipant(callInvite);
        return ResponseEntity.ok("Invitation Id: " + result.getInvitationId());
    }

    // @Tag(name = "Add/Remove Participant APIs", description = "Add/Remove Participant APIs")
    // @PostMapping("/addTeamsParticipantAsync")
    // public ResponseEntity<Object> addTeamsParticipantAsync(@RequestParam String teamsObjectId) {
    //     CallInvite callInvite = new CallInvite(new MicrosoftTeamsUserIdentifier(teamsObjectId));
    //     AddParticipantOptions options = new AddParticipantOptions(callInvite);
    //     options.setOperationContext("addTeamsUserContext");
    //     options.setInvitationTimeout(Duration.ofSeconds(15));
    //     CallConnection  callConnectionService = getConnection();
    //     Object result = callConnectionService.addParticipantWithResponse(options, Context.NONE);
    //     return ResponseEntity.ok(result);
    // }

    // @Tag(name = "Add/Remove Participant APIs", description = "Add/Remove Participant APIs")
    // @PostMapping("/addTeamsParticipant")
    // public ResponseEntity<Object> addTeamsParticipant(@RequestParam String teamsObjectId) {
    //     CallInvite callInvite = new CallInvite(new MicrosoftTeamsUserIdentifier(teamsObjectId));
    //     CallConnection  callConnectionService = getConnection();
    //     Object result = callConnectionService.addParticipant(callInvite);
    //     return ResponseEntity.ok(result);
    // }

    // @Tag(name = "Add/Remove Participant APIs", description = "Add/Remove Participant APIs")
    // @PostMapping("/removePstnParticipantAsync")
    // public ResponseEntity<Void> removePstnParticipantAsync(@RequestParam String targetParticipant) {
    //     RemoveParticipantOptions options = new RemoveParticipantOptions(new PhoneNumberIdentifier(targetParticipant));
    //     options.setOperationContext("removePstnParticipantContext");
    //     CallConnection  callConnectionService = getConnection();
    //     callConnectionService.removeParticipantWithResponse(options,Context.NONE);
    //     return ResponseEntity.ok().build();
    // }

    // @Tag(name = "Add/Remove Participant APIs", description = "Add/Remove Participant APIs")
    // @PostMapping("/removePstnParticipant")
    // public ResponseEntity<Void> removePstnParticipant(@RequestParam String targetParticipant) {
    //     CallConnection  callConnectionService = getConnection();
    //     callConnectionService.removeParticipant(new PhoneNumberIdentifier(targetParticipant));
    //     return ResponseEntity.ok().build();
    // }

    @Tag(name = "07. Add/Remove Participant APIs", description = "Add/Remove Participant APIs")
    @PostMapping("/removeParticipantAsync")
    public ResponseEntity<Void> removeParticipantAsync(@RequestParam String targetParticipant) {
        RemoveParticipantOptions options = new RemoveParticipantOptions(new CommunicationUserIdentifier(targetParticipant));
        options.setOperationContext("removeParticipantContext");
        CallConnection  callConnectionService = getConnection();
        callConnectionService.removeParticipantWithResponse(options,Context.NONE);
        return ResponseEntity.ok().build();
    }

    @Tag(name = "07. Add/Remove Participant APIs", description = "Add/Remove Participant APIs")
    @PostMapping("/removeParticipant")
    public ResponseEntity<Void> removeAcsParticipant(@RequestParam String targetParticipant) {
        CallConnection  callConnectionService = getConnection();
        callConnectionService.removeParticipant(new CommunicationUserIdentifier(targetParticipant));
        return ResponseEntity.ok().build();
    }

    // @Tag(name = "Add/Remove Participant APIs", description = "Add/Remove Participant APIs")
    // @PostMapping("/removeTeamsParticipantAsync")
    // public ResponseEntity<Void> removeTeamsParticipantAsync(@RequestParam String teamsObjectId) {
    //     RemoveParticipantOptions options = new RemoveParticipantOptions(new MicrosoftTeamsUserIdentifier(teamsObjectId));
    //     options.setOperationContext("removeTeamsParticipantContext");
    //     CallConnection  callConnectionService = getConnection();
    //     callConnectionService.removeParticipantWithResponse(options,Context.NONE);
    //     return ResponseEntity.ok().build();
    // }

    // @Tag(name = "Add/Remove Participant APIs", description = "Add/Remove Participant APIs")
    // @PostMapping("/removeTeamsParticipant")
    // public ResponseEntity<Void> removeTeamsParticipant(@RequestParam String teamsObjectId) {
    //     CallConnection  callConnectionService = getConnection();
    //     callConnectionService.removeParticipant(new MicrosoftTeamsUserIdentifier(teamsObjectId));
    //     return ResponseEntity.ok().build();
    // }

    // region Cancel Add Participant
    @Tag(name = "07. Add/Remove Participant APIs", description = "Add/Remove Participant APIs")
    @PostMapping("/cancelAddParticipantAsync")
    public ResponseEntity<Object> cancelAddParticipantAsync(@RequestParam String invitationId) {
        CancelAddParticipantOperationOptions options = new CancelAddParticipantOperationOptions(invitationId);
        options.setOperationContext("CancelAddingParticipantContext");
        CallConnection  callConnectionService = getConnection();
        Object result = callConnectionService.cancelAddParticipantOperationWithResponse(options,Context.NONE);
        return ResponseEntity.ok(result);
    }

    @Tag(name = "07. Add/Remove Participant APIs", description = "Add/Remove Participant APIs")
    @PostMapping("/cancelAddParticipant")
    public ResponseEntity<Object> cancelAddParticipant(@RequestParam String invitationId) {
        CallConnection  callConnectionService = getConnection();
        Object result = callConnectionService.cancelAddParticipantOperation(invitationId);
        return ResponseEntity.ok(result);
    }

    // @Tag(name = "Transfer Call APIs", description = "APIs for transferring calls to participants")
    // @PostMapping("/transferCallToParticipantAsync")
    // public ResponseEntity<String> transferCallToParticipantAsync(@RequestParam String targetParticipant) {
    //     try {
    //         // Create the target participant
    //         CommunicationIdentifier target = new CommunicationUserIdentifier(targetParticipant);

    //         // Create TransferCallToParticipantOptions
    //         TransferCallToParticipantOptions options = new TransferCallToParticipantOptions(target)
    //                 .setOperationContext("TransferCallContext");

    //         // Transfer the call asynchronously
    //         client.getCallConnection(callConnectionId)
    //                 .transferCallToParticipantWithResponse(options, Context.NONE);

    //         log.info("Call transferred asynchronously to participant: {}", targetParticipant);
    //         return ResponseEntity.ok("Transfer call request sent asynchronously.");
    //     } catch (Exception e) {
    //         log.error("Error transferring call asynchronously: {}", e.getMessage());
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to transfer call asynchronously.");
    //     }
    // }

    @Tag(name = "10. Transfer Call APIs", description = "APIs for transferring calls to participants")
    @PostMapping("/transferCallToParticipant")
    public ResponseEntity<String> transferCallToParticipant(@RequestParam String targetParticipant) {
        try {
            // Create the target participant
            CommunicationIdentifier target = new CommunicationUserIdentifier(targetParticipant);

            // Transfer the call 
            client.getCallConnection(callConnectionId)
                    .transferCallToParticipant(target);

            log.info("Call transferred to participant: {}", targetParticipant);
            return ResponseEntity.ok("Transfer call request sent.");
        } catch (Exception e) {
            log.error("Error transferring call : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to transfer call.");
        }
    }

    // @Tag(name = "Play Media APIs", description = "Play Media APIs")
    // @PostMapping("/playTextSourceToPstnTargetAsync")
    // public ResponseEntity<Void> playTextSourceToPstnTargetAsync(@RequestParam String targetParticipant) {
    //     CallMedia callMedia = getCallMedia();
    //     TextSource textSource = createTextSource("Hi, this is test source played through play source thanks. Goodbye!.");
    //     List<CommunicationIdentifier> playTo = Collections.singletonList(new PhoneNumberIdentifier(targetParticipant));

    //     PlayOptions options = new PlayOptions(textSource, playTo);
    //     options.setOperationContext("playToContext");
    //     callMedia.playWithResponse(options,Context.NONE);
    //     return ResponseEntity.ok().build();
    // }

    // @Tag(name = "Play Media APIs", description = "Play Media APIs")
    // @PostMapping("/playTextSourceToPstnTarget")
    // public ResponseEntity<Void> playTextSourceToPstnTarget(@RequestParam String targetParticipant) {
    //     CallMedia callMedia = getCallMedia();
    //     TextSource textSource = createTextSource("Hi, this is test source played through play source thanks. Goodbye!.");
    //     List<CommunicationIdentifier> playTo = Collections.singletonList(new PhoneNumberIdentifier(targetParticipant));
    //     callMedia.play(textSource,playTo);
    //     return ResponseEntity.ok().build();
    // }

    @Tag(name = "11. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playTextSourceTargetAsync")
    public ResponseEntity<Void> playTextSourceTargetAsync() {
        CallMedia callMedia = getCallMedia();
        List<CommunicationIdentifier> playTo = Collections.singletonList(new CommunicationUserIdentifier(acsPhoneNumber));
        TextSource textSource = createTextSource("Hi, this is test source played through play source thanks. Goodbye!.");
        PlayOptions options = new PlayOptions(textSource,playTo);
        options.setOperationContext("playToContext");
        callMedia.playWithResponse(options,Context.NONE);
        return ResponseEntity.ok().build();
    }

    @Tag(name = "11. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playTextSourceToTarget")
    public ResponseEntity<Void> playTextSourceToTarget() {
        CallMedia callMedia = getCallMedia();
        TextSource textSource = createTextSource("Hi, this is test source played through play source thanks. Goodbye!.");
        callMedia.play(textSource,Collections.singletonList(new CommunicationUserIdentifier(acsPhoneNumber)));
        return ResponseEntity.ok().build();
    }

    // @Tag(name = "Play Media APIs", description = "Play Media APIs")
    // @PostMapping("/playTextSourceToTeamsTargetAsync")
    // public ResponseEntity<Void> playTextSourceToTeamsTargetAsync(@RequestParam String teamsObjectId) {
    //     CallMedia callMedia = getCallMedia();
    //     TextSource textSource = createTextSource("Hi, this is test source played through play source thanks. Goodbye!.");
    //     PlayOptions options = new PlayOptions(textSource, Collections.singletonList(new MicrosoftTeamsUserIdentifier(teamsObjectId)));
    //     options.setOperationContext("playToContext");
    //     callMedia.playWithResponse(options,Context.NONE);
    //     return ResponseEntity.ok().build();
    // }

    // @Tag(name = "Play Media APIs", description = "Play Media APIs")
    // @PostMapping("/playTextSourceToTeamsTarget")
    // public ResponseEntity<Void> playTextSourceToTeamsTarget(@RequestParam String teamsObjectId) {
    //     CallMedia callMedia = getCallMedia();
    //     TextSource textSource = createTextSource("Hi, this is test source played through play source thanks. Goodbye!.");
    //     callMedia.play(textSource, Collections.singletonList(new MicrosoftTeamsUserIdentifier(teamsObjectId)));
    //     return ResponseEntity.ok().build();
    // }

    @Tag(name = "11. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playTextSourceToAllAsync")
    public ResponseEntity<Void> playTextSourceToAllAsync() {
        CallMedia callMedia = getCallMedia();
        TextSource textSource = createTextSource("Hi, this is test source played through play source thanks. Goodbye!.");
        PlayToAllOptions options = new PlayToAllOptions(textSource);
        options.setOperationContext("playToAllContext");
        callMedia.playToAllWithResponse(options,Context.NONE);
        return ResponseEntity.ok().build();
    }

    @Tag(name = "11. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playTextSourceToAll")
    public ResponseEntity<Void> playTextSourceToAll() {
        CallMedia callMedia = getCallMedia();
        TextSource textSource = createTextSource("Hi, this is test source played through play source thanks. Goodbye!.");
        PlayToAllOptions options = new PlayToAllOptions(textSource);
        options.setOperationContext("playToAllContext");
        callMedia.playToAll(textSource);
        return ResponseEntity.ok().build();
    }

    @Tag(name = "11. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playTextSourceBargeInAsync")
    public ResponseEntity<Void> playTextSourceBargeInAsync() {
        CallMedia callMedia = getCallMedia();
        TextSource textSource = createTextSource("Hi, this is barge in test played through play source thanks. Goodbye!.");
        PlayToAllOptions options = new PlayToAllOptions(textSource);
        options.setOperationContext("playToAllContext");
        options.setInterruptCallMediaOperation(true);
        callMedia.playToAllWithResponse(options,Context.NONE);
        return ResponseEntity.ok().build();
    }

    // @Tag(name = "Play Media APIs", description = "Play Media APIs")
    // @PostMapping("/playSsmlSourceToPstnTargetAsync")
    // public ResponseEntity<Void> playSsmlSourceToPstnTargetAsync(@RequestParam String targetParticipant) {
    //     return playSsml(targetParticipant, TargetType.PSTN, true, false);
    // }

    // // 2. PSTN - Sync
    // @Tag(name = "Play Media APIs", description = "Play Media APIs")
    // @PostMapping("/playSsmlSourceToPstnTarget")
    // public ResponseEntity<Void> playSsmlSourceToPstnTarget(@RequestParam String targetParticipant) {
    //     return playSsml(targetParticipant, TargetType.PSTN, false, false);
    // }

    // 3.  - Async
    @Tag(name = "11. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playSsmlSourceTargetAsync")
    public ResponseEntity<Void> playSsmlSourceTargetAsync() {
        return playSsml(acsPhoneNumber, TargetType.ACS, true, false);
    }

    // 4.  - Sync
    @Tag(name = "11. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playSsmlSourceToTarget")
    public ResponseEntity<Void> playSsmlSourceToTarget() {
        return playSsml(acsPhoneNumber, TargetType.ACS, false, false);
    }

    // // 5. Teams - Async
    // @Tag(name = "Play Media APIs", description = "Play Media APIs")
    // @PostMapping("/playSsmlSourceToTeamsTargetAsync")
    // public ResponseEntity<Void> playSsmlSourceToTeamsTargetAsync(@RequestParam String teamsUserId) {
    //     return playSsml(teamsUserId, TargetType.TEAMS, true, false);
    // }

    // // 6. Teams - Sync
    // @Tag(name = "Play Media APIs", description = "Play Media APIs")
    // @PostMapping("/playSsmlSourceToTeamsTarget")
    // public ResponseEntity<Void> playSsmlSourceToTeamsTarget(@RequestParam String teamsUserId) {
    //     return playSsml(teamsUserId, TargetType.TEAMS, false, false);
    // }

    // 7. All - Async
    @Tag(name = "11. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playSsmlSourceToAllAsync")
    public ResponseEntity<Void> playSsmlSourceToAllAsync() {
        return playSsml(null, TargetType.ALL, true, false);
    }

    // 8. All - Sync
    @Tag(name = "11. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playSsmlSourceToAll")
    public ResponseEntity<Void> playSsmlSourceToAll() {
        return playSsml(null, TargetType.ALL, false, false);
    }

    // 9. Barge-In - Async
    @Tag(name = "11. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playSsmlSourceBargeInAsync")
    public ResponseEntity<Void> playSsmlSourceBargeInAsync() {
        return playSsml(null, TargetType.ALL, true, true);
    }

    //     // 1. PSTN - Async
    // @Tag(name = "Play Media APIs", description = "Play Media APIs")
    // @PostMapping("/playFileSourceToPstnTargetAsync")
    // public ResponseEntity<Void> playFileSourceToPstnTargetAsync(@RequestParam String targetParticipant) {
    //     return playFile(targetParticipant, TargetType.PSTN, true, false);
    // }

    // // 2. PSTN - Sync
    // @Tag(name = "Play Media APIs", description = "Play Media APIs")
    // @PostMapping("/playFileSourceToPstnTarget")
    // public ResponseEntity<Void> playFileSourceToPstnTarget(@RequestParam String targetParticipant) {
    //     return playFile(targetParticipant, TargetType.PSTN, false, false);
    // }

    // 3.  - Async
    @Tag(name = "11. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playFileSourceToTargetAsync")
    public ResponseEntity<Void> playFileSourceToTargetAsync() {
        return playFile(acsPhoneNumber, TargetType.ACS, true, false);
    }

    // 4.  - Sync
    @Tag(name = "11. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playFileSourceToTarget")
    public ResponseEntity<Void> playFileSourceToTarget() {
        return playFile(acsPhoneNumber, TargetType.ACS, false, false);
    }

    // // 5. Teams - Async
    // @Tag(name = "Play Media APIs", description = "Play Media APIs")
    // @PostMapping("/playFileSourceToTeamsTargetAsync")
    // public ResponseEntity<Void> playFileSourceToTeamsTargetAsync(@RequestParam String teamsUserId) {
    //     return playFile(teamsUserId, TargetType.TEAMS, true, false);
    // }

    // // 6. Teams - Sync
    // @Tag(name = "Play Media APIs", description = "Play Media APIs")
    // @PostMapping("/playFileSourceToTeamsTarget")
    // public ResponseEntity<Void> playFileSourceToTeamsTarget(@RequestParam String teamsUserId) {
    //     return playFile(teamsUserId, TargetType.TEAMS, false, false);
    // }

    // 7. All - Async
    @Tag(name = "11. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playFileSourceToAllAsync")
    public ResponseEntity<Void> playFileSourceToAllAsync() {
        return playFile(null, TargetType.ALL, true, false);
    }

    // 8. All - Sync
    @Tag(name = "11. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playFileSourceToAll")
    public ResponseEntity<Void> playFileSourceToAll() {
        return playFile(null, TargetType.ALL, false, false);
    }

    // 9. Barge-In - Async
    @Tag(name = "11. Play Media APIs", description = "Play Media APIs")
    @PostMapping("/playFileSourceBargeInAsync")
    public ResponseEntity<Void> playFileSourceBargeInAsync() {
        return playFile(null, TargetType.ALL, true, true);
    }

    @Tag(name = "15. Start Recognition APIs", description = "Start Recognition APIs")
    @PostMapping("/recognizeDTMFAsync")
    public ResponseEntity<Void> recognizeDTMFAsync() {
        return startDtmfRecognition(acsPhoneNumber, true);
    }

    @Tag(name = "15. Start Recognition APIs", description = "Start Recognition APIs")
    @PostMapping("/recognizeDTMF")
    public ResponseEntity<Void> recognizeDTMF() {
        return startDtmfRecognition(acsPhoneNumber, false);
    }

    @Tag(name = "15. Start Recognition APIs", description = "Start Recognition APIs")
    @PostMapping("/recognizeSpeechAsync")
    public ResponseEntity<Void> recognizeSpeechAsync() {
        return startSpeechRecognition(acsPhoneNumber, true);
    }

    @Tag(name = "15. Start Recognition APIs", description = "Start Recognition APIs")
    @PostMapping("/recognizeSpeech")
    public ResponseEntity<Void> recognizeSpeech() {
        return startSpeechRecognition(acsPhoneNumber, false);
    }

    @Tag(name = "15. Start Recognition APIs", description = "Start Recognition APIs")
    @PostMapping("/recognizeSpeechOrDtmfAsync")
    public ResponseEntity<Void> recognizeSpeechOrDtmfAsync() {
        return startSpeechOrDtmfRecognition(acsPhoneNumber, true);
    }

    @Tag(name = "15. Start Recognition APIs", description = "Start Recognition APIs")
    @PostMapping("/recognizeSpeechOrDtmf")
    public ResponseEntity<Void> recognizeSpeechOrDtmf() {
        return startSpeechOrDtmfRecognition(acsPhoneNumber, false);
    }

    @Tag(name = "15. Start Recognition APIs", description = "Start Recognition APIs")
    @PostMapping("/recognizeChoiceAsync")
    public ResponseEntity<Void> recognizeChoiceAsync() {
        return startChoiceRecognition(acsPhoneNumber, true);
    }

    @Tag(name = "15. Start Recognition APIs", description = "Start Recognition APIs")
    @PostMapping("/recognizeChoice")
    public ResponseEntity<Void> recognizeChoice() {
        return startChoiceRecognition(acsPhoneNumber, false);
    }

       // Async Equivalent: /sendDTMFTonesAsync (C#)
    @Tag(name = "14. Send or Start DTMF APIs", description = "Send or Start DTMF APIs")
    @PostMapping("/sendDTMFTonesAsync")
    public ResponseEntity<String> sendDTMFTonesAsync() {
        CommunicationIdentifier target = new CommunicationUserIdentifier(acsPhoneNumber);
        List<DtmfTone> tones = Arrays.asList(DtmfTone.ZERO, DtmfTone.ONE);
        CallMedia callMediaService = getCallMedia();
        callMediaService.sendDtmfTones(tones, target); // .block() internally

        log.info("Async DTMF tones sent to {}", acsPhoneNumber);
        return ResponseEntity.ok("DTMF tones sent (async simulation).");
    }

    // Sync Equivalent: /sendDTMFTones (C#)
    @Tag(name = "14. Send or Start DTMF APIs", description = "Send or Start DTMF APIs")
    @PostMapping("/sendDTMFTones")
    public ResponseEntity<String> sendDTMFTones() {
        CommunicationIdentifier target = new CommunicationUserIdentifier(acsPhoneNumber);
        List<DtmfTone> tones = Arrays.asList(DtmfTone.ZERO, DtmfTone.ONE);
        CallMedia callMediaService = getCallMedia();
        callMediaService.sendDtmfTones(tones, target);

        log.info("DTMF tones sent to {}", acsPhoneNumber);
        return ResponseEntity.ok("DTMF tones sent.");
    }

    // Async Equivalent: /startContinuousDTMFTonesAsync (C#)
    @Tag(name = "14. Send or Start DTMF APIs", description = "Send or Start DTMF APIs")
    @PostMapping("/startContinuousDTMFTonesAsync")
    public ResponseEntity<String> startContinuousDTMFTonesAsync() {
        CommunicationIdentifier target = new CommunicationUserIdentifier(acsPhoneNumber);
        CallMedia callMediaService = getCallMedia();
        callMediaService.startContinuousDtmfRecognition(target); // .block() internally

        log.info("Async continuous DTMF started for {}", acsPhoneNumber);
        return ResponseEntity.ok("Started continuous DTMF recognition (async simulation).");
    }

    // Sync Equivalent: /startContinuousDTMFTones (C#)
    @Tag(name = "14. Send or Start DTMF APIs", description = "Send or Start DTMF APIs")
    @PostMapping("/startContinuousDTMFTones")
    public ResponseEntity<String> startContinuousDTMFTones() {
        CommunicationIdentifier target = new CommunicationUserIdentifier(acsPhoneNumber);
        CallMedia callMediaService = getCallMedia();
        callMediaService.startContinuousDtmfRecognition(target);

        log.info("Started continuous DTMF for {}", acsPhoneNumber);
        return ResponseEntity.ok("Started continuous DTMF recognition.");
    }

    // Async Equivalent: /stopContinuousDTMFTonesAsync (C#)
    @Tag(name = "14. Send or Start DTMF APIs", description = "Send or Start DTMF APIs")
    @PostMapping("/stopContinuousDTMFTonesAsync")
    public ResponseEntity<String> stopContinuousDTMFTonesAsync() {
        CommunicationIdentifier target = new CommunicationUserIdentifier(acsPhoneNumber);
        CallMedia callMediaService = getCallMedia();
        callMediaService.stopContinuousDtmfRecognition(target); // .block() internally

        log.info("Async stop continuous DTMF for {}", acsPhoneNumber);
        return ResponseEntity.ok("Stopped continuous DTMF recognition (async simulation).");
    }

    // Sync Equivalent: /stopContinuousDTMFTones (C#)
    @Tag(name = "14. Send or Start DTMF APIs", description = "Send or Start DTMF APIs")
    @PostMapping("/stopContinuousDTMFTones")
    public ResponseEntity<String> stopContinuousDTMFTones() {
        CommunicationIdentifier target = new CommunicationUserIdentifier(acsPhoneNumber);
        CallMedia callMediaService = getCallMedia();
        callMediaService.stopContinuousDtmfRecognition(target);

        log.info("Stopped continuous DTMF for {}", acsPhoneNumber);
        return ResponseEntity.ok("Stopped continuous DTMF recognition.");
    }

    // @Tag(name = "Create Group Call APIs", description = "Create Group Call APIs")
    // @PostMapping("/createGroupCallAsync")
    // public void createGroupCallAsync(@RequestParam String targetPhoneNumber) {
    //     PhoneNumberIdentifier target = new PhoneNumberIdentifier(targetPhoneNumber);
    //     PhoneNumberIdentifier sourceCallerId = new PhoneNumberIdentifier(acsPhoneNumber);

    //     URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");
    //     String websocketUri = callbackUriHost.replace("https", "wss") + "/ws";

    //     MediaStreamingOptions mediaStreamingOptions = new MediaStreamingOptions(
    //             websocketUri,
    //             MediaStreamingTransport.WEBSOCKET,
    //             MediaStreamingContent.AUDIO,
    //             MediaStreamingAudioChannel.UNMIXED,
    //             false
    //     );

    //     TranscriptionOptions transcriptionOptions = new TranscriptionOptions(
    //             websocketUri,
    //             TranscriptionTransport.WEBSOCKET,
    //             "en-us",
    //             false
    //     );

    //     List<CommunicationIdentifier> targets = List.of(target);

    //     CreateGroupCallOptions createGroupCallOptions = new CreateGroupCallOptions(targets, callbackUri.toString())
    //             .setCallIntelligenceOptions(new CallIntelligenceOptions().setCognitiveServicesEndpoint(cognitiveServicesEndpoint))
    //             .setSourceCallIdNumber(sourceCallerId)
    //             .setMediaStreamingOptions(mediaStreamingOptions)
    //             .setTranscriptionOptions(transcriptionOptions);

    //     Response<CreateCallResult> result = client.createGroupCallWithResponse(createGroupCallOptions, Context.NONE);
    //     String connectionId = result.getValue().getCallConnectionProperties().getCallConnectionId();
    //     log.info("Created async group call with connection id: {}", connectionId);
    // }

    // @Tag(name = "Create Group Call APIs", description = "Create Group Call APIs")
    // @PostMapping("/createGroupCall")
    // public void createGroupCall(@RequestParam String targetPhoneNumber) {
    //     PhoneNumberIdentifier target = new PhoneNumberIdentifier(targetPhoneNumber);
    //     PhoneNumberIdentifier sourceCallerId = new PhoneNumberIdentifier(acsPhoneNumber);

    //     URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");

    //     List<CommunicationIdentifier> targets = List.of(target,sourceCallerId);

    //     CreateCallResult result = client.createGroupCall(targets,callbackUri.toString());
    //     String connectionId = result.getCallConnectionProperties().getCallConnectionId();
    //     log.info("Created group call with connection id: {}", connectionId);
    // }

    // @Tag(name = "Connect Call APIs", description = "Connect Call APIs")
    // @PostMapping("/connectRoomCallAsync")
    // public ResponseEntity<String> connectRoomCallAsync(@RequestParam String roomId) {
    //     return connectCallAsync(new RoomCallLocator(roomId), "ConnectRoomCallContext");
    // }

    // @Tag(name = "Connect Call APIs", description = "Connect Call APIs")
    // @PostMapping("/connectRoomCall")
    // public ResponseEntity<String> connectRoomCall(@RequestParam String roomId) {
    //     return connectCall(new RoomCallLocator(roomId), "ConnectRoomCallContext");
    // }

    // @Tag(name = "Connect Call APIs", description = "Connect Call APIs")
    // @PostMapping("/connectGroupCallAsync")
    // public ResponseEntity<String> connectGroupCallAsync(@RequestParam String groupId) {
    //     return connectCallAsync(new GroupCallLocator(groupId), "ConnectGroupCallContext");
    // }

    // @Tag(name = "Connect Call APIs", description = "Connect Call APIs")
    // @PostMapping("/connectGroupCall")
    // public ResponseEntity<String> connectGroupCall(@RequestParam String groupId) {
    //     return connectCall(new GroupCallLocator(groupId), "ConnectGroupCallContext");
    // }

    // @Tag(name = "Connect Call APIs", description = "Connect Call APIs")
    // @PostMapping("/connectOneToNCallAsync")
    // public ResponseEntity<String> connectOneToNCallAsync(@RequestParam String serverCallId) {
    //     return connectCallAsync(new ServerCallLocator(serverCallId), "ConnectOneToNCallContext");
    // }

    // @Tag(name = "Connect Call APIs", description = "Connect Call APIs")
    // @PostMapping("/connectOneToNCall")
    // public ResponseEntity<String> connectOneToNCall(@RequestParam String serverCallId) {
    //     return connectCall(new ServerCallLocator(serverCallId), "ConnectOneToNCallContext");
    // }

    @Tag(name = "12. Recording APIs", description = "Recording APIs")
    @PostMapping("/startRecordingWithVideoMp4MixedAsync")
    public void startRecordingWithVideoMp4MixedAsync(@RequestParam boolean isRecordingWithCallConnectionId, @RequestParam boolean isPauseOnStart) {
        CallConnectionProperties properties = getCallConnectionProperties();
        CallLocator locator = new ServerCallLocator(properties.getServerCallId());
        String eventCallbackUri = callbackUriHost + "/api/callbacks";
        StartRecordingOptions options = new StartRecordingOptions(locator);

        options.setRecordingContent(RecordingContent.AUDIO_VIDEO);
        options.setRecordingFormat(RecordingFormat.MP4);
        options.setRecordingChannel(RecordingChannel.MIXED);
        options.setRecordingStateCallbackUrl(eventCallbackUri);
        options.setPauseOnStart(isPauseOnStart);
        recordingFileFormat = "mp4";

            Response<RecordingStateResult> response = client.getCallRecording()
        .startWithResponse(options, Context.NONE);

        recordingId = response.getValue().getRecordingId();
        log.info("Recording started. RecordingId: {}", recordingId);
    }

    @Tag(name = "12. Recording APIs", description = "Recording APIs")
    @PostMapping("/startRecordingWithVideoMp4Mixed")
    public void startRecordingWithVideoMp4Mixed(@RequestParam boolean isRecordingWithCallConnectionId, @RequestParam boolean isPauseOnStart) {
        CallConnectionProperties properties = getCallConnectionProperties();
        String eventCallbackUri = callbackUriHost + "/api/callbacks";
        CallLocator locator = new ServerCallLocator(properties.getServerCallId());
        StartRecordingOptions options = new StartRecordingOptions(locator);

        options.setRecordingContent(RecordingContent.AUDIO_VIDEO);
        options.setRecordingFormat(RecordingFormat.MP4);
        options.setRecordingChannel(RecordingChannel.MIXED);
        options.setRecordingStateCallbackUrl(eventCallbackUri);
        options.setPauseOnStart(isPauseOnStart);
        recordingFileFormat = "mp4";

        recordingId = client.getCallRecording().start(options).getRecordingId();
        log.info("Recording started. RecordingId: {}", recordingId);
    }

    @Tag(name = "12. Recording APIs", description = "Recording APIs")
    @PostMapping("/startRecordingWithAudioMp3MixedAsync")
    public void startRecordingWithAudioMp3MixedAsync(@RequestParam boolean isRecordingWithCallConnectionId, @RequestParam boolean isPauseOnStart) {
        CallConnectionProperties properties = getCallConnectionProperties();
        CallLocator locator = new ServerCallLocator(properties.getServerCallId());
        String eventCallbackUri = callbackUriHost + "/api/callbacks";
        StartRecordingOptions options = new StartRecordingOptions(locator);

        options.setRecordingContent(RecordingContent.AUDIO);
        options.setRecordingFormat(RecordingFormat.MP3);
        options.setRecordingChannel(RecordingChannel.MIXED);
        options.setRecordingStateCallbackUrl(eventCallbackUri);
        options.setPauseOnStart(isPauseOnStart);
        recordingFileFormat = "mp3";

            Response<RecordingStateResult> response = client.getCallRecording()
        .startWithResponse(options, Context.NONE);

        recordingId = response.getValue().getRecordingId();
        log.info("Recording started. RecordingId: {}", recordingId);
    }

    @Tag(name = "12. Recording APIs", description = "Recording APIs")
    @PostMapping("/startRecordingWithAudioMp3Mixed")
    public void startRecordingWithAudioMp3Mixed(@RequestParam boolean isRecordingWithCallConnectionId, @RequestParam boolean isPauseOnStart) {
        CallConnectionProperties properties = getCallConnectionProperties();
        String eventCallbackUri = callbackUriHost + "/api/callbacks";
        CallLocator locator = new ServerCallLocator(properties.getServerCallId());
        StartRecordingOptions options = new StartRecordingOptions(locator);

        options.setRecordingContent(RecordingContent.AUDIO);
        options.setRecordingFormat(RecordingFormat.MP3);
        options.setRecordingChannel(RecordingChannel.MIXED);
        options.setRecordingStateCallbackUrl(eventCallbackUri);
        options.setPauseOnStart(isPauseOnStart);
        recordingFileFormat = "mp4";

        recordingId = client.getCallRecording().start(options).getRecordingId();
        log.info("Recording started. RecordingId: {}", recordingId);
    }

    @Tag(name = "12. Recording APIs", description = "Recording APIs")
    @PostMapping("/startRecordingWithAudioMp3UnMixedAsync")
    public void startRecordingWithAudioMp3UnMixedAsync(@RequestParam boolean isRecordingWithCallConnectionId, @RequestParam boolean isPauseOnStart) {
        CallConnectionProperties properties = getCallConnectionProperties();
        CallLocator locator = new ServerCallLocator(properties.getServerCallId());
        String eventCallbackUri = callbackUriHost + "/api/callbacks";
        StartRecordingOptions options = new StartRecordingOptions(locator);

        options.setRecordingContent(RecordingContent.AUDIO);
        options.setRecordingFormat(RecordingFormat.MP3);
        options.setRecordingChannel(RecordingChannel.UNMIXED);
        options.setRecordingStateCallbackUrl(eventCallbackUri);
        options.setPauseOnStart(isPauseOnStart);
        recordingFileFormat = "mp3";

            Response<RecordingStateResult> response = client.getCallRecording()
        .startWithResponse(options, Context.NONE);

        recordingId = response.getValue().getRecordingId();
        log.info("Recording started. RecordingId: {}", recordingId);
    }

    @Tag(name = "12. Recording APIs", description = "Recording APIs")
    @PostMapping("/startRecordingWithAudioMp3Unmixed")
    public void startRecordingWithAudioMp3Unmixed(@RequestParam boolean isRecordingWithCallConnectionId, @RequestParam boolean isPauseOnStart) {
        CallConnectionProperties properties = getCallConnectionProperties();
        String eventCallbackUri = callbackUriHost + "/api/callbacks";
        CallLocator locator = new ServerCallLocator(properties.getServerCallId());
        StartRecordingOptions options = new StartRecordingOptions(locator);

        options.setRecordingContent(RecordingContent.AUDIO);
        options.setRecordingFormat(RecordingFormat.MP3);
        options.setRecordingChannel(RecordingChannel.UNMIXED);
        options.setRecordingStateCallbackUrl(eventCallbackUri);
        options.setPauseOnStart(isPauseOnStart);
        recordingFileFormat = "mp4";

        recordingId = client.getCallRecording().start(options).getRecordingId();
        log.info("Recording started. RecordingId: {}", recordingId);
    }

    @Tag(name = "12. Recording APIs", description = "Recording APIs")
    @PostMapping("/startRecordingWithAudioWavUnMixedAsync")
    public void startRecordingWithAudioWavUnMixedAsync(@RequestParam boolean isRecordingWithCallConnectionId, @RequestParam boolean isPauseOnStart) {
        CallConnectionProperties properties = getCallConnectionProperties();
        CallLocator locator = new ServerCallLocator(properties.getServerCallId());
        String eventCallbackUri = callbackUriHost + "/api/callbacks";
        StartRecordingOptions options = new StartRecordingOptions(locator);

        options.setRecordingContent(RecordingContent.AUDIO);
        options.setRecordingFormat(RecordingFormat.WAV);
        options.setRecordingChannel(RecordingChannel.UNMIXED);
        options.setRecordingStateCallbackUrl(eventCallbackUri);
        options.setPauseOnStart(isPauseOnStart);
        recordingFileFormat = "mp3";

            Response<RecordingStateResult> response = client.getCallRecording()
        .startWithResponse(options, Context.NONE);

        recordingId = response.getValue().getRecordingId();
        log.info("Recording started. RecordingId: {}", recordingId);
    }

    @Tag(name = "12. Recording APIs", description = "Recording APIs")
    @PostMapping("/startRecordingWithAudioWavUnmixed")
    public void startRecordingWithAudioWavUnmixed(@RequestParam boolean isRecordingWithCallConnectionId, @RequestParam boolean isPauseOnStart) {
        CallConnectionProperties properties = getCallConnectionProperties();
        String eventCallbackUri = callbackUriHost + "/api/callbacks";
        CallLocator locator = new ServerCallLocator(properties.getServerCallId());
        StartRecordingOptions options = new StartRecordingOptions(locator);

        options.setRecordingContent(RecordingContent.AUDIO);
        options.setRecordingFormat(RecordingFormat.WAV);
        options.setRecordingChannel(RecordingChannel.UNMIXED);
        options.setRecordingStateCallbackUrl(eventCallbackUri);
        options.setPauseOnStart(isPauseOnStart);
        recordingFileFormat = "mp4";

        recordingId = client.getCallRecording().start(options).getRecordingId();
        log.info("Recording started. RecordingId: {}", recordingId);
    }

    @Tag(name = "12. Recording APIs", description = "Recording APIs")
    @PostMapping("/pauseRecordingAsync")
    public void pauseRecordingAsync() {
        client.getCallRecording().pauseWithResponse(recordingId, null);
    }

    @Tag(name = "12. Recording APIs", description = "Recording APIs")
    @PostMapping("/pauseRecording")
    public void pauseRecording() {
        client.getCallRecording().pause(recordingId);
    }

    @Tag(name = "12. Recording APIs", description = "Recording APIs")
    @PostMapping("/resumeRecordingAsync")
    public void resumeRecordingAsync() {
        client.getCallRecording().resumeWithResponse(recordingId, null);
    }

    @Tag(name = "12. Recording APIs", description = "Recording APIs")
    @PostMapping("/resumeRecording")
    public void resumeRecording() {
        client.getCallRecording().resume(recordingId);
    }

    @Tag(name = "12. Recording APIs", description = "Recording APIs")
    @PostMapping("/stopRecordingAsync")
    public void stopRecordingAsync() {
        client.getCallRecording().stopWithResponse(recordingId,null);
    }

    @Tag(name = "12. Recording APIs", description = "Recording APIs")
    @PostMapping("/stopRecording")
    public void stopRecording() {
        client.getCallRecording().stop(recordingId);
    }

    @Tag(name = "12. Recording APIs", description = "Recording APIs")
    @GetMapping("/downloadRecording")
    public void downloadRecording() {
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
    }

    @Tag(name = "13. Cancel All Media Operation APIs", description = "Cancel All Media Operation APIs")
    @PostMapping("/cancelAllMediaOperationAsync")
    public ResponseEntity<Void> cancelAllMediaOperationAsync() {
        try {
            CallMedia callMedia = getCallMedia();
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

    @Tag(name = "13. Cancel All Media Operation APIs", description = "Cancel All Media Operation APIs")
    @PostMapping("/cancelAllMediaOperation")
    public ResponseEntity<Void> cancelAllMediaOperation() {
        try {
            CallMedia callMedia = getCallMedia();
            callMedia.cancelAllMediaOperations(); // synchronous method
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            log.error("Error during cancel", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Tag(name = "16. Transcription APIs", description = "APIs for managing call transcriptions")
    @PostMapping("/startTranscriptionAsync")
    public ResponseEntity<String> startTranscriptionAsync() {
        try {
            StartTranscriptionOptions transcriptionOptions = new StartTranscriptionOptions()
                    .setLocale("en-us");

            CallMedia callMedia = getCallMedia();
            callMedia.startTranscriptionWithResponse(transcriptionOptions, Context.NONE);

            log.info("Started transcription asynchronously for call");
            return ResponseEntity.ok("Transcription started successfully.");
        } catch (Exception e) {
            log.error("Error starting transcription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to start transcription.");
        }
    }

    @Tag(name = "16. Transcription APIs", description = "APIs for managing call transcriptions")
    @PostMapping("/startTranscription")
    public ResponseEntity<String> startTranscription() {
        try {
            CallMedia callMedia = getCallMedia();
            callMedia.startTranscription();

            log.info("Started transcription for call");
            return ResponseEntity.ok("Transcription started successfully.");
        } catch (Exception e) {
            log.error("Error starting transcription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to start transcription.");
        }
    }

    @Tag(name = "16. Transcription APIs", description = "APIs for managing call transcriptions")
    @PostMapping("/stopTranscriptionAsync")
    public ResponseEntity<String> stopTranscriptionAsync() {
        try {
            StopTranscriptionOptions transcriptionOptions = new StopTranscriptionOptions();

            CallMedia callMedia = getCallMedia();
            callMedia.stopTranscriptionWithResponse(transcriptionOptions, Context.NONE);

            log.info("Stopped transcription asynchronously for call");
            return ResponseEntity.ok("Transcription stopped successfully.");
        } catch (Exception e) {
            log.error("Error stopping transcription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to stop transcription.");
        }
    }

    @Tag(name = "16. Transcription APIs", description = "APIs for managing call transcriptions")
    @PostMapping("/stopTranscription")
    public ResponseEntity<String> stopTranscription() {
        try {
            CallMedia callMedia = getCallMedia();
            callMedia.stopTranscription();

            log.info("Stopped transcription for call");
            return ResponseEntity.ok("Transcription stopped successfully.");
        } catch (Exception e) {
            log.error("Error stopping transcription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to stop transcription.");
        }
    }

    // 🔄 Shared Method
    public CallMedia getCallMedia() {
        if (callConnectionId == null || callConnectionId.isEmpty()) {
            throw new IllegalArgumentException("Call connection id is empty");
        }
        return client.getCallConnection(callConnectionId).getCallMedia();
    }

    public CallConnection getConnection() {
        if (callConnectionId == null || callConnectionId.isEmpty()) {
            throw new IllegalArgumentException("Call connection id is empty");
        }
        return client.getCallConnection(callConnectionId);
    }

    public CallConnectionProperties getCallConnectionProperties() {
        if (callConnectionId == null || callConnectionId.isEmpty()) {
            throw new IllegalArgumentException("Call connection id is empty");
        }
        return client.getCallConnection(callConnectionId).getCallProperties();
    }

    // private ResponseEntity<String> connectCall(CallLocator locator, String context) {
    //     try {
    //         URI callbackUri = new URI(callbackUriHost + "/api/callbacks");
    //         ConnectCallResult result = client.connectCall(locator,callbackUri.toString());
    //         log.info("Connected sync call with connection ID: {}", result.getCallConnectionProperties().getCallConnectionId());
    //         return ResponseEntity.ok("Call connected successfully");

    //     } catch (Exception e) {
    //         log.error("Error connecting call: {}", e.getMessage());
    //         return ResponseEntity.status(500).body("Call connection failed");
    //     }
    // }

    // private ResponseEntity<String> connectCallAsync(CallLocator locator, String context) {
    //     try {
    //         String callbackUri = callbackUriHost + "/api/callbacks";
    //         String websocketUri = callbackUriHost.replace("https", "wss") + "/ws";

    //         MediaStreamingOptions mediaOptions = new MediaStreamingOptions(websocketUri, MediaStreamingTransport.WEBSOCKET, MediaStreamingContent.AUDIO,
    //                 MediaStreamingAudioChannel.UNMIXED, false);

    //         TranscriptionOptions transcriptionOptions = new TranscriptionOptions(websocketUri, TranscriptionTransport.WEBSOCKET,
    //                 "en-us", false);

    //         ConnectCallOptions options = new ConnectCallOptions(locator, callbackUri)
    //                 .setOperationContext(context)
    //                // .setCallIntelligenceOptions(new CallIntelligenceOptions().setCognitiveServicesEndpoint(CognitiveServiceEndpoint))
    //                 .setMediaStreamingOptions(mediaOptions)
    //                 .setTranscriptionOptions(transcriptionOptions);

    //     Mono<Response<ConnectCallResult>> response  = asyncClient.connectCallWithResponse(options);
    //         response.subscribe(res -> {
    //             if (res.getStatusCode() == 200) {
    //                 log.info("Connected async call. Connection ID: {}", res.getValue().getCallConnectionProperties().getCallConnectionId());
    //             } else {
    //                 log.error("Call async connection failed with status code: {}", res.getStatusCode());
    //             }
    //         });
    //         return ResponseEntity.ok("Async call request sent");

    //     } catch (Exception e) {
    //         log.error("Error connecting async call: {}", e.getMessage());
    //         return ResponseEntity.status(500).body("Async call connection failed");
    //     }
    // }

    private TextSource createTextSource(String message) {
        var textSource = new TextSource()
        .setText(message)
        .setVoiceName("en-US-NancyNeural");
            return textSource;
    }

    private static final String SSML_STRING = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>" +
        "<voice name='en-US-JennyNeural'>Hi, this is %s test played through SSML source. Goodbye!</voice></speak>";
    
    public SsmlSource createSsmlSource(boolean bargeIn) {
        String bargeText = bargeIn ? "barge in" : "test";
        SsmlSource ssmlSource = new SsmlSource();
        ssmlSource.setSsmlText(String.format(SSML_STRING, bargeText));
        return ssmlSource;
    }

    private ResponseEntity<Void> playSsml(String target, TargetType targetType, boolean async, boolean bargeIn) {
        SsmlSource ssmlSource = createSsmlSource(bargeIn);
        String context = bargeIn ? "bargeInContext" : "testContext";
        CallMedia mediaService = getCallMedia();

        try {
            if (targetType == TargetType.ALL) {
                PlayToAllOptions options = new PlayToAllOptions(ssmlSource);
                options.setOperationContext(context);
                options.setInterruptCallMediaOperation(bargeIn);

                if (async) {
                    mediaService.playToAll(Collections.singletonList(ssmlSource));
                } else {
                    mediaService.playToAllWithResponse(options, Context.NONE);
                }
            } else {
                List<CommunicationIdentifier> playTo = switch (targetType) {
                    case PSTN -> List.of(new PhoneNumberIdentifier(target));
                    case ACS -> List.of(new CommunicationUserIdentifier(target));
                    case TEAMS -> List.of(new MicrosoftTeamsUserIdentifier(target));
                    default -> throw new IllegalArgumentException("Unsupported target type.");
                };

                PlayOptions options = new PlayOptions(ssmlSource, playTo);
                options.setOperationContext(context);
                //options.setInterruptHoldAudio(bargeIn);

                if (async) {
                    mediaService.play(ssmlSource, playTo);
                } else {
                    mediaService.playWithResponse(options, Context.NONE);
                }
            }
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private static final String FILE_SOURCE_URI = "https://yourdomain.com/sample.wav"; // replace with actual URI

    private ResponseEntity<Void> playFile(String target, TargetType targetType, boolean async, boolean bargeIn) {
        FileSource fileSource = new FileSource().setUrl(FILE_SOURCE_URI);
        String context = bargeIn ? "playBargeInContext" : "playContext";
        CallMedia mediaService = getCallMedia();
    
        try {
            if (targetType == TargetType.ALL) {
                PlayToAllOptions options = new PlayToAllOptions(fileSource);
                options.setOperationContext(context);
                options.setInterruptCallMediaOperation(bargeIn);
    
                if (async) {
                    mediaService.playToAll(Collections.singletonList(fileSource));
                } else {
                    mediaService.playToAllWithResponse(options, Context.NONE);
                }
            } else {
                List<CommunicationIdentifier> playTo = switch (targetType) {
                    case PSTN -> List.of(new PhoneNumberIdentifier(target));
                    case ACS -> List.of(new CommunicationUserIdentifier(target));
                    case TEAMS -> List.of(new MicrosoftTeamsUserIdentifier(target));
                    default -> throw new IllegalArgumentException("Unsupported target type.");
                };
    
                PlayOptions options = new PlayOptions(fileSource, playTo);
                options.setOperationContext(context);
                //options.setInterruptHoldAudio(bargeIn);
    
                if (async) {
                    mediaService.play(fileSource, playTo);
                } else {
                    mediaService.playWithResponse(options, Context.NONE);
                }
            }
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<Void> startDtmfRecognition(String target, boolean async) {
        try {
            CallMedia callMedia = getCallMedia();
            TextSource prompt = new TextSource()
                .setText("Hi, this is recognize test. Please provide input. Thanks!")
                .setVoiceName("en-US-NancyNeural")
                .setSourceLocale("en-US")
                .setVoiceKind(VoiceKind.MALE); // Optional: if enum NEURAL is available
    
            PhoneNumberIdentifier participant = new PhoneNumberIdentifier(target);
    
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
    
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private ResponseEntity<Void> startSpeechRecognition(String target, boolean async) {
        try {
            CallMedia callMedia = getCallMedia();
            TextSource prompt = new TextSource()
                .setText("Hi, this is recognize test. Please provide input. Thanks!")
                .setVoiceName("en-US-NancyNeural")
                .setSourceLocale("en-US")
                .setVoiceKind(VoiceKind.MALE);
    
            PhoneNumberIdentifier participant = new PhoneNumberIdentifier(target);
    
            CallMediaRecognizeSpeechOptions options = new CallMediaRecognizeSpeechOptions(participant, Duration.ofSeconds(15))
            .setInterruptPrompt(false)
            .setInitialSilenceTimeout(Duration.ofSeconds(15))
            .setPlayPrompt(prompt) // Fixed method call
            .setOperationContext("SpeechContext");
    
                if (async) {
                    callMedia.startRecognizingWithResponse(options, Context.NONE); // async version
                } else {
                    callMedia.startRecognizing(options); // sync version
                }
    
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private ResponseEntity<Void> startSpeechOrDtmfRecognition(String target, boolean async) {
        try {
            CallMedia callMedia = getCallMedia();
            TextSource prompt = new TextSource()
                .setText("Hi, this is recognize test. Please provide input. Thanks!")
                .setVoiceName("en-US-NancyNeural")
                .setSourceLocale("en-US")
                .setVoiceKind(VoiceKind.MALE);
    
            PhoneNumberIdentifier participant = new PhoneNumberIdentifier(target);
    
            var options = new CallMediaRecognizeSpeechOrDtmfOptions(participant, 4, null)
            .setInterruptPrompt(false)
            .setInitialSilenceTimeout(Duration.ofSeconds(15))
            .setPlayPrompts(prompt) // Fixed method call
            .setOperationContext("SpeechContext");
    
                if (async) {
                    callMedia.startRecognizingWithResponse(options, Context.NONE); // async version
                } else {
                    callMedia.startRecognizing(options); // sync version
                }
    
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private ResponseEntity<Void> startChoiceRecognition(String target, boolean async) {
        try {
            CallMedia callMedia = getCallMedia();
            TextSource prompt = new TextSource()
                .setText("Hi, this is recognize test. Please provide input. Thanks!")
                .setVoiceName("en-US-NancyNeural")
                .setSourceLocale("en-US")
                .setVoiceKind(VoiceKind.MALE);
    
            PhoneNumberIdentifier participant = new PhoneNumberIdentifier(target);
    
            CallMediaRecognizeChoiceOptions options = new CallMediaRecognizeChoiceOptions(participant, getChoices())
                .setInterruptPrompt(false)
                .setInterruptCallMediaOperation(false)
                .setInitialSilenceTimeout(Duration.ofSeconds(10))
                .setPlayPrompt(prompt)
                .setOperationContext("ChoiceContext");
    
                if (async) {
                    callMedia.startRecognizingWithResponse(options, Context.NONE); // async version
                } else {
                    callMedia.startRecognizing(options); // sync version
                }
    
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private enum TargetType {
        PSTN,
        ACS,
        TEAMS,
        ALL
    }

    private List<RecognitionChoice> getChoices(){
        var choices = Arrays.asList(
            new RecognitionChoice().setLabel(confirmLabel).setPhrases(Arrays.asList("Confirm", "First", "One")).setTone(DtmfTone.ONE),
            new RecognitionChoice().setLabel(cancelLabel).setPhrases(Arrays.asList("Cancel", "Second", "Two")).setTone(DtmfTone.TWO)
            );
            return choices;
    }
    
    private CallAutomationClient initClient() {
        try {
            return new CallAutomationClientBuilder()
                    .connectionString(appConfig.getAcsConnectionString())
                    .buildClient();
        } catch (NullPointerException e) {
            log.error("Please verify if Application config is properly set up");
            return null;
        } catch (Exception e) {
            log.error("Error occurred when initializing Call Automation Client: {} {}", e.getMessage(), e.getCause());
            return null;
        }
    }

    // private CallAutomationAsyncClient initAsyncClient() {
    //     CallAutomationAsyncClient client;
    //     try {
    //         client = new CallAutomationClientBuilder()
    //                 .connectionString(appConfig.getAcsConnectionString())
    //                 .buildAsyncClient();
    //         return client;
    //     } catch (NullPointerException e) {
    //         log.error("Please verify if Application config is properly set up");
    //         return null;
    //     } catch (Exception e) {
    //         log.error("Error occurred when initializing Call Automation Async Client: {} {}",
    //                 e.getMessage(),
    //                 e.getCause());
    //         return null;
    //     }
    // }
}
