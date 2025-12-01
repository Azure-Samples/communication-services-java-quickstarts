package com.communication.callautomation;

import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.CallConnection;
import com.azure.communication.callautomation.CallMedia;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.SystemEventNames;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationEventData;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationResponse;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

/**
 * REST controller for handling Call Automation operations for the Lobby Call
 * Sample.
 * This controller manages incoming calls, lobby calls, target calls, and
 * participant transfers.
 */
@RestController
public class ProgramSample {

    private static final Logger log = LoggerFactory.getLogger(ProgramSample.class);

    private static final String LOBBY_USER_MESSAGE = "You are currently in a lobby call, we will notify the admin that you are waiting.";

    private final AcsConfiguration acsConfiguration;
    private final LobbyWebSocketHandler lobbyWebSocketHandler;
    private CallAutomationClient acsClient;

    // Call state variables
    private String lobbyCallConnectionId = "";
    private String targetCallConnectionId = "";
    private String lobbyCallerId = "";

    /**
     * Constructor with dependency injection.
     * 
     * @param acsConfiguration      Configuration properties from application.yml
     * @param lobbyWebSocketHandler WebSocket handler for lobby interactions
     */
    @Autowired
    public ProgramSample(AcsConfiguration acsConfiguration, LobbyWebSocketHandler lobbyWebSocketHandler) {
        this.acsConfiguration = acsConfiguration;
        this.lobbyWebSocketHandler = lobbyWebSocketHandler;
    }

    /**
     * Initializes the application after dependency injection.
     * Validates configuration and initializes the Call Automation client.
     */
    @PostConstruct
    public void init() {
        // Validate configuration
        try {
            acsConfiguration.validate();
            log.info("Configuration validated successfully");
        } catch (IllegalArgumentException e) {
            log.error("Configuration validation failed: {}", e.getMessage());
            throw e;
        }

        // Initialize Call Automation client
        this.acsClient = initClient(acsConfiguration.getAcsConnectionString(), acsConfiguration.getPmaEndpoint());
        if (this.acsClient == null) {
            throw new IllegalStateException("Failed to initialize Call Automation client");
        }
        log.info("Call Automation client initialized successfully");

        // Set up the connection between ProgramSample and LobbyWebSocketHandler
        if (lobbyWebSocketHandler != null) {
            lobbyWebSocketHandler.setProgramSample(this);
        }
    }

    // Method to handle WebSocket messages (called by LobbyWebSocketHandler)
    public void handleWebSocketMessage(WebSocketSession session, TextMessage message) {
        String jsResponse = message.getPayload();
        log.info("Received from JS: " + jsResponse);

        if ("yes".equalsIgnoreCase(jsResponse.trim())) {
            log.info("TODO: Move Participant");
            try {
                log.info(
                        "\n~~~~~~~~~~~~  /api/callbacks ~~~~~~~~~~~~\n" +
                                "Move Participant operation started..\n" +
                                "Source Caller Id:     " + lobbyCallerId + "\n" +
                                "Source Connection Id: " + lobbyCallConnectionId + "\n" +
                                "Target Connection Id: " + targetCallConnectionId + "\n");

                CallConnection targetConnection = acsClient.getCallConnection(targetCallConnectionId);
                // CallConnection sourceConnection =
                // client.getCallConnection(lobbyConnectionId.get());

                CommunicationIdentifier participantToMove;
                if (lobbyCallerId.startsWith("+")) {
                    participantToMove = new PhoneNumberIdentifier(lobbyCallerId);
                } else if (lobbyCallerId.startsWith("8:acs")) {
                    participantToMove = new CommunicationUserIdentifier(lobbyCallerId);
                } else {
                    log.error("Invalid participant identifier");
                    return;
                }

                targetConnection.moveParticipants(java.util.Collections.singletonList(participantToMove),
                        lobbyCallConnectionId);
                // If no exception is thrown, the operation is considered successful
                log.info("Move Participants operation completed successfully.");
            } catch (Exception ex) {
                log.info("Error in manual move participants operation: " + ex.getMessage());
            }
        }
    }

    private ResponseEntity<Object> handleSubscriptionValidation(final BinaryData eventData) {
        try {
            log.info("Received Subscription Validation Event from Incoming Call API endpoint");

            // Try to parse as JSON first to handle direct validation code
            try {
                JSONObject jsonData = new JSONObject(eventData.toString());
                if (jsonData.has("validationCode")) {
                    String validationCode = jsonData.getString("validationCode");
                    JSONObject response = new JSONObject();
                    response.put("validationResponse", validationCode);
                    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response.toString());
                }
            } catch (Exception jsonEx) {
                log.debug("Not a direct JSON validation, trying Event Grid format");
            }

            // Fall back to Event Grid format
            SubscriptionValidationEventData subscriptioneventData = eventData
                    .toObject(SubscriptionValidationEventData.class);
            SubscriptionValidationResponse responseData = new SubscriptionValidationResponse();
            responseData.setValidationResponse(subscriptioneventData.getValidationCode());

            return ResponseEntity.ok().body(responseData);
        } catch (Exception e) {
            log.error("Error at subscription validation event {} {}",
                    e.getMessage(),
                    e.getCause());

            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<Object> handleIncomingCall(final BinaryData eventData) {
        log.info("Incoming call received");
        JSONObject data = new JSONObject(eventData.toString());
        String callbackUri = URI.create(acsConfiguration.getCallbackUriHost() + "/api/callbacks").toString();

        String fromCallerId = data.getJSONObject("from").getString("rawId");
        String toCallerId = data.getJSONObject("to").getString("rawId");
        log.info("Incoming call from: {}, to: {}", fromCallerId, toCallerId);
        String incomingCallContext = data.getString("incomingCallContext");
        log.info("Incoming Call Context: " + incomingCallContext);

        // Lobby Call: Answer
        if (toCallerId.contains(acsConfiguration.getAcsTargetCallReceiver())
        || toCallerId.contains(acsConfiguration.getAcsLobbyCallReceiver())) {
            StringBuilder msgLog = new StringBuilder();
            try {
                AnswerCallOptions options = new AnswerCallOptions(incomingCallContext, callbackUri);
                options.setOperationContext("LobbyCall");
                CallIntelligenceOptions intelligenceOptions = new CallIntelligenceOptions();
                intelligenceOptions.setCognitiveServicesEndpoint(acsConfiguration.getCognitiveServiceEndpoint());
                options.setCallIntelligenceOptions(intelligenceOptions);

                AnswerCallResult answerCallResult = acsClient.answerCallWithResponse(options, Context.NONE).getValue();
                lobbyCallConnectionId = answerCallResult.getCallConnection().getCallProperties().getCallConnectionId();

                msgLog.append("User Call(Inbound) Answered by Call Automation.\n")
                        .append("From Caller Raw Id: ").append(fromCallerId).append("\n")
                        .append("To Caller Raw Id:   ").append(toCallerId).append("\n")
                        .append("Lobby Call Connection Id: ").append(lobbyCallConnectionId).append("\n")
                        .append("Correlation Id:           ")
                        .append(answerCallResult.getCallConnection().getCallProperties().getCorrelationId())
                        .append("\n")
                        .append("Lobby Call answered successfully.\n");
                log.info(msgLog.toString());

                return ResponseEntity.ok().build();
            } catch (Exception ex) {
                msgLog.append("Error answering call: ").append(ex.getMessage()).append("\n");
                log.info(msgLog.toString());

                return ResponseEntity.internalServerError().body(msgLog.toString());
            }
        }

        return ResponseEntity.ok().build();
    }

    @Tag(name = "STEP 00. Call Automation Events", description = "Configure Event Grid webhook to point to /api/lobbyCallEventHandler endpoint")
    @PostMapping("/api/lobbyCallEventHandler")
    public ResponseEntity<Object> lobbyCallEventHandler(@RequestBody final String reqBody) {
        log.info("Lobby call event received");
        List<EventGridEvent> events = EventGridEvent.fromString(reqBody);
        var response = ResponseEntity.ok().build();

        for (EventGridEvent eventGridEvent : events) {
            if (eventGridEvent.getEventType().equals(SystemEventNames.EVENT_GRID_SUBSCRIPTION_VALIDATION)) {
                response = handleSubscriptionValidation(eventGridEvent.getData());
            } else if (eventGridEvent.getEventType().equals(SystemEventNames.COMMUNICATION_INCOMING_CALL)) {
                response = handleIncomingCall(eventGridEvent.getData());
            }
        }

        return response;
    }

    @Tag(name = "STEP 00. Call Automation Events", description = "Call Back Events")
    @PostMapping("/api/callbacks")
    public ResponseEntity<String> callbackEvents(@RequestBody final String reqBody) {
        StringBuilder msgLog = new StringBuilder();
        try {
            List<CallAutomationEventBase> events = CallAutomationEventParser.parseEvents(reqBody);
            for (CallAutomationEventBase event : events) {
                String callConnectionId = event.getCallConnectionId();
                log.info(
                        "Received call event callConnectionID: {}, serverCallId: {}, CorrelationId: {}, eventType: {}",
                        callConnectionId,
                        event.getServerCallId(),
                        event.getCorrelationId(),
                        event.getClass().getSimpleName());

                // Parse the event using the Azure SDK's parser if available
                // For illustration, we use eventType string matching
                if (event instanceof CallConnected) {
                    String operationContext = ((CallConnected) event).getOperationContext();
                    String correlationId = ((CallConnected) event).getCorrelationId();

                    log.info("~~~~~~~~~~~~  /api/callbacks ~~~~~~~~~~~~ ");
                    log.info("Received callConnected.CallConnectionId : " + callConnectionId);

                    if ("LobbyCall".equals(operationContext)) {
                        msgLog.append("~~~~~~~~~~~~  /api/callbacks ~~~~~~~~~~~~ \n")
                                .append("Received call event  : ").append(event.getClass()).append("\n")
                                .append("Lobby Call Connection Id: ").append(callConnectionId).append("\n")
                                .append("Correlation Id:           ").append(correlationId).append("\n");

                        // Record lobby caller id and connection id
                        CallConnection lobbyCallConnection = acsClient.getCallConnection(callConnectionId);
                        CallConnectionProperties callConnectionProperties = lobbyCallConnection.getCallProperties();
                        lobbyCallerId = callConnectionProperties.getSource().getRawId();
                        lobbyCallConnectionId = callConnectionProperties.getCallConnectionId();
                        log.info("Lobby Caller Id:     " + lobbyCallerId);
                        log.info("Lobby Connection Id: " + lobbyCallConnectionId);

                        // Play lobby waiting message
                        CallMedia callMedia = lobbyCallConnection.getCallMedia();
                        TextSource textSource = new TextSource().setText(LOBBY_USER_MESSAGE);
                        textSource.setVoiceName("en-US-NancyNeural");
                        CommunicationUserIdentifier playTo = new CommunicationUserIdentifier(lobbyCallerId);
                        callMedia.play(textSource, List.of(playTo));
                    }
                } else if (event instanceof PlayCompleted) {
                    msgLog.append("~~~~~~~~~~~~  /api/callbacks ~~~~~~~~~~~~ \n")
                            .append("Received event: ").append(event.getClass()).append("\n");

                    // Notify Target Call user via websocket
                    // In Java/Spring, you would use a WebSocket messaging template or similar
                    // For now, just log the message
                    String confirmMessageToTargetCall = "Target Call user has been notified that the lobby call is connected.";
                    msgLog.append("Target Call notified with message: ").append(confirmMessageToTargetCall)
                            .append("\n");
                    log.info("Target Call notified with message: " + confirmMessageToTargetCall);
                    return ResponseEntity.ok("Target Call notified with message: " + confirmMessageToTargetCall);
                    // } else if (event instanceof MoveParticipantsSucceeded) {
                    // String correlationId = event.getCorrelationId();
                    // msgLog.append("~~~~~~~~~~~~ /api/callbacks ~~~~~~~~~~~~ \n")
                    // .append("Received event: ").append(event.getClass()).append("\n")
                    // .append("Call Connection Id: ").append(callConnectionId).append("\n")
                    // .append("Correlation Id: ").append(correlationId).append("\n");
                } else if (event instanceof CallDisconnected) {
                    msgLog.append("~~~~~~~~~~~~  /api/callbacks ~~~~~~~~~~~~ \n")
                            .append("Received event: ").append(event.getClass()).append("\n")
                            .append("Call Connection Id: ").append(callConnectionId).append("\n");
                }
            }
        } catch (Exception ex) {
            msgLog.append("Error processing event: ").append(ex.getMessage()).append("\n");
        }
        log.info(msgLog.toString());
        return ResponseEntity.ok(msgLog.toString());
    }

    @Tag(name = "STEP 01. Target Call To ACSUser", description = "Make a call to ACS User by using this endpoint")
    @PostMapping("/targetCallToAcsUser")
    public ResponseEntity<String> createTargetCall(@RequestParam String acsTarget) {
        StringBuilder msgLog = new StringBuilder();
        msgLog.append("\n~~~~~~~~~~~~ /TargetCall(Create)  ~~~~~~~~~~~~\n");

        try {
            URI callbackUri = new URI(acsConfiguration.getCallbackUriHost() + "/api/callbacks");
            CommunicationUserIdentifier targetUser = new CommunicationUserIdentifier(acsTarget);
            CallInvite callInvite = new CallInvite(targetUser);
            CreateCallOptions createCallOptions = new CreateCallOptions(callInvite, callbackUri.toString());
            CallIntelligenceOptions intelligenceOptions = new CallIntelligenceOptions();
            intelligenceOptions.setCognitiveServicesEndpoint(acsConfiguration.getCognitiveServiceEndpoint());
            createCallOptions.setCallIntelligenceOptions(intelligenceOptions);

            CreateCallResult createCallResult = acsClient.createCall(callInvite, callbackUri.toString());
            targetCallConnectionId = createCallResult.getCallConnectionProperties().getCallConnectionId();

            msgLog.append("TargetCall:\n")
                    .append("-----------\n")
                    .append("From: Call Automation\n")
                    .append("To:   ").append(acsTarget).append("\n")
                    .append("Target Call Connection Id: ").append(targetCallConnectionId).append("\n")
                    .append("Correlation Id:            ")
                    .append(createCallResult.getCallConnectionProperties().getCorrelationId()).append("\n");

            log.info(msgLog.toString());
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(msgLog.toString());
        } catch (Exception ex) {
            msgLog.append("Error creating call: ").append(ex.getMessage()).append("\n");
            log.error(msgLog.toString());
            return ResponseEntity.status(500).contentType(MediaType.TEXT_PLAIN).body(msgLog.toString());
        }
    }

    @Tag(name = "STEP 03. Get Call Participants", description = "Get call participants for the lobby call connection by using this endpoint")
    @GetMapping("/getParticipants")
    public ResponseEntity<String> getParticipants() {
        StringBuilder msgLog = new StringBuilder();
        msgLog.append("\n~~~~~~~~~~~~ /GetParticipants/").append(targetCallConnectionId).append(" ~~~~~~~~~~~~\n");

        try {
            CallConnection callConnection = acsClient.getCallConnection(targetCallConnectionId);
            PagedIterable<CallParticipant> participantsResult = callConnection.listParticipants();
            List<CallParticipant> participants = participantsResult.stream().collect(Collectors.toList());

            // Map and sort participants: phone numbers first, then ACS users
            List<String> participantInfo = participants.stream()
                    .map(p -> {
                        CommunicationIdentifier id = p.getIdentifier();
                        String type = id.getClass().getSimpleName();
                        String rawId = id.getRawId();
                        String phoneNumber = (id instanceof PhoneNumberIdentifier)
                                ? ((PhoneNumberIdentifier) id).getPhoneNumber()
                                : null;
                        String acsUserId = (id instanceof CommunicationUserIdentifier)
                                ? ((CommunicationUserIdentifier) id).getId()
                                : null;

                        if (acsUserId == null || acsUserId.isBlank()) {
                            return String.format("%s       - RawId: %s, Phone: %s", type, rawId, phoneNumber);
                        } else {
                            return String.format("%s - RawId: %s", type, acsUserId);
                        }
                    })
                    .sorted(Comparator.comparing(info -> info.contains("Phone:") ? "" : "z")) // phone numbers first
                    .collect(Collectors.toList());

            if (participantInfo.isEmpty()) {
                return ResponseEntity.status(404).body(
                        String.format(
                                "{\"Message\":\"No participants found for the specified call connection.\",\"CallConnectionId\":\"%s\"}",
                                lobbyCallConnectionId));
            } else {
                msgLog.append("\nNo of Participants: ").append(participantInfo.size())
                        .append("\nParticipants: \n-------------\n");
                for (int i = 0; i < participantInfo.size(); i++) {
                    msgLog.append(i + 1).append(". ").append(participantInfo.get(i)).append("\n");
                }
                log.info(msgLog.toString());
                return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(msgLog.toString());
            }
        } catch (Exception ex) {
            log.error("Error getting participants for call " + targetCallConnectionId + ": " + ex.getMessage());
            return ResponseEntity.badRequest().body(
                    String.format("{\"Error\":\"%s\",\"CallConnectionId\":\"%s\"}", ex.getMessage(),
                            targetCallConnectionId));
        }
    }

    @Tag(name = "STEP 04. Terminate Calls", description = "Terminate all Calls created so far by using this endpoint")
    @GetMapping("/terminateCalls")
    public ResponseEntity<String> terminateCalls() {
        try {
            CallConnection callConnection = getCallConnection(acsClient, lobbyCallConnectionId);
            callConnection.hangUpWithResponse(true, Context.NONE);
        } catch (Exception e) {
            log.warn("Could not hang up lobby call: {}", e.getMessage());
        }
        try {
            CallConnection callConnection = getCallConnection(acsClient, targetCallConnectionId);
            callConnection.hangUpWithResponse(true, Context.NONE);
        } catch (Exception e) {
            log.warn("Could not hang up target call: {}", e.getMessage());
        }
        lobbyCallConnectionId = "";
        targetCallConnectionId = "";
        lobbyCallerId = "";
        return ResponseEntity.ok("Terminated all calls");
    }

    // 🔄 Shared Methods
    private CallConnection getCallConnection(CallAutomationClient client, String callConnectionId) {
        if (callConnectionId == null || callConnectionId.isEmpty()) {
            throw new IllegalArgumentException("Call connection id is empty");
        }
        return client.getCallConnection(callConnectionId);
    }

    private CallAutomationClient initClient(String connectionString, String endpoint) {
        try {
            if (connectionString == null || connectionString.trim().isEmpty()) {
                log.error("ACS Connection String is null or empty");
                return null;
            }

            log.info("Initializing Call Automation Client with connection string length: {}",
                    connectionString.length());

            var client = new CallAutomationClientBuilder()
                    .endpoint(endpoint)
                    .connectionString(connectionString)
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
