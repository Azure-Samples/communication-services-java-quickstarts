package com.communication.simpleivr;

import com.communication.simpleivr.utils.ConfigurationManager;
import com.communication.simpleivr.utils.Logger;
import com.azure.core.util.BinaryData;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.azure.communication.callautomation.*;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallConnection;
import com.azure.communication.callautomation.EventHandler;
import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.models.AddParticipantsOptions;
import com.azure.communication.callautomation.models.AddParticipantsResult;
import com.azure.communication.callautomation.models.AnswerCallOptions;
import com.azure.communication.callautomation.models.AnswerCallResult;
import com.azure.communication.callautomation.models.CallMediaRecognizeOptions;
import com.azure.communication.callautomation.models.DtmfTone;
import com.azure.communication.callautomation.models.FileSource;
import com.azure.communication.callautomation.models.HangUpOptions;
import com.azure.communication.callautomation.models.RecordingStateResult;
import com.azure.communication.callautomation.models.ServerCallLocator;
import com.azure.communication.callautomation.models.StartRecordingOptions;
import com.azure.communication.callautomation.models.CallMediaRecognizeDtmfOptions;
import com.azure.communication.callautomation.models.events.AddParticipantsSucceeded;
import com.azure.communication.callautomation.models.events.CallAutomationEventBase;
import com.azure.communication.callautomation.models.events.CallConnected;
import com.azure.communication.callautomation.models.events.RecognizeCompleted;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.core.http.HttpHeader;
import com.azure.core.http.rest.Response;

import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.SystemEventNames;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationEventData;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class SimpleIvr {
    private static ConfigurationManager configurationManager = ConfigurationManager.getInstance();
    private static CallConnection callConnection = null;
    
    private CallAutomationAsyncClient client;
    private String connectionString = "<resource_connection_string>"; // noted from pre-requisite step
    private String callbackBaseUri = "<public_url_generated_by_ngrok>";
    private String agentAudio = "/audio/agent.wav";
    private String customercareAudio = "/audio/customercare.wav";
    private String invalidAudio = "/audio/invalid.wav";
    private String mainmenuAudio = "/audio/mainmenu.wav";
    private String marketingAudio = "/audio/marketing.wav";
    private String salesAudio = "/audio/sales.wav";
    private String applicationPhoneNumber = "<phone_number_acquired_as_prerequisite>";
    private String phoneNumberToAddToCall = "<phone_number_to_add_to_call>"; // in format of +1...

    private CallAutomationAsyncClient getCallAutomationAsyncClient() {
        if (client == null) {
            client = new CallAutomationClientBuilder()
                    .connectionString(connectionString)
                    .buildAsyncClient();
        }
        return client;
    }

    @RequestMapping(value = "/api/incomingCall", method = RequestMethod.POST)
    public ResponseEntity<?> handleIncomingCall(@RequestBody(required = false) String requestBody) {
        List<EventGridEvent> eventGridEvents = EventGridEvent.fromString(requestBody);

        for (EventGridEvent eventGridEvent : eventGridEvents) {
            // Handle the subscription validation event
            if (eventGridEvent.getEventType().equals("Microsoft.EventGrid.SubscriptionValidationEvent")) {
                SubscriptionValidationEventData subscriptionValidationEventData = eventGridEvent.getData()
                        .toObject(SubscriptionValidationEventData.class);
                SubscriptionValidationResponse subscriptionValidationResponse = new SubscriptionValidationResponse()
                        .setValidationResponse(subscriptionValidationEventData.getValidationCode());
                ResponseEntity<SubscriptionValidationResponse> ret = new ResponseEntity<>(
                        subscriptionValidationResponse, HttpStatus.OK);
                return ret;
            }

            // Answer the incoming call and pass the callbackUri where Call Automation
            // events will be delivered
            JsonObject data = new Gson().fromJson(eventGridEvent.getData().toString(), JsonObject.class); // Extract
                                                                                                          // body of the
                                                                                                          // event
            String incomingCallContext = data.get("incomingCallContext").getAsString(); // Query the incoming call
                                                                                        // context info for answering
            String callerId = data.getAsJsonObject("from").get("rawId").getAsString(); // Query the id of caller for
                                                                                       // preparing the Recognize
                                                                                       // prompt.

            // Call events of this call will be sent to an url with unique id.
            String callbackUri = callbackBaseUri
                    + String.format("/api/calls/%s?callerId=%s", UUID.randomUUID(), callerId);

            AnswerCallResult answerCallResult = getCallAutomationAsyncClient()
                    .answerCall(incomingCallContext, callbackUri).block();
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/api/calls/{contextId}", method = RequestMethod.POST)
    public ResponseEntity<?> handleCallEvents(@RequestBody String requestBody, @PathVariable String contextId,
            @RequestParam(name = "callerId", required = true) String callerId) {
        List<CallAutomationEventBase> acsEvents = EventHandler.parseEventList(requestBody);
        
        PlaySource playSource = null;

        for (CallAutomationEventBase acsEvent : acsEvents) {
            if (acsEvent instanceof CallConnected) {
                CallConnected event = (CallConnected) acsEvent;

                // Call was answered and is now established
                String callConnectionId = event.getCallConnectionId();
                CommunicationIdentifier target = CommunicationIdentifier.fromRawId(callerId);

                // Play audio then recognize 1-digit DTMF input with pound (#) stop tone
                playSource = new FileSource().setUri(callbackBaseUri + mainmenuAudio);
                CallMediaRecognizeDtmfOptions recognizeOptions = new CallMediaRecognizeDtmfOptions(target, 1);
                recognizeOptions.setInterToneTimeout(Duration.ofSeconds(10))
                        .setStopTones(new ArrayList<>(Arrays.asList(DtmfTone.POUND)))
                        .setInitialSilenceTimeout(Duration.ofSeconds(5))
                        .setInterruptPrompt(true)
                        .setPlayPrompt(playSource)
                        .setOperationContext("MainMenu");

                getCallAutomationAsyncClient().getCallConnectionAsync(callConnectionId)
                        .getCallMediaAsync()
                        .startRecognizing(recognizeOptions)
                        .block();
            } else if (acsEvent instanceof RecognizeCompleted) {
                RecognizeCompleted event = (RecognizeCompleted) acsEvent;
                // This RecognizeCompleted correlates to the previous action as per the
                // OperationContext value
                if (event.getOperationContext().equals("MainMenu")) {

                    DtmfTone tone = event.getCollectTonesResult().getTones().get(0);
                    if (tone == DtmfTone.ONE) {
                        playSource = new FileSource().setUri(callbackBaseUri + salesAudio);
                    } else if (tone == DtmfTone.TWO) {
                        playSource = new FileSource().setUri(callbackBaseUri + marketingAudio);
                    } else if (tone == DtmfTone.THREE) {
                        playSource = new FileSource().setUri(callbackBaseUri + customercareAudio);
                    } 
                    else if (tone == DtmfTone.FOUR) {
                        playSource = new FileSource().setUri(callbackBaseUri + agentAudio);

                        CallConnectionAsync callConnectionAsync = getCallAutomationAsyncClient()
                                .getCallConnectionAsync(event.getCallConnectionId());

                        // Invite other participants to the call
                        CommunicationIdentifier target = new PhoneNumberIdentifier(phoneNumberToAddToCall);
                        List<CommunicationIdentifier> targets = new ArrayList<>(Arrays.asList(target));
                        AddParticipantsOptions addParticipantsOptions = new AddParticipantsOptions(targets)
                                .setSourceCallerId(new PhoneNumberIdentifier(applicationPhoneNumber));
                        Response<AddParticipantsResult> addParticipantsResultResponse = callConnectionAsync
                                .addParticipantsWithResponse(addParticipantsOptions).block();
                    } else if (tone == DtmfTone.FIVE) {
                        hangupAsync();
                    } else {
                        playSource = new FileSource().setUri(callbackBaseUri + invalidAudio);
                    }
                    String callConnectionId = event.getCallConnectionId();
                    getCallAutomationAsyncClient().getCallConnectionAsync(callConnectionId)
                        .getCallMediaAsync().playToAllWithResponse(playSource, new PlayOptions()).block();
                }
            } else if (acsEvent instanceof RecognizeFailed) {
                RecognizeFailed event = (RecognizeFailed) acsEvent;
                String callConnectionId = event.getCallConnectionId();
                playSource = new FileSource().setUri(callbackBaseUri + invalidAudio);
                getCallAutomationAsyncClient().getCallConnectionAsync(callConnectionId)
                        .getCallMediaAsync().playToAllWithResponse(playSource, new PlayOptions()).block();
            } else if (acsEvent instanceof PlayCompleted){
                hangupAsync();
            }

        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    private static void hangupAsync() {
        Logger.logMessage(Logger.MessageType.INFORMATION, "Performing Hangup operation");

        HangUpOptions hangUpOptions = new HangUpOptions(true);
        Response<Void> response = callConnection.hangUpWithResponse(hangUpOptions, null);
        Logger.logMessage(Logger.MessageType.INFORMATION, "hangupWithResponse -- > " + getResponse(response));
    }

    private static String getResponse(Response<?> response) {
        StringBuilder responseString;
        responseString = new StringBuilder("StatusCode: " + response.getStatusCode() + ", Headers: { ");

        for (HttpHeader header : response.getHeaders()) {
            responseString.append(header.getName()).append(":").append(header.getValue()).append(", ");
        }
        responseString.append("} ");
        return responseString.toString();
    }
}
