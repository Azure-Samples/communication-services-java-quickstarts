package com.communication.callautomation;

import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.CallConnection;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.MoveParticipantsOptions;
import com.azure.communication.callautomation.models.events.*;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.SystemEventNames;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationEventData;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.List;

@RestController
public class ProgramSample {

    private static final Logger log = LoggerFactory.getLogger(ProgramSample.class);
    private CallAutomationClient acsClient;

    // Configuration variables from application.yml
    @Value("${acs.acsConnectionString}")
    private String acsConnectionString;

    @Value("${acs.acsInboundPhoneNumber}")
    private String acsInboundPhoneNumber;

    @Value("${acs.acsOutboundPhoneNumber}")
    private String acsOutboundPhoneNumber;

    @Value("${acs.userPhoneNumber}")
    private String userPhoneNumber;

    @Value("${acs.acsTestIdentity2}")
    private String acsTestIdentity2;

    @Value("${acs.acsTestIdentity3}")
    private String acsTestIdentity3;

    @Value("${acs.callbackUriHost}")
    private String callbackUriHost;

    @Value("${acs.pmaEndpoint}")
    private String acsPmaEndpoint;

    private String callConnectionId1 = "";
    private String callConnectionId2 = "";
    private String callConnectionId3 = "";
    private String lastWorkflowCallType = "";
    private String callerId1 = "";
    private String callerId2 = "";
    private String callerId3 = "";
    private String calleeId1 = "";
    private String calleeId2 = "";
    private String calleeId3 = "";

    @Tag(name = "STEP 00. Call Automation Events", description = "Configure Event Grid webhook to point to /api/moveParticipantEvent endpoint")
    @PostMapping("/api/moveParticipantEvent")
    public ResponseEntity<String> moveParticipantEvent(@RequestBody final String reqBody) {
        try {
            log.info("Received webhook request body: {}", reqBody);
            
            // Check if request body is empty or not JSON
            if (reqBody == null || reqBody.trim().isEmpty()) {
                log.warn("Received empty request body");
                return ResponseEntity.badRequest().body("Request body is empty");
            }
            
            // Check if it's a simple test string
            if (!reqBody.trim().startsWith("[") && !reqBody.trim().startsWith("{")) {
                log.info("Received test string: {}", reqBody);
                return ResponseEntity.ok("Test webhook received: " + reqBody);
            }
            
            List<EventGridEvent> events = EventGridEvent.fromString(reqBody);
            for (EventGridEvent eventGridEvent : events) {
                log.info("Processing event type: {}", eventGridEvent.getEventType());
                
                if (eventGridEvent.getEventType().equals(SystemEventNames.EVENT_GRID_SUBSCRIPTION_VALIDATION)) {
                    return handleSubscriptionValidation(eventGridEvent.getData());
                } else if (eventGridEvent.getEventType().equals(SystemEventNames.COMMUNICATION_INCOMING_CALL)) {
                    handleIncomingCall(eventGridEvent.getData());
                }
            }
            return ResponseEntity.ok().body(null);
        } catch (Exception e) {
            log.error("Error processing Event Grid webhook: {}", e.getMessage());
            log.error("Request body was: {}", reqBody);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook: " + e.getMessage());
        }
    }

    private ResponseEntity<String> handleSubscriptionValidation(final BinaryData eventData) {
        try {
            log.info("Received Subscription Validation Event from Incoming Call API endpoint");
            SubscriptionValidationEventData subscriptioneventData = eventData
                    .toObject(SubscriptionValidationEventData.class);
            
            String validationCode = subscriptioneventData.getValidationCode();
            log.info("Returning validation code: {}", validationCode);
            
            // Event Grid expects a JSON response with validationResponse field
            String responseBody = "{\"validationResponse\":\"" + validationCode + "\"}";
            
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(responseBody);
        } catch (Exception e) {
            log.error("Error at subscription validation event {} {}",
                    e.getMessage(),
                    e.getCause());
            return ResponseEntity.internalServerError().body("Validation failed");
        }
    }

    private void handleIncomingCall(final BinaryData eventData) {
        JSONObject data = new JSONObject(eventData.toString());
        String callbackUri = URI.create(callbackUriHost + "/api/callbacks").toString();
        log.info("Method - handleIncomingCall: {}", data.toString());

        try {
            String fromCallerId = data.getJSONObject("from").getString("rawId");
            String toCallerId = data.getJSONObject("to").getString("rawId");
            log.info("Incoming call from: {}, to: {}", fromCallerId, toCallerId);
            String incomingCallContext = data.getString("incomingCallContext");
            log.info("Incoming Call Context: " + incomingCallContext);

            // Scenario 1: User calls from their phone number to ACS inbound number
            if (fromCallerId != null && fromCallerId.contains(userPhoneNumber)) {
                log.info("=== SCENARIO 1: USER INCOMING CALL ===");
                AnswerCallOptions options = new AnswerCallOptions(incomingCallContext, callbackUri);
                options.setOperationContext("IncomingCallFromUser");
                AnswerCallResult answerCallResult = acsClient.answerCallWithResponse(options, Context.NONE).getValue();
                callConnectionId1 = answerCallResult.getCallConnection().getCallProperties().getCallConnectionId();

                log.info("User Call Answered - CallConnectionId1: " + callConnectionId1);
                log.info("Correlation Id: " + data.getString("correlationId"));
                log.info("Operation Context: IncomingCallFromUser");
                log.info("=== END SCENARIO 1 ===");
            }
            // Scenario 2: ACS inbound number calls ACS outbound number (workflow triggered)
            else if (fromCallerId != null && fromCallerId.contains(acsInboundPhoneNumber)) {
                log.info("=== SCENARIO 2: WORKFLOW CALL TO BE REDIRECTED ===");
                log.info("Last Workflow Call Type: " + lastWorkflowCallType);

                String redirectTarget = acsTestIdentity2;
                if ("CallTwo".equals(lastWorkflowCallType)) {
                    redirectTarget = acsTestIdentity2;
                    log.info("Processing Call Two - Redirecting to ACS User Identity 2");
                } else if ("CallThree".equals(lastWorkflowCallType)) {
                    redirectTarget = acsTestIdentity3;
                    log.info("Processing Call Three - Redirecting to ACS User Identity 3");
                }

                // Use ACS Java SDK to redirect the call
                CallInvite callInvite = new CallInvite(new CommunicationUserIdentifier(redirectTarget));
                RedirectCallOptions redirectOptions = new RedirectCallOptions(incomingCallContext, callInvite);
                acsClient.redirectCallWithResponse(redirectOptions, Context.NONE);

                // Add a delay to allow call redirect to process
                try {
                    Thread.sleep(2000); // 2 seconds delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Sleep interrupted: {}", e.getMessage());
                }

                if ("CallTwo".equals(lastWorkflowCallType)) {
                    calleeId2 = acsTestIdentity2;
                } else if ("CallThree".equals(lastWorkflowCallType)) {
                    calleeId3 = acsTestIdentity3;
                }

                log.info("Call Redirected to ACS User Identity: " + redirectTarget);
                log.info("Correlation Id: " + data.getString("correlationId"));
                log.info("Operation Context: " + ("CallThree".equals(lastWorkflowCallType) ? "CallThree" : "CallTwo"));
                log.info("=== END SCENARIO 2 ===");
            }
        } catch (Exception e) {
            log.error("Error getting info {} {}",
                    e.getMessage(),
                    e.getCause());
        }
    }

    @Tag(name = "STEP 00. Call Automation Events", description = "Call Back Events")
    @PostMapping(path = "/api/callbacks")
    public ResponseEntity<String> callbackEvents(@RequestBody final String reqBody) {
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

                if (event instanceof CreateCallFailed ||
                        event instanceof ConnectFailed ||
                        event instanceof PlayFailed ||
                        event instanceof RecognizeFailed) {
                    // handle Failed
                    log.error(reqBody);
                } else if (event instanceof CallConnected) {
                    log.info("Call connected successfully");
                } else {
                    // handle Success
                }
            }
            return ResponseEntity.ok().body("");
        } catch (Exception e) {
            log.error("Error processing callback events: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process callback events.");
        }
    }

    @PostConstruct
    public void initializeClient() {
        try {
            log.info("Initializing Call Automation Client from configuration...");
            acsClient = initClient(acsConnectionString, acsPmaEndpoint);
            log.info("Call Automation Client initialized successfully from application.yml configuration.");
        } catch (Exception e) {
            log.error("Error initializing Call Automation Client: {}", e.getMessage());
        }
    }

    @Tag(name = "STEP 01. User Call", description = "Make a call from User Phone Number to ACS Inbound Phone Number by using this endpoint")
    @GetMapping("/userCallToCallAutomation")
    public ResponseEntity<String> userCallToCallAutomation() {
        try {
            PhoneNumberIdentifier target = new PhoneNumberIdentifier(acsInboundPhoneNumber);
            PhoneNumberIdentifier caller = new PhoneNumberIdentifier(userPhoneNumber);

            URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");
            CallInvite callInvite = new CallInvite(target, caller);

            CreateCallResult result = acsClient.createCall(callInvite, callbackUri.toString());
            callConnectionId1 = result.getCallConnectionProperties().getCallConnectionId();
            lastWorkflowCallType = "CallOne";
            callerId1 = userPhoneNumber;
            calleeId1 = acsInboundPhoneNumber;
            log.info("Created user call with connection id: " + callConnectionId1);
            return ResponseEntity.ok("Created user call with connection id: " + callConnectionId1);
        } catch (Exception e) {
            log.error("Error creating user call : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create user call.");
        }
    }

    @Tag(name = "STEP 02a. Create Call 2", description = "Create a Call for ACS Test Identity 2 by using this endpoint and answer it manually from Web Client App")
    @GetMapping("/createCall2")
    public ResponseEntity<String> createCall2() {
        try {
            PhoneNumberIdentifier target = new PhoneNumberIdentifier(acsOutboundPhoneNumber);
            PhoneNumberIdentifier caller = new PhoneNumberIdentifier(acsInboundPhoneNumber);

            URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");
            CallInvite callInvite = new CallInvite(target, caller);

            CreateCallResult result = acsClient.createCall(callInvite, callbackUri.toString());
            callConnectionId2 = result.getCallConnectionProperties().getCallConnectionId();
            lastWorkflowCallType = "CallTwo";
            callerId2 = acsInboundPhoneNumber;
            calleeId2 = acsOutboundPhoneNumber;

            log.info("Created call 2 with connection id: " + callConnectionId2);
            return ResponseEntity.ok("Created call 2 with connection id: " + callConnectionId2);
        } catch (Exception e) {
            log.error("Error creating call 2 : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create call 2.");
        }
    }

    @Tag(name = "STEP 02b. Create Call 3", description = "Create a Call for ACS Test Identity 3 by using this endpoint and answer it manually from Web Client App")
    @GetMapping("/createCall3")
    public ResponseEntity<String> createCall3() {
        try {
            PhoneNumberIdentifier target = new PhoneNumberIdentifier(acsOutboundPhoneNumber);
            PhoneNumberIdentifier caller = new PhoneNumberIdentifier(acsInboundPhoneNumber);

            URI callbackUri = URI.create(callbackUriHost + "/api/callbacks");
            CallInvite callInvite = new CallInvite(target, caller);

            CreateCallResult result = acsClient.createCall(callInvite, callbackUri.toString());
            callConnectionId3 = result.getCallConnectionProperties().getCallConnectionId();
            lastWorkflowCallType = "CallThree";
            callerId3 = acsInboundPhoneNumber;
            calleeId3 = acsOutboundPhoneNumber;

            log.info("Created call 3 with connection id: " + callConnectionId3);
            return ResponseEntity.ok("Created call 3 with connection id: " + callConnectionId3);
        } catch (Exception e) {
            log.error("Error creating call 3 : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create call 3.");
        }
    }

    @Tag(name = "STEP 03a. Move Participant 2", description = "Move Participant to ACS Test Identity 2 in the main Call by using this endpoint")
    @GetMapping("/moveParticipant2")
    public ResponseEntity<String> moveParticipant2() {
        try {
            log.info("=== MANUAL MOVE PARTICIPANTS REQUESTED ===");
            log.info("Source Connection ID: " + callConnectionId2);
            log.info("Target Connection ID: " + callConnectionId1);
            log.info("Participant to Move: " + acsOutboundPhoneNumber);

            // Get the target connection (where we want to move participants to)
            CallConnection targetConnection = acsClient.getCallConnection(callConnectionId1);

            // Create participant identifier based on the input
            Object participantToMove;
            if (acsOutboundPhoneNumber.startsWith("+")) {
                // Phone number
                participantToMove = new PhoneNumberIdentifier(acsOutboundPhoneNumber);
                log.info("Moving phone number participant: " + acsOutboundPhoneNumber);
            } else if (acsOutboundPhoneNumber.startsWith("8:acs:")) {
                // ACS Communication User
                participantToMove = new CommunicationUserIdentifier(acsOutboundPhoneNumber);
                log.info("Moving ACS user participant: " + acsOutboundPhoneNumber);
            } else {
                return ResponseEntity.badRequest().body(
                        "Invalid participant format. Use phone number (+1234567890) or ACS user ID (8:acs:...)");
            }
            // Prepare move participants request
            MoveParticipantsOptions request = new MoveParticipantsOptions(
                    List.of((CommunicationIdentifier) participantToMove),
                    callConnectionId2);

            // Call the ACS SDK to move participants
            targetConnection.moveParticipants(request);
            lastWorkflowCallType = "";
            calleeId1 = acsTestIdentity2;
            callConnectionId2 = "";
            callerId2 = "";
            calleeId2 = "";

            // For demonstration, assume success
            log.info("Move Participants operation completed successfully");
            log.info("Moved " + acsTestIdentity2 + " from " +
                    callConnectionId2 + " to " + callConnectionId1);
            log.info("=== MOVE PARTICIPANTS OPERATION COMPLETE ===");

            return ResponseEntity.ok(
                    "Successfully moved participant " + acsTestIdentity2 +
                            " from " + callConnectionId2 +
                            " to " + callConnectionId1);
        } catch (Exception e) {
            log.error("Error moving participant : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to move participant.");
        }
    }

    @Tag(name = "STEP 03b. Move Participant 3", description = "Move Participant to ACS Test Identity 3 in the main Call by using this endpoint")
    @GetMapping("/moveParticipant3")
    public ResponseEntity<String> moveParticipant3() {
        try {
            log.info("=== MANUAL MOVE PARTICIPANTS REQUESTED ===");
            log.info("Source Connection ID: " + callConnectionId3);
            log.info("Target Connection ID: " + callConnectionId1);
            log.info("Participant to Move: " + acsOutboundPhoneNumber);

            // Get the target connection (where we want to move participants to)
            CallConnection targetConnection = acsClient.getCallConnection(callConnectionId1);

            // Create participant identifier based on the input
            Object participantToMove;
            if (acsOutboundPhoneNumber.startsWith("+")) {
                // Phone number
                participantToMove = new PhoneNumberIdentifier(acsOutboundPhoneNumber);
                log.info("Moving phone number participant: " + acsOutboundPhoneNumber);
            } else if (acsOutboundPhoneNumber.startsWith("8:acs:")) {
                // ACS Communication User
                participantToMove = new CommunicationUserIdentifier(acsOutboundPhoneNumber);
                log.info("Moving ACS user participant: " + acsOutboundPhoneNumber);
            } else {
                return ResponseEntity.badRequest().body(
                        "Invalid participant format. Use phone number (+1234567890) or ACS user ID (8:acs:...)");
            }
            // Prepare move participants request
            MoveParticipantsOptions request = new MoveParticipantsOptions(
                    List.of((CommunicationIdentifier) participantToMove),
                    callConnectionId3);

            // Call the ACS SDK to move participants
            targetConnection.moveParticipants(request);
            lastWorkflowCallType = "";
            calleeId1 = acsTestIdentity3;
            callConnectionId3 = "";
            callerId3 = "";
            calleeId3 = "";

            // For demonstration, assume success
            log.info("Move Participants operation completed successfully");
            log.info("Moved " + acsTestIdentity3 + " from " +
                    callConnectionId3 + " to " + callConnectionId1);
            log.info("=== MOVE PARTICIPANTS OPERATION COMPLETE ===");

            return ResponseEntity.ok(
                    "Successfully moved participant " + acsTestIdentity3 +
                            " from " + callConnectionId3 +
                            " to " + callConnectionId1);
        } catch (Exception e) {
            log.error("Error moving participant : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to move participant.");
        }
    }

    @Tag(name = "STEP 04. Get Status", description = "Get the current status of the calls by using this endpoint")
    @GetMapping("/getStatus")
    public ResponseEntity<String> getStatus() {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n")
                    .append("<html>\n")
                    .append("<head>\n")
                    .append("<title>Call Status</title>\n")
                    .append("<style>\n")
                    .append("body { font-family: Arial, sans-serif; margin: 20px; }\n")
                    .append("table { border-collapse: collapse; width: 100%; }\n")
                    .append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }\n")
                    .append("th { background-color: #f2f2f2; }\n")
                    .append("tr:nth-child(even) { background-color: #f9f9f9; }\n")
                    .append("h1 { color: #333; }\n")
                    .append(".empty { color: #999; font-style: italic; }\n")
                    .append("</style>\n")
                    .append("</head>\n")
                    .append("<body>\n")
                    .append("<h1>Call Connection Status</h1>\n")
                    .append("<table>\n")
                    .append("<tr><th>Call</th><th>Connection ID</th><th>Caller ID</th><th>Callee ID</th></tr>\n");

            // Add call connection IDs to the table
            html.append("<tr>")
                    .append("<td>Call 1 (User Call)</td>")
                    .append("<td>")
                    .append(callConnectionId1.isEmpty() ? "<span class='empty'>Not created</span>" : callConnectionId1)
                    .append("</td>")
                    .append("<td>").append(callConnectionId1.isEmpty() ? "Inactive" : callerId1).append("</td>")
                    .append("<td>").append(callConnectionId1.isEmpty() ? "Inactive" : calleeId1).append("</td>")
                    .append("</tr>\n");

            html.append("<tr>")
                    .append("<td>Call 2 (ACS Test Identity 2)</td>")
                    .append("<td>")
                    .append(callConnectionId2.isEmpty() ? "<span class='empty'>Not created</span>" : callConnectionId2)
                    .append("</td>")
                    .append("<td>").append(callConnectionId2.isEmpty() ? "Inactive" : callerId2).append("</td>")
                    .append("<td>").append(callConnectionId2.isEmpty() ? "Inactive" : calleeId2).append("</td>")
                    .append("</tr>\n");

            html.append("<tr>")
                    .append("<td>Call 3 (ACS Test Identity 3)</td>")
                    .append("<td>")
                    .append(callConnectionId3.isEmpty() ? "<span class='empty'>Not created</span>" : callConnectionId3)
                    .append("</td>")
                    .append("<td>").append(callConnectionId3.isEmpty() ? "Inactive" : callerId3).append("</td>")
                    .append("<td>").append(callConnectionId3.isEmpty() ? "Inactive" : calleeId3).append("</td>")
                    .append("</tr>\n");

            html.append("</table>\n")
                    .append("</body>\n")
                    .append("</html>");

            return ResponseEntity.ok().contentType(org.springframework.http.MediaType.TEXT_HTML).body(html.toString());
        } catch (Exception e) {
            log.error("Error getting status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(org.springframework.http.MediaType.TEXT_HTML)
                    .body("<html><body><h1>Error</h1><p>Failed to get call status: " + e.getMessage()
                            + "</p></body></html>");
        }
    }

    @Tag(name = "STEP 05. Terminate Calls", description = "Terminate all Calls created so far by using this endpoint")
    @GetMapping("/terminateCalls")
    public ResponseEntity<String> terminateCalls() {
        try {
            CallConnection callConnection = getCallConnection(acsClient, callConnectionId1);
            callConnection.hangUpWithResponse(true, Context.NONE);
        } catch (Exception e) {
            log.warn("Could not hang up call 1: {}", e.getMessage());
        }
        try {
            CallConnection callConnection = getCallConnection(acsClient, callConnectionId2);
            callConnection.hangUpWithResponse(true, Context.NONE);
        } catch (Exception e) {
            log.warn("Could not hang up call 2: {}", e.getMessage());
        }
        try {
            CallConnection callConnection = getCallConnection(acsClient, callConnectionId3);
            callConnection.hangUpWithResponse(true, Context.NONE);
        } catch (Exception e) {
            log.warn("Could not hang up call 3: {}", e.getMessage());
        }
        callConnectionId1 = "";
        callConnectionId2 = "";
        callConnectionId3 = "";
        lastWorkflowCallType = "";
        callerId1 = "";
        callerId2 = "";
        callerId3 = "";
        calleeId1 = "";
        calleeId2 = "";
        calleeId3 = "";
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
