package com.communication.callautomation;

import com.azure.communication.callautomation.CallAutomationAsyncClient;
import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.CallConnection;
import com.azure.communication.callautomation.CallDialog;
import com.azure.communication.callautomation.CallMedia;
import com.azure.communication.callautomation.implementation.models.PowerVirtualAgentsDialog;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.common.MicrosoftTeamsUserIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.communication.identity.implementation.models.CommunicationErrorResponseException;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.core.models.CloudEvent;
import com.azure.core.util.Context;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.systemevents.AcsIncomingCallEventData;
import com.azure.messaging.eventgrid.systemevents.AcsRecordingFileStatusUpdatedEventData;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationEventData;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import reactor.core.publisher.Mono;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@Slf4j
public class ProgramSample {

    private final ConfigurationRequest appConfig;
    private CallAutomationClient client;
    private final CallAutomationAsyncClient asyncClient;
    // Configuration state variables
    private String acsConnectionString = "";
    private String cognitiveServicesEndpoint = "";
    private String acsPhoneNumber = "";
    private String callbackUriHost = "";
    private String fileSourceUri = "";

    private String callConnectionId = "";
    private String recordingId = "";
    private String recordingLocation = "";
    private String recordingFileFormat = "";
    private URI eventCallbackUri;

    private String callerId = "";
    private String calleeId = "";

    private Map<String, String> botRouting = new HashMap<>();
    private String defaultBotId = "";
    private static final String INCOMING_CALL_CONTEXT = "incomingCallContext";
    private ConfigurationRequest configuration = new ConfigurationRequest();
    private String confirmLabel = "Confirm";
    private String cancelLabel = "Cancel";
    public ProgramSample(final ConfigurationRequest appConfig) {
        this.appConfig = appConfig;
        client = initClient();
        asyncClient =  initAsyncClient();
    }

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

    @PostMapping("/api/setConfigurations")
    public ResponseEntity<String> setConfigurations(@RequestBody ConfigurationRequest configurationRequest) {
        // Reset variables
        acsConnectionString = "";
        cognitiveServicesEndpoint = "";
        acsPhoneNumber = "";
        callbackUriHost = "";
        fileSourceUri = "";

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

            configuration.setBotRouting(
                Optional.ofNullable(configurationRequest.getBotRouting())
                    .orElse(new HashMap<>())
            );

            configuration.setDefaultBotId(
                Optional.ofNullable(configurationRequest.getDefaultBotId())
                    .filter(s -> !s.isEmpty())
                    .orElseThrow(() -> new IllegalArgumentException("DefaultBotId is required"))
            );
        }

        // Assign to global variables
        acsConnectionString = configuration.getAcsConnectionString();
        cognitiveServicesEndpoint = configuration.getCognitiveServiceEndpoint();
        acsPhoneNumber = configuration.getAcsPhoneNumber();
        callbackUriHost = configuration.getCallbackUriHost();
        botRouting = configuration.getBotRouting();
        defaultBotId = configuration.getDefaultBotId();
        fileSourceUri = "https://sample-videos.com/audio/mp3/crowd-cheering.mp3";

        client = new CallAutomationClientBuilder()
                     .connectionString(acsConnectionString)
                     .buildClient();

        log.info("Initialized call automation client.");
        return ResponseEntity.ok("Configuration set successfully. Initialized call automation client.");
    }
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

                    URI callbackUri = new URI(appConfig.getCallbackUriHost() + "/api/callbacks");
                    log.info("Incoming call - correlationId: {}, Callback url: {}",
                            incomingCallEventData.getCorrelationId(), callbackUri);

                    eventCallbackUri = callbackUri;

                    AnswerCallOptions options = new AnswerCallOptions(
                        incomingCallEventData.getIncomingCallContext(),
                        callbackUri.toString()
                    );
                    
                    // options.setCallIntelligenceOptions(
                    //         new CallIntelligenceOptions()
                    //         .setCognitiveServicesEndpoint(appConfig.getCognitiveServiceEndpoint()
                    //         )
                    //);

                    // Call client to answer call
                    AnswerCallResult result = client.answerCallWithResponse(options, Context.NONE).getValue();
                    CallMedia callConnectionMedia = result.getCallConnection().getCallMedia();
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

// POST: /outboundCallToPstnAsync
@PostMapping("/outboundCallToPstnAsync")
public void outboundCallToPstnAsync(@RequestParam String targetPhoneNumber) {
    PhoneNumberIdentifier target = new PhoneNumberIdentifier(targetPhoneNumber);
    PhoneNumberIdentifier caller = new PhoneNumberIdentifier(acsPhoneNumber);

    URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");
    eventCallbackUri = callbackUri;

    CallInvite callInvite = new CallInvite(target, caller);
    CreateCallOptions createCallOptions = new CreateCallOptions(callInvite, appConfig.getCallbackUriHost());

    // Make async call and block to get the result
    Response<CreateCallResult> response = asyncClient.createCallWithResponse(createCallOptions).block();

    if (response != null && response.getValue() != null) {
        String connectionId = response.getValue().getCallConnectionProperties().getCallConnectionId();
        log.info("Created async pstn call with connection id: " + connectionId);
    } else {
        log.error("Failed to create call. Response or value was null.");
    }
}


@PostMapping("/outboundCallToPstn")
public void outboundCallToPstn(@RequestParam String targetPhoneNumber) {
    PhoneNumberIdentifier target = new PhoneNumberIdentifier(targetPhoneNumber);
    PhoneNumberIdentifier caller = new PhoneNumberIdentifier(acsPhoneNumber);

    URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");
    eventCallbackUri = callbackUri;
    CallInvite callInvite = new CallInvite(target, caller);

    // ✅ Convert URI to String
    CreateCallResult result = client.createCall(callInvite, callbackUri.toString());
    String connectionId = result.getCallConnectionProperties().getCallConnectionId();
    log.info("Created call with connection id: " + connectionId);  
}

@PostMapping("/outboundCallToAcsAsync")
public void outboundCallToAcsAsync(@RequestParam String acsTarget) {
    CommunicationUserIdentifier target = new CommunicationUserIdentifier(acsTarget);
    URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");
    eventCallbackUri = callbackUri;

    CallInvite callInvite = new CallInvite(target);
    CreateCallOptions createCallOptions = new CreateCallOptions(callInvite, callbackUri.toString());

    // CallIntelligenceOptions callIntelligenceOptions = new CallIntelligenceOptions()
    //     .setCognitiveServicesEndpoint(cognitiveServicesEndpoint);
    // createCallOptions.setCallIntelligenceOptions(callIntelligenceOptions);

    Response<CreateCallResult> result = client.createCallWithResponse(createCallOptions, Context.NONE);
    String connectionId = result.getValue().getCallConnectionProperties().getCallConnectionId();
    log.info("Created async ACS call with connection id: " + connectionId);
}
@PostMapping("/outboundCallToAcs")
public void outboundCallToAcs(@RequestParam String acsTarget) {
    CommunicationUserIdentifier target = new CommunicationUserIdentifier(acsTarget);
    URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");
    eventCallbackUri = callbackUri;

    CallInvite callInvite = new CallInvite(target);
    CreateCallResult result = client.createCall(callInvite, callbackUri.toString());
    String connectionId = result.getCallConnectionProperties().getCallConnectionId();
    log.info("Created ACS call with connection id: " + connectionId);
}

@PostMapping("/outboundCallToTeamsAsync")
public void outboundCallToTeamsAsync(@RequestParam String teamsObjectId) {
    MicrosoftTeamsUserIdentifier target = new MicrosoftTeamsUserIdentifier(teamsObjectId);
    URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");
    eventCallbackUri = callbackUri;

    CallInvite callInvite = new CallInvite(target);
    CreateCallOptions createCallOptions = new CreateCallOptions(callInvite, callbackUri.toString());

    // CallIntelligenceOptions callIntelligenceOptions = new CallIntelligenceOptions()
    //     .setCognitiveServicesEndpoint(cognitiveServicesEndpoint);
    // createCallOptions.setCallIntelligenceOptions(callIntelligenceOptions);

    Response<CreateCallResult> result = client.createCallWithResponse(createCallOptions, Context.NONE);
    String connectionId = result.getValue().getCallConnectionProperties().getCallConnectionId();
    log.info("Created async Teams call with connection id: " + connectionId);
}
@PostMapping("/outboundCallToTeams")
public void outboundCallToTeams(@RequestParam String teamsObjectId) {
    MicrosoftTeamsUserIdentifier target = new MicrosoftTeamsUserIdentifier(teamsObjectId);
    URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");
    eventCallbackUri = callbackUri;

    CallInvite callInvite = new CallInvite(target);
    CreateCallResult result = client.createCall(callInvite, callbackUri.toString());
    String connectionId = result.getCallConnectionProperties().getCallConnectionId();
    log.info("Created Teams call with connection id: " + connectionId);
}
@PostMapping("/createGroupCallAsync")
    public void createGroupCallAsync(@RequestParam String targetPhoneNumber) {
        PhoneNumberIdentifier target = new PhoneNumberIdentifier(targetPhoneNumber);
        PhoneNumberIdentifier sourceCallerId = new PhoneNumberIdentifier(appConfig.getAcsPhoneNumber());

        URI callbackUri = URI.create(appConfig.getCallbackUriHost() + "/api/callbacks");
        eventCallbackUri = callbackUri;
        String websocketUri = appConfig.getCallbackUriHost().replace("https", "wss") + "/ws";

        MediaStreamingOptions mediaStreamingOptions = new MediaStreamingOptions(
                websocketUri,
                MediaStreamingTransport.WEBSOCKET,
                MediaStreamingContent.AUDIO,
                MediaStreamingAudioChannel.UNMIXED,
                false
        );

        TranscriptionOptions transcriptionOptions = new TranscriptionOptions(
                websocketUri,
                TranscriptionTransport.WEBSOCKET,
                "en-us",
                false
        );

        List<CommunicationIdentifier> targets = List.of(target);

        CreateGroupCallOptions createGroupCallOptions = new CreateGroupCallOptions(targets, callbackUri.toString())
               // .setCallIntelligenceOptions(new CallIntelligenceOptions().setCognitiveServicesEndpoint(appConfig.getCognitiveServiceEndpoint()))
                .setSourceCallIdNumber(sourceCallerId)
                .setMediaStreamingOptions(mediaStreamingOptions)
                .setTranscriptionOptions(transcriptionOptions);

        Response<CreateCallResult> result = client.createGroupCallWithResponse(createGroupCallOptions, Context.NONE);
        String connectionId = result.getValue().getCallConnectionProperties().getCallConnectionId();
        log.info("Created async group call with connection id: {}", connectionId);
    }

    @PostMapping("/createGroupCall")
    public void createGroupCall(@RequestParam String targetPhoneNumber) {
        PhoneNumberIdentifier target = new PhoneNumberIdentifier(targetPhoneNumber);
        PhoneNumberIdentifier sourceCallerId = new PhoneNumberIdentifier(appConfig.getAcsPhoneNumber());

        URI callbackUri = URI.create(appConfig.getCallbackUriHost() + "/api/callbacks");
        eventCallbackUri = callbackUri;

        List<CommunicationIdentifier> targets = List.of(target,sourceCallerId);

        CreateCallResult result = client.createGroupCall(targets,callbackUri.toString());
        String connectionId = result.getCallConnectionProperties().getCallConnectionId();
        log.info("Created group call with connection id: {}", connectionId);
    }

    @PostMapping("/connectRoomCallAsync")
    public ResponseEntity<String> connectRoomCallAsync(@RequestParam String roomId) {
        return connectCallAsync(new RoomCallLocator(roomId), "ConnectRoomCallContext");
    }

    @PostMapping("/connectRoomCall")
    public ResponseEntity<String> connectRoomCall(@RequestParam String roomId) {
        return connectCall(new RoomCallLocator(roomId), "ConnectRoomCallContext");
    }

    @PostMapping("/connectGroupCallAsync")
    public ResponseEntity<String> connectGroupCallAsync(@RequestParam String groupId) {
        return connectCallAsync(new GroupCallLocator(groupId), "ConnectGroupCallContext");
    }

    @PostMapping("/connectGroupCall")
    public ResponseEntity<String> connectGroupCall(@RequestParam String groupId) {
        return connectCall(new GroupCallLocator(groupId), "ConnectGroupCallContext");
    }

    @PostMapping("/connectOneToNCallAsync")
    public ResponseEntity<String> connectOneToNCallAsync(@RequestParam String serverCallId) {
        return connectCallAsync(new ServerCallLocator(serverCallId), "ConnectOneToNCallContext");
    }

    @PostMapping("/connectOneToNCall")
    public ResponseEntity<String> connectOneToNCall(@RequestParam String serverCallId) {
        return connectCall(new ServerCallLocator(serverCallId), "ConnectOneToNCallContext");
    }

    private ResponseEntity<String> connectCall(CallLocator locator, String context) {
        try {
            URI callbackUri = new URI(appConfig.getCallbackUriHost() + "/api/callbacks");
            ConnectCallResult result = client.connectCall(locator,callbackUri.toString());
            log.info("Connected sync call with connection ID: {}", result.getCallConnectionProperties().getCallConnectionId());
            return ResponseEntity.ok("Call connected successfully");

        } catch (Exception e) {
            log.error("Error connecting call: {}", e.getMessage());
            return ResponseEntity.status(500).body("Call connection failed");
        }
    }

    private ResponseEntity<String> connectCallAsync(CallLocator locator, String context) {
        try {
            String callbackUri = appConfig.getCallbackUriHost() + "/api/callbacks";
            String websocketUri = appConfig.getCallbackUriHost().replace("https", "wss") + "/ws";

            MediaStreamingOptions mediaOptions = new MediaStreamingOptions(websocketUri, MediaStreamingTransport.WEBSOCKET, MediaStreamingContent.AUDIO,
                    MediaStreamingAudioChannel.UNMIXED, false);

            TranscriptionOptions transcriptionOptions = new TranscriptionOptions(websocketUri, TranscriptionTransport.WEBSOCKET,
                    "en-us", false);

            ConnectCallOptions options = new ConnectCallOptions(locator, callbackUri)
                    .setOperationContext(context)
                   // .setCallIntelligenceOptions(new CallIntelligenceOptions().setCognitiveServicesEndpoint(appConfig.getCognitiveServiceEndpoint()))
                    .setMediaStreamingOptions(mediaOptions)
                    .setTranscriptionOptions(transcriptionOptions);

        Mono<Response<ConnectCallResult>> response  = asyncClient.connectCallWithResponse(options);
            response.subscribe(res -> {
                if (res.getStatusCode() == 200) {
                    log.info("Connected async call. Connection ID: {}", res.getValue().getCallConnectionProperties().getCallConnectionId());
                } else {
                    log.error("Call async connection failed with status code: {}", res.getStatusCode());
                }
            });
            return ResponseEntity.ok("Async call request sent");

        } catch (Exception e) {
            log.error("Error connecting async call: {}", e.getMessage());
            return ResponseEntity.status(500).body("Async call connection failed");
        }
    }

    // region Add PSTN Participant
    @PostMapping("/addPstnParticipantAsync")
    public ResponseEntity<Object> addPstnParticipantAsync(@RequestParam String pstnParticipant) {
        CallConnection  callConnectionService = getConnection();
        CallInvite callInvite = new CallInvite(
                new PhoneNumberIdentifier(pstnParticipant),
                new PhoneNumberIdentifier("acsPhoneNumber")); // Replace with actual ACS number
        AddParticipantOptions options = new AddParticipantOptions(callInvite);
        options.setOperationContext("addPstnUserContext");
        options.setInvitationTimeout(Duration.ofSeconds(15));
        Object result = callConnectionService.addParticipantWithResponse(options,Context.NONE);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/addPstnParticipant")
    public ResponseEntity<Object> addPstnParticipant(@RequestParam String pstnParticipant) {
        CallConnection  callConnectionService = getConnection();
        CallInvite callInvite = new CallInvite(
                new PhoneNumberIdentifier(pstnParticipant),
                new PhoneNumberIdentifier(appConfig.getAcsPhoneNumber())).setSourceCallerIdNumber(new PhoneNumberIdentifier(appConfig.getAcsPhoneNumber())); // Replace with actual ACS number
        Object result = callConnectionService.addParticipant(callInvite);
        return ResponseEntity.ok(result);
    }

    // region Add ACS Participant
    @PostMapping("/addAcsParticipantAsync")
    public ResponseEntity<Object> addAcsParticipantAsync(@RequestParam String acsParticipant) {
        CallInvite callInvite = new CallInvite(new CommunicationUserIdentifier(acsParticipant));
        AddParticipantOptions options = new AddParticipantOptions(callInvite);
        options.setOperationContext("addAcsUserContext");
        options.setInvitationTimeout(Duration.ofSeconds(15));

       
        CallConnection  callConnectionService = getConnection();
        Object result = callConnectionService.addParticipantWithResponse(options, Context.NONE);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/addAcsParticipant")
    public ResponseEntity<Object> addAcsParticipant(@RequestParam String acsParticipant) {
        CallInvite callInvite = new CallInvite(new CommunicationUserIdentifier(acsParticipant));
        CallConnection  callConnectionService = getConnection();
        Object result = callConnectionService.addParticipant(callInvite);
        return ResponseEntity.ok(result);
    }

    // region Add Teams Participant
    @PostMapping("/addTeamsParticipantAsync")
    public ResponseEntity<Object> addTeamsParticipantAsync(@RequestParam String teamsObjectId) {
        CallInvite callInvite = new CallInvite(new MicrosoftTeamsUserIdentifier(teamsObjectId));
        AddParticipantOptions options = new AddParticipantOptions(callInvite);
        options.setOperationContext("addTeamsUserContext");
        options.setInvitationTimeout(Duration.ofSeconds(15));
        CallConnection  callConnectionService = getConnection();
        Object result = callConnectionService.addParticipantWithResponse(options, Context.NONE);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/addTeamsParticipant")
    public ResponseEntity<Object> addTeamsParticipant(@RequestParam String teamsObjectId) {
        CallInvite callInvite = new CallInvite(new MicrosoftTeamsUserIdentifier(teamsObjectId));
        CallConnection  callConnectionService = getConnection();
        Object result = callConnectionService.addParticipant(callInvite);
        return ResponseEntity.ok(result);
    }

    // region Remove PSTN Participant
    @PostMapping("/removePstnParticipantAsync")
    public ResponseEntity<Void> removePstnParticipantAsync(@RequestParam String pstnTarget) {
        RemoveParticipantOptions options = new RemoveParticipantOptions(new PhoneNumberIdentifier(pstnTarget));
        options.setOperationContext("removePstnParticipantContext");
        CallConnection  callConnectionService = getConnection();
        callConnectionService.removeParticipantWithResponse(options,Context.NONE);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/removePstnParticipant")
    public ResponseEntity<Void> removePstnParticipant(@RequestParam String pstnTarget) {
        CallConnection  callConnectionService = getConnection();
        callConnectionService.removeParticipant(new PhoneNumberIdentifier(pstnTarget));
        return ResponseEntity.ok().build();
    }

    // region Remove ACS Participant
    @PostMapping("/removeAcsParticipantAsync")
    public ResponseEntity<Void> removeAcsParticipantAsync(@RequestParam String acsTarget) {
        RemoveParticipantOptions options = new RemoveParticipantOptions(new CommunicationUserIdentifier(acsTarget));
        options.setOperationContext("removeAcsParticipantContext");
        CallConnection  callConnectionService = getConnection();
        callConnectionService.removeParticipantWithResponse(options,Context.NONE);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/removeAcsParticipant")
    public ResponseEntity<Void> removeAcsParticipant(@RequestParam String acsTarget) {
        CallConnection  callConnectionService = getConnection();
        callConnectionService.removeParticipant(new CommunicationUserIdentifier(acsTarget));
        return ResponseEntity.ok().build();
    }

    // region Remove Teams Participant
    @PostMapping("/removeTeamsParticipantAsync")
    public ResponseEntity<Void> removeTeamsParticipantAsync(@RequestParam String teamsObjectId) {
        RemoveParticipantOptions options = new RemoveParticipantOptions(new MicrosoftTeamsUserIdentifier(teamsObjectId));
        options.setOperationContext("removeTeamsParticipantContext");
        CallConnection  callConnectionService = getConnection();
        callConnectionService.removeParticipantWithResponse(options,Context.NONE);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/removeTeamsParticipant")
    public ResponseEntity<Void> removeTeamsParticipant(@RequestParam String teamsObjectId) {
        CallConnection  callConnectionService = getConnection();
        callConnectionService.removeParticipant(new MicrosoftTeamsUserIdentifier(teamsObjectId));
        return ResponseEntity.ok().build();
    }

    // region Cancel Add Participant
    @PostMapping("/cancelAddParticipantAsync")
    public ResponseEntity<Object> cancelAddParticipantAsync(@RequestParam String invitationId) {
        CancelAddParticipantOperationOptions options = new CancelAddParticipantOperationOptions(invitationId);
        options.setOperationContext("CancelAddingParticipantContext");
        CallConnection  callConnectionService = getConnection();
        Object result = callConnectionService.cancelAddParticipantOperationWithResponse(options,Context.NONE);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/cancelAddParticipant")
    public ResponseEntity<Object> cancelAddParticipant(@RequestParam String invitationId) {
        CallConnection  callConnectionService = getConnection();
        Object result = callConnectionService.cancelAddParticipantOperation(invitationId);
        return ResponseEntity.ok(result);
    }
   private TextSource createTextSource(String message) {
    var textSource = new TextSource()
    .setText(message)
    .setVoiceName("en-US-NancyNeural");
        return textSource;
    }

    @PostMapping("/playTextSourceToPstnTargetAsync")
    public ResponseEntity<Void> playTextSourceToPstnTargetAsync(@RequestParam String pstnTarget) {
        CallMedia callMedia = getCallMedia();
        TextSource textSource = createTextSource("Hi, this is test source played through play source thanks. Goodbye!.");
        List<CommunicationIdentifier> playTo = Collections.singletonList(new PhoneNumberIdentifier(pstnTarget));

        PlayOptions options = new PlayOptions(textSource, playTo);
        options.setOperationContext("playToContext");
        callMedia.playWithResponse(options,Context.NONE);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/playTextSourceToPstnTarget")
    public ResponseEntity<Void> playTextSourceToPstnTarget(@RequestParam String pstnTarget) {
        CallMedia callMedia = getCallMedia();
        TextSource textSource = createTextSource("Hi, this is test source played through play source thanks. Goodbye!.");
        List<CommunicationIdentifier> playTo = Collections.singletonList(new PhoneNumberIdentifier(pstnTarget));
        callMedia.play(textSource,playTo);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/playTextSourceAcsTargetAsync")
    public ResponseEntity<Void> playTextSourceAcsTargetAsync(@RequestParam String acsTarget) {
        CallMedia callMedia = getCallMedia();
        List<CommunicationIdentifier> playTo = Collections.singletonList(new PhoneNumberIdentifier(acsTarget));
        TextSource textSource = createTextSource("Hi, this is test source played through play source thanks. Goodbye!.");
        PlayOptions options = new PlayOptions(textSource,playTo);
        options.setOperationContext("playToContext");
        callMedia.playWithResponse(options,Context.NONE);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/playTextSourceToAcsTarget")
    public ResponseEntity<Void> playTextSourceToAcsTarget(@RequestParam String acsTarget) {
        CallMedia callMedia = getCallMedia();
        TextSource textSource = createTextSource("Hi, this is test source played through play source thanks. Goodbye!.");
        callMedia.play(textSource,Collections.singletonList(new PhoneNumberIdentifier(acsTarget)));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/playTextSourceToTeamsTargetAsync")
    public ResponseEntity<Void> playTextSourceToTeamsTargetAsync(@RequestParam String teamsObjectId) {
        CallMedia callMedia = getCallMedia();
        TextSource textSource = createTextSource("Hi, this is test source played through play source thanks. Goodbye!.");
        PlayOptions options = new PlayOptions(textSource, Collections.singletonList(new MicrosoftTeamsUserIdentifier(teamsObjectId)));
        options.setOperationContext("playToContext");
        callMedia.playWithResponse(options,Context.NONE);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/playTextSourceToTeamsTarget")
    public ResponseEntity<Void> playTextSourceToTeamsTarget(@RequestParam String teamsObjectId) {
        CallMedia callMedia = getCallMedia();
        TextSource textSource = createTextSource("Hi, this is test source played through play source thanks. Goodbye!.");
        callMedia.play(textSource, Collections.singletonList(new MicrosoftTeamsUserIdentifier(teamsObjectId)));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/playTextSourceToAllAsync")
    public ResponseEntity<Void> playTextSourceToAllAsync() {
        CallMedia callMedia = getCallMedia();
        TextSource textSource = createTextSource("Hi, this is test source played through play source thanks. Goodbye!.");
        PlayToAllOptions options = new PlayToAllOptions(textSource);
        options.setOperationContext("playToAllContext");
        callMedia.playToAllWithResponse(options,Context.NONE);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/playTextSourceToAll")
    public ResponseEntity<Void> playTextSourceToAll() {
        CallMedia callMedia = getCallMedia();
        TextSource textSource = createTextSource("Hi, this is test source played through play source thanks. Goodbye!.");
        PlayToAllOptions options = new PlayToAllOptions(textSource);
        options.setOperationContext("playToAllContext");
        callMedia.playToAll(textSource);
        return ResponseEntity.ok().build();
    }

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
    private static final String SSML_STRING = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>" +
        "<voice name='en-US-JennyNeural'>Hi, this is %s test played through SSML source. Goodbye!</voice></speak>";
    public SsmlSource createSsmlSource(boolean bargeIn) {
        String bargeText = bargeIn ? "barge in" : "test";
        SsmlSource ssmlSource = new SsmlSource();
        ssmlSource.setSsmlText(String.format(SSML_STRING, bargeText));
        return ssmlSource;
    }
    // 1. PSTN - Async
    @PostMapping("/playSsmlSourceToPstnTargetAsync")
    public ResponseEntity<Void> playSsmlSourceToPstnTargetAsync(@RequestParam String pstnTarget) {
        return playSsml(pstnTarget, TargetType.PSTN, true, false);
    }

    // 2. PSTN - Sync
    @PostMapping("/playSsmlSourceToPstnTarget")
    public ResponseEntity<Void> playSsmlSourceToPstnTarget(@RequestParam String pstnTarget) {
        return playSsml(pstnTarget, TargetType.PSTN, false, false);
    }

    // 3. ACS - Async
    @PostMapping("/playSsmlSourceAcsTargetAsync")
    public ResponseEntity<Void> playSsmlSourceAcsTargetAsync(@RequestParam String acsTarget) {
        return playSsml(acsTarget, TargetType.ACS, true, false);
    }

    // 4. ACS - Sync
    @PostMapping("/playSsmlSourceToAcsTarget")
    public ResponseEntity<Void> playSsmlSourceToAcsTarget(@RequestParam String acsTarget) {
        return playSsml(acsTarget, TargetType.ACS, false, false);
    }

    // 5. Teams - Async
    @PostMapping("/playSsmlSourceToTeamsTargetAsync")
    public ResponseEntity<Void> playSsmlSourceToTeamsTargetAsync(@RequestParam String teamsUserId) {
        return playSsml(teamsUserId, TargetType.TEAMS, true, false);
    }

    // 6. Teams - Sync
    @PostMapping("/playSsmlSourceToTeamsTarget")
    public ResponseEntity<Void> playSsmlSourceToTeamsTarget(@RequestParam String teamsUserId) {
        return playSsml(teamsUserId, TargetType.TEAMS, false, false);
    }

    // 7. All - Async
    @PostMapping("/playSsmlSourceToAllAsync")
    public ResponseEntity<Void> playSsmlSourceToAllAsync() {
        return playSsml(null, TargetType.ALL, true, false);
    }

    // 8. All - Sync
    @PostMapping("/playSsmlSourceToAll")
    public ResponseEntity<Void> playSsmlSourceToAll() {
        return playSsml(null, TargetType.ALL, false, false);
    }

    // 9. Barge-In - Async
    @PostMapping("/playSsmlSourceBargeInAsync")
    public ResponseEntity<Void> playSsmlSourceBargeInAsync() {
        return playSsml(null, TargetType.ALL, true, true);
    }

    // 🔄 Shared Method
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
                options.setInterruptHoldAudio(bargeIn);

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

    // 1. PSTN - Async
    @PostMapping("/playFileSourceToPstnTargetAsync")
    public ResponseEntity<Void> playFileSourceToPstnTargetAsync(@RequestParam String pstnTarget) {
        return playFile(pstnTarget, TargetType.PSTN, true, false);
    }

    // 2. PSTN - Sync
    @PostMapping("/playFileSourceToPstnTarget")
    public ResponseEntity<Void> playFileSourceToPstnTarget(@RequestParam String pstnTarget) {
        return playFile(pstnTarget, TargetType.PSTN, false, false);
    }

    // 3. ACS - Async
    @PostMapping("/playFileSourceToAcsTargetAsync")
    public ResponseEntity<Void> playFileSourceToAcsTargetAsync(@RequestParam String acsTarget) {
        return playFile(acsTarget, TargetType.ACS, true, false);
    }

    // 4. ACS - Sync
    @PostMapping("/playFileSourceToAcsTarget")
    public ResponseEntity<Void> playFileSourceToAcsTarget(@RequestParam String acsTarget) {
        return playFile(acsTarget, TargetType.ACS, false, false);
    }

    // 5. Teams - Async
    @PostMapping("/playFileSourceToTeamsTargetAsync")
    public ResponseEntity<Void> playFileSourceToTeamsTargetAsync(@RequestParam String teamsUserId) {
        return playFile(teamsUserId, TargetType.TEAMS, true, false);
    }

    // 6. Teams - Sync
    @PostMapping("/playFileSourceToTeamsTarget")
    public ResponseEntity<Void> playFileSourceToTeamsTarget(@RequestParam String teamsUserId) {
        return playFile(teamsUserId, TargetType.TEAMS, false, false);
    }

    // 7. All - Async
    @PostMapping("/playFileSourceToAllAsync")
    public ResponseEntity<Void> playFileSourceToAllAsync() {
        return playFile(null, TargetType.ALL, true, false);
    }

    // 8. All - Sync
    @PostMapping("/playFileSourceToAll")
    public ResponseEntity<Void> playFileSourceToAll() {
        return playFile(null, TargetType.ALL, false, false);
    }

    // 9. Barge-In - Async
    @PostMapping("/playFileSourceBargeInAsync")
    public ResponseEntity<Void> playFileSourceBargeInAsync() {
        return playFile(null, TargetType.ALL, true, true);
    }

    // Shared Logic
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
                options.setInterruptHoldAudio(bargeIn);
    
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
    @PostMapping("/recognizeDTMFAsync")
    public ResponseEntity<Void> recognizeDTMFAsync(@RequestParam String pstnTarget) {
        return startDtmfRecognition(pstnTarget, true);
    }

    @PostMapping("/recognizeDTMF")
    public ResponseEntity<Void> recognizeDTMF(@RequestParam String pstnTarget) {
        return startDtmfRecognition(pstnTarget, false);
    }

    @PostMapping("/recognizeSpeechAsync")
    public ResponseEntity<Void> recognizeSpeechAsync(@RequestParam String pstnTarget) {
        return startSpeechRecognition(pstnTarget, true);
    }

    @PostMapping("/recognizeSpeech")
    public ResponseEntity<Void> recognizeSpeech(@RequestParam String pstnTarget) {
        return startSpeechRecognition(pstnTarget, false);
    }

    @PostMapping("/recognizeSpeechOrDtmfAsync")
    public ResponseEntity<Void> recognizeSpeechOrDtmfAsync(@RequestParam String pstnTarget) {
        return startSpeechOrDtmfRecognition(pstnTarget, true);
    }

    @PostMapping("/recognizeSpeechOrDtmf")
    public ResponseEntity<Void> recognizeSpeechOrDtmf(@RequestParam String pstnTarget) {
        return startSpeechOrDtmfRecognition(pstnTarget, false);
    }

    @PostMapping("/recognizeChoiceAsync")
    public ResponseEntity<Void> recognizeChoiceAsync(@RequestParam String pstnTarget) {
        return startChoiceRecognition(pstnTarget, true);
    }

    @PostMapping("/recognizeChoice")
    public ResponseEntity<Void> recognizeChoice(@RequestParam String pstnTarget) {
        return startChoiceRecognition(pstnTarget, false);
    }

    // 🔁 Reusable helper methods below
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
   // Async Equivalent: /sendDTMFTonesAsync (C#)
   @PostMapping("/sendDTMFTonesAsync")
   public ResponseEntity<String> sendDTMFTonesAsync(@RequestParam String pstnTarget) {
       CommunicationIdentifier target = new PhoneNumberIdentifier(pstnTarget);
       List<DtmfTone> tones = Arrays.asList(DtmfTone.ZERO, DtmfTone.ONE);
       CallMedia callMediaService = getCallMedia();
       SendDtmfTonesResult result = callMediaService.sendDtmfTones(tones, target); // .block() internally

       log.info("Async DTMF tones sent to {}", pstnTarget);
       return ResponseEntity.ok("DTMF tones sent (async simulation).");
   }

   // Sync Equivalent: /sendDTMFTones (C#)
   @PostMapping("/sendDTMFTones")
   public ResponseEntity<String> sendDTMFTones(@RequestParam String pstnTarget) {
       CommunicationIdentifier target = new PhoneNumberIdentifier(pstnTarget);
       List<DtmfTone> tones = Arrays.asList(DtmfTone.ZERO, DtmfTone.ONE);
       CallMedia callMediaService = getCallMedia();
       SendDtmfTonesResult result = callMediaService.sendDtmfTones(tones, target);

       log.info("DTMF tones sent to {}", pstnTarget);
       return ResponseEntity.ok("DTMF tones sent.");
   }

   // Async Equivalent: /startContinuousDTMFTonesAsync (C#)
   @PostMapping("/startContinuousDTMFTonesAsync")
   public ResponseEntity<String> startContinuousDTMFTonesAsync(@RequestParam String pstnTarget) {
       CommunicationIdentifier target = new PhoneNumberIdentifier(pstnTarget);
       CallMedia callMediaService = getCallMedia();
       callMediaService.startContinuousDtmfRecognition(target); // .block() internally

       log.info("Async continuous DTMF started for {}", pstnTarget);
       return ResponseEntity.ok("Started continuous DTMF recognition (async simulation).");
   }

   // Sync Equivalent: /startContinuousDTMFTones (C#)
   @PostMapping("/startContinuousDTMFTones")
   public ResponseEntity<String> startContinuousDTMFTones(@RequestParam String pstnTarget) {
       CommunicationIdentifier target = new PhoneNumberIdentifier(pstnTarget);
       CallMedia callMediaService = getCallMedia();
       callMediaService.startContinuousDtmfRecognition(target);

       log.info("Started continuous DTMF for {}", pstnTarget);
       return ResponseEntity.ok("Started continuous DTMF recognition.");
   }

   // Async Equivalent: /stopContinuousDTMFTonesAsync (C#)
   @PostMapping("/stopContinuousDTMFTonesAsync")
   public ResponseEntity<String> stopContinuousDTMFTonesAsync(@RequestParam String pstnTarget) {
       CommunicationIdentifier target = new PhoneNumberIdentifier(pstnTarget);
       CallMedia callMediaService = getCallMedia();
       callMediaService.stopContinuousDtmfRecognition(target); // .block() internally

       log.info("Async stop continuous DTMF for {}", pstnTarget);
       return ResponseEntity.ok("Stopped continuous DTMF recognition (async simulation).");
   }

   // Sync Equivalent: /stopContinuousDTMFTones (C#)
   @PostMapping("/stopContinuousDTMFTones")
   public ResponseEntity<String> stopContinuousDTMFTones(@RequestParam String pstnTarget) {
       CommunicationIdentifier target = new PhoneNumberIdentifier(pstnTarget);
       CallMedia callMediaService = getCallMedia();
       callMediaService.stopContinuousDtmfRecognition(target);

       log.info("Stopped continuous DTMF for {}", pstnTarget);
       return ResponseEntity.ok("Stopped continuous DTMF recognition.");
   }


   @PostMapping("/holdParticipantAsync")
public ResponseEntity<String> holdParticipantAsync(@RequestParam String pstnTarget,
                                                   @RequestParam boolean isPlaySource) {
    CommunicationIdentifier target = new PhoneNumberIdentifier(pstnTarget);
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

@PostMapping("/holdParticipant")
public ResponseEntity<String> holdParticipant(@RequestParam String pstnTarget,
                                              @RequestParam boolean isPlaySource) {
    CommunicationIdentifier target = new PhoneNumberIdentifier(pstnTarget);
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

@PostMapping("/interruptAudioAndAnnounceAsync")
public ResponseEntity<String> interruptAudioAndAnnounceAsync(@RequestParam String pstnTarget) {
    CommunicationIdentifier target = new PhoneNumberIdentifier(pstnTarget);
    CallMedia callMediaService = getCallMedia();

    TextSource textSource = new TextSource()
            .setText("Hi, this is interrupt audio and announcement test.")
            .setVoiceName("en-US-NancyNeural")
            .setSourceLocale("en-US")
            .setVoiceKind(VoiceKind.MALE);

    InterruptAudioAndAnnounceOptions options = new InterruptAudioAndAnnounceOptions(textSource, target)
            .setOperationContext("interruptContext");

    callMediaService.interruptAudioAndAnnounceWithResponse(options, Context.NONE);
    log.info("InterruptAudioAndAnnounce (async) sent to {}", pstnTarget);
    return ResponseEntity.ok("Interrupt audio and announce sent (async).");
}

@PostMapping("/interruptAudioAndAnnounce")
public ResponseEntity<String> interruptAudioAndAnnounce(@RequestParam String pstnTarget) {
    CommunicationIdentifier target = new PhoneNumberIdentifier(pstnTarget);
    CallMedia callMediaService = getCallMedia();

    TextSource textSource = new TextSource()
            .setText("Hi, this is interrupt audio and announcement test.")
            .setVoiceName("en-US-NancyNeural")
            .setSourceLocale("en-US")
            .setVoiceKind(VoiceKind.MALE);

    callMediaService.interruptAudioAndAnnounce(textSource, target);
    log.info("InterruptAudioAndAnnounce (sync) sent to {}", pstnTarget);
    return ResponseEntity.ok("Interrupt audio and announce sent.");
}

@PostMapping("/unholdParticipantAsync")
public ResponseEntity<String> unholdParticipantAsync(@RequestParam String pstnTarget) {
    CommunicationIdentifier target = new PhoneNumberIdentifier(pstnTarget);
    UnholdOptions unholdOptions = new UnholdOptions(target).setOperationContext("unholdUserContext");
    CallMedia callMediaService = getCallMedia();

    callMediaService.unholdWithResponse(unholdOptions, Context.NONE);
    log.info("Unhold participant asynchronously {}", pstnTarget);
    return ResponseEntity.ok("Participant unheld (async).");
}

@PostMapping("/unholdParticipant")
public ResponseEntity<String> unholdParticipant(@RequestParam String pstnTarget) {
    CommunicationIdentifier target = new PhoneNumberIdentifier(pstnTarget);
    CallMedia callMediaService = getCallMedia();

    callMediaService.unhold(target);
    log.info("Unhold participant synchronously {}", pstnTarget);
    return ResponseEntity.ok("Participant unheld.");
}

@PostMapping("/interruptHoldWithPlayAsync")
public ResponseEntity<String> interruptHoldWithPlayAsync(@RequestParam String pstnTarget) {
    CommunicationIdentifier target = new PhoneNumberIdentifier(pstnTarget);
    CallMedia callMediaService = getCallMedia();

    TextSource textSource = new TextSource()
            .setText("Hi, this is interrupt hold and play test.")
            .setVoiceName("en-US-NancyNeural")
            .setSourceLocale("en-US")
            .setVoiceKind(VoiceKind.MALE);

    List<CommunicationIdentifier> playTo = Collections.singletonList(target);
    PlayOptions playOptions = new PlayOptions(textSource, playTo)
            .setOperationContext("playToContext")
            .setInterruptHoldAudio(true);

    callMediaService.playWithResponse(playOptions, Context.NONE);
    log.info("Interrupt hold with play sent (async) to {}", pstnTarget);
    return ResponseEntity.ok("Interrupt hold with play sent (async).");
}

@PostMapping("/interruptHoldWithPlay")
public ResponseEntity<String> interruptHoldWithPlay(@RequestParam String pstnTarget) {
    CommunicationIdentifier target = new PhoneNumberIdentifier(pstnTarget);
    CallMedia callMediaService = getCallMedia();

    TextSource textSource = new TextSource()
            .setText("Hi, this is interrupt hold and play test.")
            .setVoiceName("en-US-NancyNeural")
            .setSourceLocale("en-US")
            .setVoiceKind(VoiceKind.MALE);

    List<CommunicationIdentifier> playTo = Collections.singletonList(target);
    callMediaService.play(textSource, playTo);

    log.info("Interrupt hold with play sent (sync) to {}", pstnTarget);
    return ResponseEntity.ok("Interrupt hold with play sent.");
}
@PostMapping("/hangupAsync")
    public ResponseEntity<String> hangupAsync(@RequestParam boolean isForEveryOne) {
        CallConnection callConnection = getConnection();

        callConnection.hangUpWithResponse(isForEveryOne, Context.NONE);
        log.info("Call hangup requested (async) forEveryone={}", isForEveryOne);

        return ResponseEntity.ok("Call hangup requested (async).");
    }

    @PostMapping("/hangup")
    public ResponseEntity<String> hangup(@RequestParam boolean isForEveryOne) {
        CallConnection callConnection = getConnection();

        callConnection.hangUp(isForEveryOne);
        log.info("Call hangup requested (sync) forEveryone={}", isForEveryOne);

        return ResponseEntity.ok("Call hangup requested.");
    }

    @PostMapping("/muteAcsParticipantAsync")
    public ResponseEntity<String> muteAcsParticipantAsync(@RequestParam String acsTarget) {
        CommunicationIdentifier target = new CommunicationUserIdentifier(acsTarget);
        CallConnection callConnection = getConnection();

        MuteParticipantOptions options = new MuteParticipantOptions(target)
                .setOperationContext("muteContext");

        // Assuming you're calling a method like muteParticipantWithResponse(options, context)
        callConnection.muteParticipantWithResponse(options, Context.NONE);

        log.info("Muted ACS participant asynchronously: {}", acsTarget);
        return ResponseEntity.ok("Muted ACS participant (async).");
    }

    @PostMapping("/muteAcsParticipant")
    public ResponseEntity<String> muteAcsParticipant(@RequestParam String acsTarget) {
        CommunicationIdentifier target = new CommunicationUserIdentifier(acsTarget);
        CallConnection callConnection = getConnection();

        callConnection.muteParticipant(target); // Synchronous mute using options if method is available
        log.info("Muted ACS participant synchronously: {}", acsTarget);
        return ResponseEntity.ok("Muted ACS participant.");
    }

    @PostMapping("/transferCallToPstnParticipantAsync")
    public ResponseEntity<String> transferCallToPstnParticipantAsync(
            @RequestParam String pstnTransferTarget,
            @RequestParam String pstnTarget) {

        CallConnection callConnection = getConnection();

        TransferCallToParticipantOptions options = new TransferCallToParticipantOptions(new PhoneNumberIdentifier(pstnTransferTarget));
        options.setOperationContext("TransferCallContext");
        options.setTransferee(new PhoneNumberIdentifier(pstnTarget));

        // Async transfer, assuming a method like below exists
        ((Mono<Response<CreateCallResult>>) callConnection.transferCallToParticipantWithResponse(options,Context.NONE))
                      .subscribe(response -> log.info("Call transferred successfully."),
                                 error -> log.error("Transfer failed", error));

        return ResponseEntity.ok("Transfer initiated");
    }

    @PostMapping("/transferCallToPstnParticipant")
    public ResponseEntity<String> transferCallToPstnParticipant(
            @RequestParam String pstnTransferTarget,
            @RequestParam String pstnTarget) {

        CallConnection callConnection = getConnection();


        // Synchronous transfer
        callConnection.transferCallToParticipant(new PhoneNumberIdentifier(pstnTarget));

        return ResponseEntity.ok("Transfer completed");
    }
    @PostMapping("/getPstnParticipantAsync")
    public ResponseEntity<Void> getPstnParticipantAsync(@RequestParam String pstnTarget) {
        CallConnection callConnection = getConnection();
        
        Response<CallParticipant> response = callConnection.getParticipantWithResponse(
            new PhoneNumberIdentifier(pstnTarget),
            Context.NONE
        );
    
        CallParticipant participant = response.getValue();
    
        if (participant != null) {
            log.info("Participant: --> {}", participant.getIdentifier().getRawId());
            log.info("Is Participant on hold: --> {}", participant.isOnHold());
        }
    
        return ResponseEntity.ok().build();
    }
    

    @PostMapping("/getPstnParticipant")
    public ResponseEntity<Void> getPstnParticipant(@RequestParam String pstnTarget) {
        CallConnection callConnection = getConnection();
        CallParticipant participant = callConnection.getParticipant(new PhoneNumberIdentifier(pstnTarget));

        if (participant != null) {
            log.info("Participant: --> {}", participant.getIdentifier().getRawId());
            log.info("Is Participant on hold: --> {}", participant.isOnHold());
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/getAcsParticipantAsync")
    public ResponseEntity<Void> getAcsParticipantAsync(@RequestParam String acsTarget) {
        CallConnection callConnection = getConnection();
    
        Response<CallParticipant> response = callConnection.getParticipantWithResponse(
            new CommunicationUserIdentifier(acsTarget),
            Context.NONE
        );
    
        CallParticipant participant = response.getValue();
    
        if (participant != null) {
            log.info("Participant: --> {}", participant.getIdentifier().getRawId());
            log.info("Is Participant on hold: --> {}", participant.isOnHold());
        } else {
            log.warn("No participant found for ACS identifier: {}", acsTarget);
        }
    
        return ResponseEntity.ok().build();
    }
    

    @PostMapping("/getAcsParticipant")
    public ResponseEntity<Void> getAcsParticipant(@RequestParam String acsTarget) {
        CallConnection callConnection = getConnection();
        CallParticipant participant = callConnection.getParticipant(new CommunicationUserIdentifier(acsTarget));

        if (participant != null) {
            log.info("Participant: --> {}", participant.getIdentifier().getRawId());
            log.info("Is Participant on hold: --> {}", participant.isOnHold());
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/getTeamsParticipantAsync")
    public ResponseEntity<Void> getTeamsParticipantAsync(@RequestParam String teamsObjectId) {
        CallConnection callConnection = getConnection();
    
        // Call the method correctly and extract the participant from the response
        Response<CallParticipant> response = callConnection.getParticipantWithResponse(
            new MicrosoftTeamsUserIdentifier(teamsObjectId),
            Context.NONE
        );
    
        CallParticipant participant = response.getValue();
    
        if (participant != null) {
            log.info("Participant: --> {}", participant.getIdentifier().getRawId());
            log.info("Is Participant on hold: --> {}", participant.isOnHold());
        } else {
            log.warn("No participant found for Teams Object ID: {}", teamsObjectId);
        }
    
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/getTeamsParticipant")
    public ResponseEntity<Void> getTeamsParticipant(@RequestParam String teamsObjectId) {
        CallConnection callConnection = getConnection();
        CallParticipant participant = callConnection.getParticipant(new MicrosoftTeamsUserIdentifier(teamsObjectId));

        if (participant != null) {
            log.info("Participant: --> {}", participant.getIdentifier().getRawId());
            log.info("Is Participant on hold: --> {}", participant.isOnHold());
        }
        return ResponseEntity.ok().build();
    }

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

@PostMapping("/startRecordingWithVideoMp4MixedAsync")
    public void startRecordingWithVideoMp4MixedAsync(@RequestParam boolean isRecordingWithCallConnectionId, @RequestParam boolean isPauseOnStart) {
        CallConnectionProperties properties = getCallConnectionProperties();
        CallLocator locator = new ServerCallLocator(properties.getServerCallId());
        String eventCallbackUri = appConfig.getCallbackUriHost() + "/api/callbacks";
        StartRecordingOptions options = isRecordingWithCallConnectionId ?
                new StartRecordingOptions(properties.getCallConnectionId()) : new StartRecordingOptions(locator);

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

    @PostMapping("/startRecordingWithVideoMp4Mixed")
    public void startRecordingWithVideoMp4Mixed(@RequestParam boolean isRecordingWithCallConnectionId, @RequestParam boolean isPauseOnStart) {
        CallConnectionProperties properties = getCallConnectionProperties();
        String eventCallbackUri = appConfig.getCallbackUriHost() + "/api/callbacks";
        CallLocator locator = new ServerCallLocator(properties.getServerCallId());
        StartRecordingOptions options = isRecordingWithCallConnectionId ?
                new StartRecordingOptions(properties.getCallConnectionId()) : new StartRecordingOptions(locator);

        options.setRecordingContent(RecordingContent.AUDIO_VIDEO);
        options.setRecordingFormat(RecordingFormat.MP4);
        options.setRecordingChannel(RecordingChannel.MIXED);
        options.setRecordingStateCallbackUrl(eventCallbackUri);
        options.setPauseOnStart(isPauseOnStart);
        recordingFileFormat = "mp4";

        recordingId = client.getCallRecording().start(options).getRecordingId();
        log.info("Recording started. RecordingId: {}", recordingId);
    }

    @PostMapping("/startRecordingWithAudioMp3MixedAsync")
    public void startRecordingWithAudioMp3MixedAsync(@RequestParam boolean isRecordingWithCallConnectionId, @RequestParam boolean isPauseOnStart) {
        CallConnectionProperties properties = getCallConnectionProperties();
        CallLocator locator = new ServerCallLocator(properties.getServerCallId());
        String eventCallbackUri = appConfig.getCallbackUriHost() + "/api/callbacks";
        StartRecordingOptions options = isRecordingWithCallConnectionId ?
                new StartRecordingOptions(properties.getCallConnectionId()) : new StartRecordingOptions(locator);

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

    @PostMapping("/startRecordingWithAudioMp3Mixed")
    public void startRecordingWithAudioMp3Mixed(@RequestParam boolean isRecordingWithCallConnectionId, @RequestParam boolean isPauseOnStart) {
        CallConnectionProperties properties = getCallConnectionProperties();
        String eventCallbackUri = appConfig.getCallbackUriHost() + "/api/callbacks";
        CallLocator locator = new ServerCallLocator(properties.getServerCallId());
        StartRecordingOptions options = isRecordingWithCallConnectionId ?
                new StartRecordingOptions(properties.getCallConnectionId()) : new StartRecordingOptions(locator);

        options.setRecordingContent(RecordingContent.AUDIO);
        options.setRecordingFormat(RecordingFormat.MP3);
        options.setRecordingChannel(RecordingChannel.MIXED);
        options.setRecordingStateCallbackUrl(eventCallbackUri);
        options.setPauseOnStart(isPauseOnStart);
        recordingFileFormat = "mp4";

        recordingId = client.getCallRecording().start(options).getRecordingId();
        log.info("Recording started. RecordingId: {}", recordingId);
    }

    @PostMapping("/startRecordingWithAudioMp3UnMixedAsync")
    public void startRecordingWithAudioMp3UnMixedAsync(@RequestParam boolean isRecordingWithCallConnectionId, @RequestParam boolean isPauseOnStart) {
        CallConnectionProperties properties = getCallConnectionProperties();
        CallLocator locator = new ServerCallLocator(properties.getServerCallId());
        String eventCallbackUri = appConfig.getCallbackUriHost() + "/api/callbacks";
        StartRecordingOptions options = isRecordingWithCallConnectionId ?
                new StartRecordingOptions(properties.getCallConnectionId()) : new StartRecordingOptions(locator);

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

    @PostMapping("/startRecordingWithAudioMp3Unmixed")
    public void startRecordingWithAudioMp3Unmixed(@RequestParam boolean isRecordingWithCallConnectionId, @RequestParam boolean isPauseOnStart) {
        CallConnectionProperties properties = getCallConnectionProperties();
        String eventCallbackUri = appConfig.getCallbackUriHost() + "/api/callbacks";
        CallLocator locator = new ServerCallLocator(properties.getServerCallId());
        StartRecordingOptions options = isRecordingWithCallConnectionId ?
                new StartRecordingOptions(properties.getCallConnectionId()) : new StartRecordingOptions(locator);

        options.setRecordingContent(RecordingContent.AUDIO);
        options.setRecordingFormat(RecordingFormat.MP3);
        options.setRecordingChannel(RecordingChannel.UNMIXED);
        options.setRecordingStateCallbackUrl(eventCallbackUri);
        options.setPauseOnStart(isPauseOnStart);
        recordingFileFormat = "mp4";

        recordingId = client.getCallRecording().start(options).getRecordingId();
        log.info("Recording started. RecordingId: {}", recordingId);
    }
    @PostMapping("/startRecordingWithAudioWavUnMixedAsync")
    public void startRecordingWithAudioWavUnMixedAsync(@RequestParam boolean isRecordingWithCallConnectionId, @RequestParam boolean isPauseOnStart) {
        CallConnectionProperties properties = getCallConnectionProperties();
        CallLocator locator = new ServerCallLocator(properties.getServerCallId());
        String eventCallbackUri = appConfig.getCallbackUriHost() + "/api/callbacks";
        StartRecordingOptions options = isRecordingWithCallConnectionId ?
                new StartRecordingOptions(properties.getCallConnectionId()) : new StartRecordingOptions(locator);

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

    @PostMapping("/startRecordingWithAudioWavUnmixed")
    public void startRecordingWithAudioWavUnmixed(@RequestParam boolean isRecordingWithCallConnectionId, @RequestParam boolean isPauseOnStart) {
        CallConnectionProperties properties = getCallConnectionProperties();
        String eventCallbackUri = appConfig.getCallbackUriHost() + "/api/callbacks";
        CallLocator locator = new ServerCallLocator(properties.getServerCallId());
        StartRecordingOptions options = isRecordingWithCallConnectionId ?
                new StartRecordingOptions(properties.getCallConnectionId()) : new StartRecordingOptions(locator);

        options.setRecordingContent(RecordingContent.AUDIO);
        options.setRecordingFormat(RecordingFormat.WAV);
        options.setRecordingChannel(RecordingChannel.UNMIXED);
        options.setRecordingStateCallbackUrl(eventCallbackUri);
        options.setPauseOnStart(isPauseOnStart);
        recordingFileFormat = "mp4";

        recordingId = client.getCallRecording().start(options).getRecordingId();
        log.info("Recording started. RecordingId: {}", recordingId);
    }

    @PostMapping("/pauseRecordingAsync")
    public void pauseRecordingAsync() {
        client.getCallRecording().pauseWithResponse(recordingId, null);
    }

    @PostMapping("/pauseRecording")
    public void pauseRecording() {
        client.getCallRecording().pause(recordingId);
    }

    @PostMapping("/resumeRecordingAsync")
    public void resumeRecordingAsync() {
        client.getCallRecording().resumeWithResponse(recordingId, null);
    }

    @PostMapping("/resumeRecording")
    public void resumeRecording() {
        client.getCallRecording().resume(recordingId);
    }

    @PostMapping("/stopRecordingAsync")
    public void stopRecordingAsync() {
        client.getCallRecording().stopWithResponse(recordingId,null);
    }

    @PostMapping("/stopRecording")
    public void stopRecording() {
        client.getCallRecording().stop(recordingId);
    }

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
    // <editor-fold desc=" Cancel All Media Operations">
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
 // </editor-fold>

    @PostMapping("/startDialogAsync")
    public ResponseEntity<Void> startDialogAsync() {
        try {
            Map<String, Object> dialogContext = new HashMap<>();
    
            PhoneNumberIdentifier target = new PhoneNumberIdentifier(calleeId);
            PhoneNumberIdentifier sourceCallerId = new PhoneNumberIdentifier(callerId);
    
            String callbackUri = callbackUriHost + "/api/callbacks";
            CallInvite callInvite = new CallInvite(target, sourceCallerId);
            CreateCallOptions createCallOptions = new CreateCallOptions(callInvite, callbackUri);
    
            CreateCallResult createCallResult = client
                .createCallWithResponse(createCallOptions, Context.NONE)
                .getValue();
    
            CallConnection callConnection = getConnection(); // You need to implement this
            CallDialog callDialog = callConnection.getCallDialog();
    
            log.info("Async call created with ID: {}", createCallResult.getCallConnectionProperties().getCallConnectionId());
    
            String botAppId = botRouting.getOrDefault(calleeId, defaultBotId);
    
            // Create DialogInputType implementation
            // PowerVirtualAgentsDialog dialogInputType = new PowerVirtualAgentsDialog()
            //     .setBotAppId(botAppId)
            //     .setContext(dialogContext);

            DialogInputType dialogInputType = new DialogInputType();
            //  .setBotAppId(botAppId)
             // .setContext(dialogContext);
    
            // FIX: Add a unique dialogId
            String dialogId = UUID.randomUUID().toString();
            
            StartDialogOptions dialogOptions = new StartDialogOptions(dialogId, dialogInputType, dialogContext);
            dialogOptions.setOperationContext("DialogStart");
    
            CompletableFuture.runAsync(() -> {
                try {
                    Response<DialogStateResult> response = callDialog.startDialogWithResponse(dialogOptions, Context.NONE);
                    DialogStateResult result = response.getValue();
                    int statusCode = response.getStatusCode();
                    
                    log.info("Dialog started asynchronously. Status code: {}, Result: {}", statusCode, result);
                } catch (Exception e) {
                    log.error("Error starting dialog async", e);
                }
            });
    
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            log.error("Error in /startDialogAsync", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/startDialog")
    public ResponseEntity<Void> startDialog() {
        try {
            Map<String, Object> dialogContext = new HashMap<>();

            PhoneNumberIdentifier target = new PhoneNumberIdentifier(calleeId);
            PhoneNumberIdentifier sourceCallerId = new PhoneNumberIdentifier(callerId);

            String callbackUri = callbackUriHost + "/api/callbacks";
            CallInvite callInvite = new CallInvite(target, sourceCallerId);

            CreateCallResult createCallResult = client.createCall(callInvite, callbackUri);
            CallConnection callConnection = getConnection(); // Implement this method
            CallDialog callDialog = callConnection.getCallDialog();

            log.info("Call created with ID: {}", createCallResult.getCallConnectionProperties().getCallConnectionId());

            String botAppId = botRouting.getOrDefault(calleeId, defaultBotId);
            // PowerVirtualAgentsDialog dialogInputType = new PowerVirtualAgentsDialog()
            // .setBotAppId(botAppId)
            // .setContext(dialogContext);
            DialogInputType dialogInputType = new DialogInputType();
            //  .setBotAppId(botAppId)
             // .setContext(dialogContext);
             
            String dialogId = UUID.randomUUID().toString();
            StartDialogOptions dialogOptions = new StartDialogOptions(dialogId, dialogInputType, dialogContext);
            dialogOptions.setOperationContext("DialogStart");

            DialogStateResult response = callDialog.startDialog(dialogOptions);

           log.info("Dialog started synchronously. Dialog ID: {}, Dialog State: {}",response.getDialogId()); // assuming getter exists

            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            log.error("Error in /startDialog", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private enum TargetType {
        PSTN,
        ACS,
        TEAMS,
        ALL
    }

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

        private CallAutomationAsyncClient initAsyncClient() {
        CallAutomationAsyncClient client;
        try {
            client = new CallAutomationClientBuilder()
                    .connectionString(appConfig.getAcsConnectionString())
                    .buildAsyncClient();
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
