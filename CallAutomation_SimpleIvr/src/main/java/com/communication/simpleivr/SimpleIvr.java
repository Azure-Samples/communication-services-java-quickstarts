package com.communication.simpleivr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.azure.communication.callautomation.*;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.EventHandler;
import com.azure.communication.callautomation.models.AddParticipantsOptions;
import com.azure.communication.callautomation.models.AddParticipantsResult;
import com.azure.communication.callautomation.models.AnswerCallResult;
import com.azure.communication.callautomation.models.DtmfTone;
import com.azure.communication.callautomation.models.FileSource;
import com.azure.communication.callautomation.models.CallMediaRecognizeDtmfOptions;
import com.azure.communication.callautomation.models.events.CallAutomationEventBase;
import com.azure.communication.callautomation.models.events.CallConnected;
import com.azure.communication.callautomation.models.events.RecognizeCompleted;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.core.http.HttpHeader;
import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.SystemEventNames;
import com.azure.messaging.eventgrid.systemevents.AcsRecordingChunkInfoProperties;
import com.azure.messaging.eventgrid.systemevents.AcsRecordingFileStatusUpdatedEventData;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationEventData;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class SimpleIvr {
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
    Logger logger =  Logger.getLogger(SimpleIvr.class.getName());

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
        callerId = callerId.replaceAll("\\s", "");
        for (CallAutomationEventBase acsEvent : acsEvents) {
            if (acsEvent instanceof CallConnected) {
                CallConnected event = (CallConnected) acsEvent;

                // Call was answered and is now established
                String callConnectionId = event.getCallConnectionId();

                // Start recording
                ServerCallLocator serverCallLocator = new ServerCallLocator(getCallAutomationAsyncClient().getCallConnectionAsync(callConnectionId)
                        .getCallProperties().block().getServerCallId());
                StartRecordingOptions recordingOptions = new StartRecordingOptions(serverCallLocator);
                Response<RecordingStateResult> response = getCallAutomationAsyncClient()
                .getCallRecordingAsync().startRecordingWithResponse(recordingOptions).block();
                logger.log(Level.INFO, "Start Recording with recording ID: " + response.getValue().getRecordingId());

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
                        
                        logger.log(Level.INFO, String.format("addParticipants Response %s", getResponse(addParticipantsResultResponse)));

                    } else if (tone == DtmfTone.FIVE) {
                        hangupAsync(event.getCallConnectionId());
                        break;
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
                hangupAsync(acsEvent.getCallConnectionId());
            }

        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping(value = "/api/recording", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> getRecordingFile(@RequestBody String requestBody) {

        List<EventGridEvent> eventGridEvents = EventGridEvent.fromString(requestBody);
        for (EventGridEvent eventGridEvent : eventGridEvents) {
            if (eventGridEvent.getEventType().equals(SystemEventNames.EVENT_GRID_SUBSCRIPTION_VALIDATION)) {
                try {

                    SubscriptionValidationEventData subscriptionValidationEvent = eventGridEvent.getData()
                            .toObject(SubscriptionValidationEventData.class);
                    SubscriptionValidationResponse responseData = new SubscriptionValidationResponse();
                    responseData.setValidationResponse(subscriptionValidationEvent.getValidationCode());

                    return new ResponseEntity<>(responseData, HttpStatus.OK);
                } catch (Exception e) {
                    e.printStackTrace();
                    return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            if (eventGridEvent.getEventType().equals(SystemEventNames.COMMUNICATION_RECORDING_FILE_STATUS_UPDATED)) {
                try {
                    AcsRecordingFileStatusUpdatedEventData event = eventGridEvent
                            .getData()
                            .toObject(AcsRecordingFileStatusUpdatedEventData.class);

                    AcsRecordingChunkInfoProperties recordingChunk = event
                            .getRecordingStorageInfo()
                            .getRecordingChunks().get(0);

                    String fileName = String.format("%s.mp4", recordingChunk.getDocumentId());
                    Response<BinaryData> downloadResponse = getCallAutomationAsyncClient().getCallRecordingAsync()
                        .downloadContentWithResponse(recordingChunk.getContentLocation(), null).block();
                    
                    FileOutputStream fos = new FileOutputStream(new File(fileName));
                    fos.write(downloadResponse.getValue().toBytes());

                    logger.log(Level.INFO,
                            String.format("Download media response --> %s", getResponse(downloadResponse)));
                    logger.log(Level.INFO,
                            String.format("successfully downloaded recording file here: %s ", fileName));

                    return new ResponseEntity<>(true, HttpStatus.OK);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.log(Level.SEVERE, e.getMessage());
                    return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else {
                return new ResponseEntity<>(eventGridEvent.getEventType() + " is not handled.",
                        HttpStatus.BAD_REQUEST);
            }
        }

        return new ResponseEntity<>("Event count is not available.", HttpStatus.BAD_REQUEST);
    }
    
    @RequestMapping("/audio/{fileName}")
	public ResponseEntity<Object> loadFile(@PathVariable(value = "fileName", required = false) String fileName) {
		String filePath = "src/main/java/com/communication/simpleivr/audio/" + fileName;
		File file = new File(filePath);
		InputStreamResource resource = null;

		try {
			resource = new InputStreamResource(new FileInputStream(file));
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}

		HttpHeaders headers = new HttpHeaders();
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");

		return ResponseEntity.ok().headers(headers).contentLength(file.length())
				.contentType(MediaType.parseMediaType("audio/x-wav")).body(resource);
	}
    
    private void hangupAsync(String callConnectionId) {
        logger.log(Level.INFO, "Performing Hangup operation");
        getCallAutomationAsyncClient().getCallConnectionAsync(callConnectionId)
                .hangUp(true).block();
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
