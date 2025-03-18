package com.communication.callautomation;

import com.azure.communication.rooms.models.CommunicationRoom;
import com.azure.communication.rooms.models.CreateRoomOptions;
import com.azure.communication.rooms.models.ParticipantRole;
import com.azure.communication.rooms.models.RoomParticipant;
import com.azure.communication.callautomation.CallAutomationAsyncClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.CallConnectionAsync;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.communication.common.implementation.CommunicationConnectionString;
import com.azure.communication.identity.CommunicationIdentityClient;
import com.azure.communication.identity.CommunicationIdentityClientBuilder;
import com.azure.communication.identity.models.*;
import com.azure.communication.rooms.RoomsClient;
import com.azure.communication.rooms.RoomsClientBuilder;
import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.azure.communication.common.*;
import com.azure.core.credential.*;

@RestController
@Slf4j
public class ProgramSample {
    private final AppConfig appConfig;
    private final CallAutomationAsyncClient asyncClient;
    RoomParticipant participant_1;
    RoomParticipant participant_2;
    RoomsClient roomsClient;
    CommunicationIdentityClient communicationIdentityClient;
    // Global variables (class-level)
    private String callConnectionId = null;
    private String serverCallId = null;
    private String roomId = null;
    private CallConnectionAsync callConnection = null;

    public String correlationId = null;

    public void setCallConnection(CallConnectionAsync callConnection) {
        this.callConnection = callConnection;
    }

    // Method to set call connection ID
    public void setCallConnectionId(String callConnectionId) {
        this.callConnectionId = callConnectionId;
    }

    // Method to set server call ID
    public void setServerCallId(String serverCallId) {
        this.serverCallId = serverCallId;
    }

    // Method to set room ID
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public CallConnectionAsync getCallConnection() {
        return callConnection;
    }

    // Accessor methods to retrieve the values
    public String getCallConnectionId() {
        return callConnectionId;
    }

    public String getServerCallId() {
        return serverCallId;
    }

    public String getRoomId() {
        return roomId;
    }

    private CommunicationUserIdentifier user1;
    private AccessToken userToken1;
    private CommunicationUserIdentifier user2;
    private AccessToken userToken2;

    public ProgramSample(final AppConfig appConfig) {
        this.appConfig = appConfig;
        this.asyncClient = initAsyncClient();
        this.roomsClient = createRoomsClientWithConnectionString();
        this.communicationIdentityClient = getCommunicationIdentityClientBuilder().buildClient();
    }

    public RoomsClient createRoomsClientWithConnectionString() {
        RoomsClient roomsClient = new RoomsClientBuilder().connectionString(appConfig.getConnectionString())
                .buildClient();

        return roomsClient;
    }

    public CommunicationIdentityClientBuilder getCommunicationIdentityClientBuilder() {
        CommunicationIdentityClientBuilder builder = new CommunicationIdentityClientBuilder();
        CommunicationConnectionString connectionStringObject = new CommunicationConnectionString(
                appConfig.getConnectionString());
        String endpoint = connectionStringObject.getEndpoint();
        String accessKey = connectionStringObject.getAccessKey();
        builder.endpoint(endpoint)
                .credential(new AzureKeyCredential(accessKey));

        return builder;
    }

    // Method to create users and their tokens only once
    private void initializeUsers() {
        List<CommunicationTokenScope> scopes = Arrays.asList(CommunicationTokenScope.VOIP);

        // Create user1
        user1 = communicationIdentityClient.createUser();
        userToken1 = communicationIdentityClient.getToken(user1, scopes);
        log.info("User1 created with ID: {}", user1.getId());
        log.info("User1 token: {}", userToken1.getToken());

        // Create user2
        user2 = communicationIdentityClient.createUser();
        userToken2 = communicationIdentityClient.getToken(user2, scopes);
        log.info("User2 created with ID: {}", user2.getId());
        log.info("User2 token: {}", userToken2.getToken());
    }

    @PostMapping("/create-room")
    public ResponseEntity<Object> createRoomApi() {
        try {
            initializeUsers();
            // Call the createRoom method
            CommunicationRoom roomResult = createRoom();

            if (roomResult != null) {
                log.info("Room created successfully with ID: {}", roomResult.getRoomId());
                setRoomId(roomResult.getRoomId());
                getRoom(roomResult.getRoomId());

                var responsePayload = Map.of(
                        "roomId", roomResult.getRoomId(),
                        "user1Id", user1.getId(),
                        "user1Token", userToken1.getToken(),
                        "user2Id", user2.getId(),
                        "user2Token", userToken2.getToken());
                return ResponseEntity.ok(responsePayload);
            } else {
                log.error("Room creation failed.");
                return ResponseEntity.status(500).body("Room creation failed.");
            }
        } catch (Exception ex) {
            log.error("Error while creating room: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body("Error occurred: " + ex.getMessage());
        }
    }

    public CommunicationRoom createRoom() {

        participant_1 = new RoomParticipant(user1);
        participant_2 = new RoomParticipant(user2);
        OffsetDateTime validFrom = OffsetDateTime.now();
        OffsetDateTime validUntil = validFrom.plusDays(30);

        List<RoomParticipant> roomParticipants = new ArrayList<>();
        roomParticipants.add(participant_1.setRole(ParticipantRole.PRESENTER));
        roomParticipants.add(participant_2.setRole(ParticipantRole.ATTENDEE));

        System.out.print("Creating room...\n");

        CreateRoomOptions roomOptions = new CreateRoomOptions()
                .setValidFrom(validFrom)
                .setValidUntil(validUntil)
                .setPstnDialOutEnabled(true)
                .setParticipants(roomParticipants);

        // Synchronously get the response and return CommunicationRoom
        Response<CommunicationRoom> response = roomsClient.createRoomWithResponse(roomOptions, Context.NONE);
        return response.getValue(); // This gives you the CommunicationRoom object
    }

    public void getRoom(String roomId) {
        try {
            System.out.print("Getting room...\n");

            CommunicationRoom roomResult = roomsClient.getRoom(roomId);
            System.out.println("RoomId: " + roomResult.getRoomId());
        } catch (Exception ex) {
            System.out.println(ex);
        }

    }

    private CallAutomationAsyncClient initAsyncClient() {
        CallAutomationAsyncClient client;
        try {
            client = new CallAutomationClientBuilder()
                    .connectionString(appConfig.getConnectionString())
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

    @PostMapping("/connectCall")
    public ResponseEntity<Map<String, String>> connectCall() {
        if (getRoomId() != null && !getRoomId().isEmpty()) {
            // Construct the callback URL from the config
            String callbackUri = appConfig.getCallBackUri();
            log.info("Callback URL: {}", callbackUri);
            log.info("Room ID: {}", getRoomId());

            try {
                // Create a CallLocator for the room
                CallLocator callLocator = new RoomCallLocator(getRoomId());

                // Build the ConnectCallOptions
                ConnectCallOptions connectCallOptions = new ConnectCallOptions(callLocator, callbackUri)
                        .setOperationContext("connectCallContext");
                // Call the connectCallWithResponse method asynchronously
                Mono<Response<ConnectCallResult>> res = asyncClient.connectCallWithResponse(connectCallOptions);
                res.subscribe(response -> {
                    if (response.getStatusCode() == 200) {
                        log.info("Connect request succeeded {}", response.getValue());
                        log.info("Connect request correlationId {}",
                                response.getValue().getCallConnectionProperties().getCorrelationId());
                    } else {
                        log.error("Failed to connect call: {}", response.getStatusCode());
                    }
                });
                // Return the callConnectionId and correlationId in the response
                Map<String, String> responsePayload = Map.of(
                        "callConnectionId", getCallConnectionId());
                return ResponseEntity.ok(responsePayload);

            } catch (Exception ex) {
                log.error("Unexpected error while connecting the call: {}", ex.getMessage());
                return ResponseEntity.status(500).body(Map.of("error", "Error occurred while connecting the call"));
            }
        } else {
            log.warn("Room ID is empty or room not available.");
            return ResponseEntity.status(400).body(Map.of("error", "Room ID is empty or room not available."));
        }
    }

    @PostMapping(path = "/api/callback")
    public ResponseEntity<String> handleCallbacks(@RequestBody String requestBody) {
        try {
            // Parse events from the request body
            List<CallAutomationEventBase> events = CallAutomationEventParser.parseEvents(requestBody);

            for (CallAutomationEventBase event : events) {
                String callConnectionId = event.getCallConnectionId();
                setCallConnectionId(callConnectionId);
                String serverCallId = event.getServerCallId();
                setServerCallId(serverCallId);
                // Assuming asyncClient is an instance of your client class that interacts with
                // the API
                CallConnectionAsync callConnection = asyncClient.getCallConnectionAsync(callConnectionId);
                setCallConnection(callConnection); // Set the call connection object for further processing

                // Handle specific event types
                if (event instanceof CallConnected) {
                    this.correlationId = ((CallConnected) event).getCorrelationId();
                    log.info("Received CallConnected event");
                    log.info("ConnectionId: {}", callConnectionId);
                    log.info("Correlation ID: {}", ((CallConnected) event).getCorrelationId());
                } else if (event instanceof AddParticipantSucceeded) {
                    AddParticipantSucceeded addParticipantSucceededEvent = (AddParticipantSucceeded) event;
                    log.info("Received AddParticipantSucceeded event");
                } else if (event instanceof ParticipantsUpdated) {
                    ParticipantsUpdated participantsUpdatedEvent = (ParticipantsUpdated) event;
                    log.info("Received ParticipantsUpdated event");
                } else if (event instanceof AddParticipantFailed) {
                    AddParticipantFailed addParticipantFailedEvent = (AddParticipantFailed) event;
                    ResultInformation resultInfo = addParticipantFailedEvent.getResultInformation();
                    log.info("Received AddParticipantFailed event");
                    log.info("Code: {}, Subcode: {}, Message: {}",
                            resultInfo.getCode(), resultInfo.getSubCode(), resultInfo.getMessage());
                } else if (event instanceof CallDisconnected) {
                    log.info("Received CallDisconnected event");
                    log.info("Correlation ID: {}", ((CallDisconnected) event).getCorrelationId());
                } else {
                    log.warn("Unhandled event type: {}", event.getClass().getSimpleName());
                }
            }

            // Respond with success
            return ResponseEntity.ok("{\"status\":\"OK\"}");
        } catch (Exception e) {
            log.error("Error processing callback", e);
            return ResponseEntity.status(500).body("{\"error\":\"Internal server error\"}");
        }
    }

    @PostMapping("/add-pstn-participant")
    public ResponseEntity<String> addPstnParticipant() {
        String callConnectionId = getCallConnectionId();

        if (callConnectionId == null || callConnectionId.isEmpty()) {
            log.warn("Call connection ID is empty or call not active.");
        }

        // Create PhoneNumberIdentifier instances
        PhoneNumberIdentifier target = new PhoneNumberIdentifier(appConfig.getTargetphonenumber());
        PhoneNumberIdentifier sourceCallerIdNumber = new PhoneNumberIdentifier(appConfig.getCallerphonenumber());

        try {
            // Prepare AddParticipantOptions
            CallInvite callInvite = new CallInvite(target, sourceCallerIdNumber)
                    .setSourceCallerIdNumber(sourceCallerIdNumber);
            AddParticipantOptions addParticipantOptions = new AddParticipantOptions(callInvite)
                    .setOperationContext("addAgentContext")
                    .setInvitationTimeout(Duration.ofSeconds(15));

            // Add participant reactively
            getCallConnection()
                    .addParticipantWithResponse(addParticipantOptions)
                    .subscribe(response -> {
                        log.info("Added PSTN participant with invitation ID: {}",
                                response.getValue().getInvitationId());
                    });
            return ResponseEntity.ok("PSTN participant added successfully.");
        } catch (Exception e) {
            log.error("Unexpected error while initiating add participant request: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error occurred while adding PSTN participant.");
        }
    }

    @PostMapping("/hangUp")
    public Mono<ResponseEntity<String>> hangUp() {
        try {
            // Hang up the call asynchronously
            return getCallConnection() // Initiate the hang-up process
                    .hangUpWithResponse(true) // Hang up for all participants (set to false if not for everyone)
                    .doOnTerminate(() -> {
                        log.info("Correlation ID: {}", this.correlationId);
                        log.info("Call terminated for connectionId: {}", getCallConnectionId());
                    })
                    .map(response -> ResponseEntity.ok("Call terminated successfully.")) // Return success response
                    .onErrorResume(error -> {
                        log.error("Error when terminating the call for all participants: {} {}", error.getMessage(),
                                error.getCause());
                        return Mono.just(
                                ResponseEntity.status(500).body("Error terminating the call: " + error.getMessage()));
                    });
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            return Mono.just(ResponseEntity.status(500).body("Unexpected error: " + e.getMessage()));
        }
    }
}
