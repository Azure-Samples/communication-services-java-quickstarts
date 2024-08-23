package com.communication.callautomation;

import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.SystemEventNames;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.List;

import com.azure.communication.callautomation.*;
import com.azure.communication.callautomation.implementation.models.CommunicationErrorResponseException;
import com.azure.messaging.eventgrid.systemevents.*;
import com.nimbusds.jose.shaded.gson.Gson;
import com.azure.communication.common.*;
import com.azure.core.models.CloudEvent;
import java.io.*;
import java.net.URI;
import java.util.concurrent.*;
import java.nio.file.*;
import java.time.Duration;
import java.awt.*;

@RestController
@Slf4j
@RequestMapping("/api")
public class ProgramSample {
    private static CommunicationIdentifier Callee = null;
    private final AppConfig appConfig;
    private final CallAutomationClient callAutomationClient;
    private static String recordingId = "";

    String handlePrompt = "Welcome to the Contoso Utilities. Thank you!";
    String pstnUserPrompt = "Hello this is contoso recognition test please confirm or cancel to proceed further.";
    String dtmfPrompt = "Thank you for the update. Please type  one two three four on your keypad to close call.";
    String removeParticipantSucceededPrompt = "RemoveParticipantSucceeded!";
    String confirmLabel = "Confirm";
    String cancelLabel = "Cancel";
    public Response<AnswerCallResult> answerCallResult = null;
    public CallConnection callConnection = null;

    @Autowired
    public ProgramSample(
            final AppConfig appConfig) {
        this.appConfig = appConfig;
        callAutomationClient = initClient();
    }

    @GetMapping(path = "/outboundCall")
    public ResponseEntity<String> outboundCall() {
        String callConnectionId = createOutboundCall();
        return ResponseEntity.ok().body("Target participant: "
                + appConfig.getAcsPhonenumber2() +
                ", CallConnectionId: " + callConnectionId);
    }

    @PostMapping("/createCall")
    public CompletableFuture<ResponseEntity<String>> createCall(@RequestParam String targetId) {
        String callbackUriString = String.format("%s?callerId=%s",
                appConfig.getCallBackUri(),
                targetId);

        Callee = new CommunicationUserIdentifier(targetId);
        CallInvite callInvite = new CallInvite((CommunicationUserIdentifier) Callee);

        CreateCallOptions createCallOptions = new CreateCallOptions(callInvite, callbackUriString);
        callAutomationClient.createCallWithResponse(createCallOptions, Context.NONE);
        return CompletableFuture.completedFuture(ResponseEntity.ok().body("Call created successfully."));
    }

    @PostMapping("/createGroupCall")
    public CompletableFuture<ResponseEntity<String>> createGroupCall(@RequestParam String targetId) {
        val targetList = new ArrayList<CommunicationIdentifier>();
        String callbackUriString = String.format("%s?callerId=%s",
                appConfig.getCallBackUri(),
                targetId);
        val pstnEndPoint = new PhoneNumberIdentifier(appConfig.getAcsPhonenumber1());
        val sourceCaller = new PhoneNumberIdentifier(appConfig.getAcsPhonenumber2());
        val voipEndPoint = new CommunicationUserIdentifier(targetId);
        targetList.add(pstnEndPoint);
        targetList.add(voipEndPoint);
        targetList.add(sourceCaller);
        val groupCallOption = new CreateGroupCallOptions(targetList, callbackUriString);
        groupCallOption.setSourceCallIdNumber(sourceCaller);
        callAutomationClient.createGroupCallWithResponse(groupCallOption, Context.NONE);
        return CompletableFuture.completedFuture(ResponseEntity.ok().body("Group Call created successfully."));
    }
  

    @PostMapping("/events")
    public ResponseEntity<SubscriptionValidationResponse> handle(@RequestBody String eventsFromServer)
            throws InterruptedException, ExecutionException {

        List<EventGridEvent> eventGridEvents = EventGridEvent.fromString(eventsFromServer);
        for (EventGridEvent eventGridEvent : eventGridEvents) {
            log.info("Incoming Call event received : " + eventGridEvent);
            // Handle system events

            if (eventGridEvent.getEventType().equals(SystemEventNames.EVENT_GRID_SUBSCRIPTION_VALIDATION)) {
                try {

                    SubscriptionValidationEventData subscriptioneventData = eventGridEvent.getData()
                            .toObject(SubscriptionValidationEventData.class);
                    SubscriptionValidationResponse responseData = new SubscriptionValidationResponse();
                    responseData.setValidationResponse(subscriptioneventData.getValidationCode());
                    log.info("Incoming Call event response received : " + responseData);
                    return ResponseEntity.ok().body(responseData);
                } catch (Exception e) {
                    log.error("Error at subscription validation event {} {}",
                            e.getMessage(),
                            e.getCause());
                }

            }
            if (eventGridEvent.getEventType().equals(SystemEventNames.COMMUNICATION_INCOMING_CALL)) {
                AcsIncomingCallEventData incomingCallEventData = eventGridEvent.getData()
                        .toObject(AcsIncomingCallEventData.class); // (AcsIncomingCallEventData) eventData;
                String callerId = incomingCallEventData.getFromCommunicationIdentifier().getRawId();
                String incomingCallContext = incomingCallEventData.getIncomingCallContext();
                String callbackUriString = this.appConfig.getBasecallbackuri() + "/api/callbacks/" + UUID.randomUUID()
                        + "?callerId="
                        + callerId;
                
                if(Boolean.parseBoolean(appConfig.getIsRedirectCall())) {
                        val target = new PhoneNumberIdentifier(appConfig.getTargetPhonenumber());
                        val caller = new PhoneNumberIdentifier(appConfig.getAcsPhonenumber());
                        val callInvite = new CallInvite(target, caller);

                        RedirectCallOptions redirectCallOptions = new RedirectCallOptions(incomingCallContext, callInvite);
                            
                        callAutomationClient.redirectCallWithResponse(redirectCallOptions, Context.NONE);
                        
                        log.info(("Call Redirected Successfully"));
                        
                        
                        }

                else if (Boolean.parseBoolean(appConfig.getIsRejectCall())) {
                    // Assuming callAutomationClient is an instance of a class providing
                    // asynchronous call rejection method
                    callAutomationClient.rejectCall(incomingCallContext); // Wait for the rejection operation to
                                                                          // complete
                    log.info("Call Rejected, reject call setting is: "
                            + Boolean.parseBoolean(appConfig.getIsRejectCall()));
                } 
                
                else {
                    TranscriptionOptions transcriptionOptions = new TranscriptionOptions(appConfig.getWebSocketUrl(), TranscriptionTransport.WEBSOCKET, "en-US", false);
                    MediaStreamingOptions mediaStreamingOptions = new MediaStreamingOptions(appConfig.getWebSocketUrl(), MediaStreamingTransport.WEBSOCKET, MediaStreamingContentType.AUDIO, MediaStreamingAudioChannel.UNMIXED, true);
                    AnswerCallOptions options = new AnswerCallOptions(incomingCallContext, callbackUriString)
                            .setCallIntelligenceOptions(new CallIntelligenceOptions().setCognitiveServicesEndpoint(appConfig.getCognitiveServicesUrl()))
                            .setTranscriptionOptions(transcriptionOptions)
                            .setMediaStreamingOptions(mediaStreamingOptions);
                            
                    answerCallResult = this.callAutomationClient
                            .answerCallWithResponse(options, Context.NONE);
                     log.info("Answer call result: "
                             + answerCallResult.getValue().getCallConnectionProperties().getCallConnectionId());
                     log.info("Correlation ID is : " + answerCallResult.getValue().getCallConnectionProperties().getCorrelationId());
                    //  if(Boolean.parseBoolean(appConfig.getIsRedirectCall())) {
                    //     val target = new PhoneNumberIdentifier(appConfig.getTargetPhonenumber());
                    //     val caller = new PhoneNumberIdentifier(appConfig.getAcsPhonenumber());
                    //     val callInvite = new CallInvite(target, caller);
                            
                    //     callAutomationClient.redirectCall(incomingCallContext, callInvite);
                    //     log.info(("Call Redirected Successfully"));
                    //     }
                    return ResponseEntity.ok().build();
                }
            }
            if (eventGridEvent.getEventType().equals(SystemEventNames.COMMUNICATION_RECORDING_FILE_STATUS_UPDATED)) {
                log.info("The event received for recording file status update");
                AcsRecordingFileStatusUpdatedEventData statusUpdated = eventGridEvent.getData()
                        .toObject(AcsRecordingFileStatusUpdatedEventData.class);
                String metadataLocation = statusUpdated.getRecordingStorageInfo().getRecordingChunks().get(0)
                        .getMetadataLocation();
                String contentLocation = statusUpdated.getRecordingStorageInfo().getRecordingChunks().get(0)
                        .getContentLocation();
                if (!Boolean.parseBoolean(appConfig.getIsByos())) {
                    downloadRecording(contentLocation);
                    downloadMetadata(metadataLocation);
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping(path = "/callbacks/{contextid}")
    public ResponseEntity<String> callbackEvents(@RequestBody final String reqBody,
            @RequestParam final String callerId) {
        List<CallAutomationEventBase> events = CallAutomationEventParser.parseEvents(reqBody);
        for (CallAutomationEventBase event : events) {
            String callConnectionId = event.getCallConnectionId();
            if (event instanceof CallConnected) {
                log.info("Call connected, call connection Id:--> {}", callConnectionId);
                PagedIterable<CallParticipant> response = callAutomationClient.getCallConnection(callConnectionId)
                            .listParticipants();
                log.info("*************************************");
                int count = 0;
                for (CallParticipant participant : response) {
                    count++;
                    System.out.println(participant.getIdentifier().getRawId());
                    System.out.println("call has been Muted" + participant.isMuted()); 
                }
                log.info("TOTAL PARTICIPANTS IN CALL:--> " + count);
                
                log.info("ANSWERED For:--> " + callAutomationClient.getCallConnection(callConnectionId).getCallProperties().getAnsweredFor());

                // //Start Transcription
                // startTranscription(callConnectionId);
                // try {
                //     TimeUnit.SECONDS.sleep(10);    
                // } catch (Exception e) {
                    
                // }


                // //#region Media Streaming
                // // start Media Streaming
                // startMediaStreamingOptions(callConnectionId);
                // log.info("Start Media Streaming.....");
                // try {
                //     TimeUnit.SECONDS.sleep(5);    
                // } catch (Exception e) {
                    
                // }

                // // stop Media Streaming
                // stopMediaStreamingOptions(callConnectionId);
                // log.info("Stopped Media streaming....");
                // try {
                //     TimeUnit.SECONDS.sleep(5);    
                // } catch (Exception e) {
                    
                // }
                // // start Media Streaming
                // startMediaStreamingOptions(callConnectionId);
                // log.info("Start Media Streaming.....");

                // try {
                //     TimeUnit.SECONDS.sleep(5);    
                // } catch (Exception e) {
                    
                // }
                // // stop Media Streaming
                // stopMediaStreamingOptions(callConnectionId);
                // var callConnectionProperties = callAutomationClient.getCallConnection(callConnectionId).getCallProperties();
                // log.info("State{}" ,callConnectionProperties.getCallConnectionState());
                // log.info("Stopped Media streaming....");
                // try {
                //     TimeUnit.SECONDS.sleep(5);    
                // } catch (Exception e) {   
                // }
                // // #endregion
  
                                     

                callConnection = callAutomationClient.getCallConnection(callConnectionId);                  
                String serverCallId = callConnection.getCallProperties().getServerCallId();
                // StartRecordingOptions recordingOptions = new StartRecordingOptions(new ServerCallLocator(
                //         serverCallId))
                //         .setRecordingContent(RecordingContent.AUDIO)
                //         .setRecordingChannel(RecordingChannel.MIXED)
                //         .setRecordingFormat(RecordingFormat.WAV)
                //  .setPauseOnStart(Boolean.parseBoolean(appConfig.getIsPauseOnStart()))
                //  .setRecordingStorage(Boolean.parseBoolean(appConfig.getIsByos())
                //  && appConfig.getBringYourOwnStorageUrl() != null
                //  ? new AzureBlobContainerRecordingStorage(appConfig.getBringYourOwnStorageUrl())
                //  : null);

                //  log.info("Pause On Start-->: " + recordingOptions.isPauseOnStart());
                 

                // recordingId = callAutomationClient.getCallRecording().startWithResponse(recordingOptions, Context.NONE)
                //         .getValue().getRecordingId();

                // log.info("Call recording id--> " + recordingId);

                

                // Add PstnUser
                CallInvite callInvite = new CallInvite(
                        new PhoneNumberIdentifier(appConfig.getTargetPhonenumber()),
                        new PhoneNumberIdentifier(appConfig.getAcsPhonenumber1()));
                val addParticipantOptions = new AddParticipantOptions(callInvite)
                        .setOperationContext("addPstnUserContext");
                Response<AddParticipantResult> addParticipantResult = callConnection.addParticipantWithResponse(
                        addParticipantOptions,
                        Context.NONE);
                log.info("Adding PstnUser to the call: {}",
                        addParticipantResult.getValue().getInvitationId());


                //  //#region Transfer Call
                //  PhoneNumberIdentifier caller = new PhoneNumberIdentifier(appConfig.getAcsPhonenumber1());
                //  PhoneNumberIdentifier target = new PhoneNumberIdentifier("+919160985789");
                //  TransferCallToParticipantOptions transferOption = new TransferCallToParticipantOptions(target);
                //  transferOption.setOperationContext("transferCallContext");
                //  //transferOption.setSourceCallerIdNumber(caller);
                //  transferOption.setTransferee(caller);
 
                //  // Sending event to a non-default endpoint.
                //  transferOption.setOperationCallbackUrl(appConfig.getBasecallbackuri());
                //  //TransferCallResult result = client.getCallConnection(callConnectionId).transferCallToParticipant(target);
                //  var result = callAutomationClient.getCallConnection(callConnectionId).transferCallToParticipantWithResponse(transferOption, Context.NONE);
                //  log.info("Call Transferred successfully");
                //  //#endregion




            } else if (event instanceof RecognizeCompleted) {
                log.info("Recognize Completed event received for Call Connection ID: {}", callConnectionId);
                if (event instanceof RecognizeCompleted) {
                    RecognizeCompleted completedEvent = (RecognizeCompleted) event;
                    log.info("Recognize completed event received for connection id: "
                            + completedEvent.getCallConnectionId());
                    RecognizeResult recognizeResult = completedEvent.getRecognizeResult().get();
                    if (recognizeResult instanceof ChoiceResult) {
                        ChoiceResult collectChoiceResult = (ChoiceResult) recognizeResult;
                        String labelDetected = collectChoiceResult.getLabel();
                        String phraseDetected = collectChoiceResult.getRecognizedPhrase();
                        log.info("Detected label:--> " + labelDetected);
                        log.info("Detected phrase:--> " + phraseDetected);
                        if (labelDetected.toLowerCase().equals(confirmLabel.toLowerCase())) {
                            log.info("Moving towards dtmf test.");
                            log.info("Recognize completed successfully, labelDetected="
                                    + labelDetected + ", phraseDetected=" + phraseDetected);
                            handleRecognize(dtmfPrompt, callConnectionId, this.appConfig.getTargetPhonenumber(),
                                    true);

                // // stop Media Streaming
                // stopMediaStreamingOptions(callConnectionId);
                // var callConnectionProperties = callAutomationClient.getCallConnection(callConnectionId).getCallProperties();
                // log.info("State{}" ,callConnectionProperties.getCallConnectionState());
                // log.info("Stopped Media streaming....");
                // try {
                //     TimeUnit.SECONDS.sleep(5);    
                // } catch (Exception e) {   
                // }

                // // start Media Streaming
                // startMediaStreamingOptions(callConnectionId);
                // log.info("Start Media Streaming.....");
                // try {
                //     TimeUnit.SECONDS.sleep(5);    
                // } catch (Exception e) {   
                // }

                // // stop Media Streaming
                // stopMediaStreamingOptions(callConnectionId);
                // log.info("Stopped Media streaming....");
                // try {
                //     TimeUnit.SECONDS.sleep(5);    
                // } catch (Exception e) {   
                // }
                
                // // call on Hold 
                // hold(callConnectionId);
                // // // var participant = client.getCallConnection(callConnectionId).getParticipant(new PhoneNumberIdentifier(appConfig.getTargetphonenumber()));
                // // // var isParticipantHold = participant.isOnHold();
                // // // log.info("Is participant on hold ----> {}", isParticipantHold);
                // log.info("Call On Hold successfully");
                // try {
                //     TimeUnit.SECONDS.sleep(5);    
                // } catch (Exception e) {    
                // }

                // // Call Unhold
                // unhold(callConnectionId);
                // // // isParticipantHold = participant.isOnHold();
                // // // log.info("Is participant on hold ----> {}", isParticipantHold);
                // log.info("Call UnHolded successfully");

                // // Stop Transcription
                // stopTranscription(callConnectionId);

                        } else {
                            log.info("Moving towards continuous dtmf & send dtmf tones test.");
                            startContinuousDtmf(callConnectionId);
                        }
                    } else if (recognizeResult instanceof DtmfResult) {
                        String context = completedEvent.getOperationContext();
                        log.info("Current context-->" + context);
                        callConnection.removeParticipant(
                                new PhoneNumberIdentifier(this.appConfig.getTargetPhonenumber()));
                    }
                }
            } else if (event instanceof RecognizeFailed) {
                log.error("Received failed event: {}", ((CallAutomationEventBaseWithReasonCode) event)
                        .getResultInformation().getMessage());

            } else if (event instanceof CallTransferAccepted) {
                log.info("Call transfer accepted event received for connection id:{}", callConnectionId);
            } else if (event instanceof CallTransferFailed) {
                log.info("Call transfer failed event received for connection id:{}", callConnectionId);
                
            } else if(event instanceof TranscriptionStarted) {
                log.info("TranscriptionStarted event triggered");
                TranscriptionStarted acsEvent = (TranscriptionStarted) event;
                log.info("*****Transcription Status --> "
                        + acsEvent.getTranscriptionUpdateResult().getTranscriptionStatus());
                log.info("*****Transcritpion Status Details --> "
                        + acsEvent.getTranscriptionUpdateResult().getTranscriptionStatusDetails());

            }
            else if(event instanceof TranscriptionStopped) {
                log.info("TranscriptionStopped event triggered");
                TranscriptionStopped acsEvent = (TranscriptionStopped) event;
                log.info("*****Transcription Status --> "
                        + acsEvent.getTranscriptionUpdateResult().getTranscriptionStatus());
                log.info("*****Transcritpion Status Details --> "
                        + acsEvent.getTranscriptionUpdateResult().getTranscriptionStatusDetails());                
                
            }
            else if(event instanceof TranscriptionFailed) {
                log.info("TranscriptionFailed event triggered");
                                TranscriptionFailed acsEvent = (TranscriptionFailed) event;
                log.info("Message --> " + acsEvent.getResultInformation().getMessage());
                log.info("Code --> " + acsEvent.getResultInformation().getCode());
                log.info("SubCode --> " + acsEvent.getResultInformation().getSubCode());
            }
            else if(event instanceof MediaStreamingStarted) {
                log.info("MediaStreamingStarted event triggered.");
                MediaStreamingStarted acsEvent = (MediaStreamingStarted) event;
                log.info("*****Media Streaming Status --> "
                        + acsEvent.getMediaStreamingUpdateResult().getMediaStreamingStatus());
                log.info("*****Media Streaming Status Details --> "
                        + acsEvent.getMediaStreamingUpdateResult().getMediaStreamingStatusDetails());
                log.info("*****Media Streaming Content --> "
                        + acsEvent.getMediaStreamingUpdateResult().getContentType());
                log.info("*****MediaOperationContext --> " + acsEvent.getOperationContext());    
            }
            else if(event instanceof MediaStreamingStopped) {
                log.info("MediaStreamingStopped event triggered.");
                MediaStreamingStopped acsEvent = (MediaStreamingStopped) event;
                log.info("*****Media Streaming Status --> "
                        + acsEvent.getMediaStreamingUpdateResult().getMediaStreamingStatus());
                log.info("*****Media Streaming Status Details --> "
                        + acsEvent.getMediaStreamingUpdateResult().getMediaStreamingStatusDetails());
                log.info("*****Media Streaming Content --> "
                        + acsEvent.getMediaStreamingUpdateResult().getContentType());
                log.info("*****MediaOperationContext --> " + acsEvent.getOperationContext());
            }
            else if(event instanceof MediaStreamingFailed) {
                log.info("MediaStreamingFailed event triggered.");
                MediaStreamingFailed acsEvent = (MediaStreamingFailed) event;
                log.info("Message --> " + acsEvent.getResultInformation().getMessage());
                log.info("Code --> " + acsEvent.getResultInformation().getCode());
            }
            else if(event instanceof PlayStarted) {
                log.info("PlayStarted event triggered.");
            }
            else if (event instanceof PlayCompleted) {
                PlayCompleted playCompletedEvent = (PlayCompleted) event;

                log.info("Play completed event received for connection id: "
                        + playCompletedEvent.getCallConnectionId());
                Toolkit.getDefaultToolkit().beep();

                if (appConfig.getTeamsComplianceUserId() != null
                        && !appConfig.getTeamsComplianceUserId().isEmpty()) {
                    MicrosoftTeamsUserIdentifier participant = new MicrosoftTeamsUserIdentifier(
                            appConfig.getTeamsComplianceUserId());
                    CallInvite callInvite = new CallInvite(participant);
                    AddParticipantOptions addParticipantOptions = new AddParticipantOptions(callInvite);
                    callConnection.addParticipantWithResponse(addParticipantOptions, Context.NONE);
                }

                // CompletableFuture<String> stateFuture = getRecordingState(recordingId);
                // String state = stateFuture.join();
                // try {
                //     Thread.sleep(5000);
                // } catch (InterruptedException e) {
                //     log.error(e.getMessage());
                //     Thread.currentThread().interrupt();
                // }

                // if (state.equals("active")) {
                //     callAutomationClient.getCallRecording().pauseWithResponse(recordingId,
                //             Context.NONE);
                //     log.info("Recording is Paused.");
                //     getRecordingState(recordingId).join();
                // } else {
                //     callAutomationClient.getCallRecording().resumeWithResponse(recordingId,
                //             Context.NONE);
                //     log.info("Recording is Resumed.");
                //     getRecordingState(recordingId).join();
                // }

                // try {
                //     Thread.sleep(5000);
                // } catch (InterruptedException e) {
                //     log.error(e.getMessage());
                //     Thread.currentThread().interrupt();
                // }
                // callAutomationClient.getCallRecording().stopWithResponse(recordingId, Context.NONE);
                // log.info("Recording is Stopped.");

                // CompletableFuture<String> stateFuture = getRecordingState(recordingId);
                // String initialState = stateFuture.join();

                // try {
                //     Thread.sleep(5000);
                // } catch (InterruptedException e) {
                //     log.error(e.getMessage());
                //     Thread.currentThread().interrupt();
                // }

                // // Handle initial state
                // if (initialState.equalsIgnoreCase("Active")) {
                //     // Pause the recording
                //     callAutomationClient.getCallRecording().pauseWithResponse(recordingId, Context.NONE);
                //     log.info("Recording is Paused.");

                //     // Check state after pausing
                //     String stateAfterPause = getRecordingState(recordingId).join();
                //     log.info("Recording state after pause: " + stateAfterPause);

                    

                //     // Resume the recording
                //     callAutomationClient.getCallRecording().resumeWithResponse(recordingId, Context.NONE);
                //     log.info("Recording is Resumed.");

                //     // Check state after resuming
                //     String stateAfterResume = getRecordingState(recordingId).join();
                //     log.info("Recording state after resume: " + stateAfterResume);

                //     try {
                //         Thread.sleep(5000);
                //     } catch (InterruptedException e) {
                //         log.error(e.getMessage());
                //         Thread.currentThread().interrupt();
                //     }
                // } else if (initialState.equalsIgnoreCase("Inactive")) {
                //     // Resume the recording
                //     callAutomationClient.getCallRecording().resumeWithResponse(recordingId, Context.NONE);
                //     log.info("Recording is Resumed.");

                //     // Check state after resuming
                //     String stateAfterResume = getRecordingState(recordingId).join();
                //     log.info("Recording state after resume: " + stateAfterResume);

                //     try {
                //         Thread.sleep(5000);
                //     } catch (InterruptedException e) {
                //         log.error(e.getMessage());
                //         Thread.currentThread().interrupt();
                //     }
                // } else {
                //     log.error("Unexpected initial recording state: " + initialState);
                // }

                // // Stop the recording for both scenarios
                // callAutomationClient.getCallRecording().stopWithResponse(recordingId, Context.NONE);
                // log.info("Recording is Stopped.");


                CallConnection callConnection = callAutomationClient
                        .getCallConnection(playCompletedEvent.getCallConnectionId());
                callConnection.hangUpWithResponse(true, Context.NONE);
                
            }

            else if (event instanceof PlayFailed) {
                log.error("Received Play Failed event: {}", ((CallAutomationEventBaseWithReasonCode) event)
                        .getResultInformation().getMessage());
                PlayFailed playFailedEvent = (PlayFailed) event;
                log.info("Play failed event received for connection id: "
                        + playFailedEvent.getCallConnectionId());
                // PlayResultInformation resultInformation =
                // playFailedEvent.getResultInformation();
                callAutomationClient.getCallConnection(playFailedEvent.getCallConnectionId())
                        .hangUpWithResponse(true, Context.NONE);

            } else if (event instanceof CallDisconnected) {
                log.info("Received Call Disconnected event for Call Connection ID: {}", callConnectionId);
                log.info("Correlation ID is : " + answerCallResult.getValue().getCallConnectionProperties().getCorrelationId());
            } else if (event instanceof AddParticipantFailed) {
                AddParticipantFailed addPartFailedEvent = (AddParticipantFailed) event;
                log.info("AddParticipantFailed event received for connection id: "
                        + event.getCallConnectionId());
                log.info("Message: " + (addPartFailedEvent.getResultInformation().getMessage() != null
                        ? addPartFailedEvent.getResultInformation().getMessage()
                        : "null"));
            } else if (event instanceof AddParticipantSucceeded) {
                log.info("AddParticipantSucceeded event received for connection id: "
                        + event.getCallConnectionId());
                log.info("Participant: " + new Gson().toJson(event));

                if ("addPstnUserContext".equals(event.getOperationContext())) {
                    log.info("PSTN user added.");

                    PagedIterable<CallParticipant> response = callAutomationClient.getCallConnection(callConnectionId)
                            .listParticipants();

                    // log.info(msg:"Total participant in call-->");
                    if (response != null) {

                        // MuteParticipantResult muteResponse = callAutomationClient.getCallConnection(callConnectionId)
                        //         .muteParticipant(Callee);

                        // if (muteResponse != null) {
                        //     log.info("Participant is muted. Waiting for confirmation...");
                        //     CallParticipant participant =
                        //     callAutomationClient.getCallConnection(callConnectionId)
                        //     .getParticipant(Callee);
                        //     if (participant != null) {
                        //     log.info("Is participant muted: " + participant.isMuted());
                        //     log.info("Mute participant test completed.");
                        //     }
                        // }
                        log.info("*************************************");
                        int count = 0;
                        for (CallParticipant participant : response) {
                        count++;
                        System.out.println(participant.getIdentifier().getRawId());
                        System.out.println("call has been Muted" + participant.isMuted()); 
                }
                log.info("TOTAL PARTICIPANTS IN CALL:--> " + count);
                        
                        handleRecognize(pstnUserPrompt, callConnectionId, this.appConfig.getTargetPhonenumber(), false);
                        // callAutomationClient.getCallConnection(callConnectionId).getCallMedia().cancelAllMediaOperations();
                        // log.info("cancelled all media operations successfully");
                    }
                }

                if ("addTeamsComplianceUserContext".equals(event.getOperationContext())) {
                    log.info("Microsoft teams user added.");
                }
            } else if (event instanceof RemoveParticipantSucceeded) {

                RemoveParticipantSucceeded eventData = (RemoveParticipantSucceeded) event;
                log.info("RemoveParticipantSucceeded event received for connection id: "
                        + eventData.getCallConnectionId());
                log.info("Received RemoveParticipantSucceeded event");
                try {
                    handlePlayTo(removeParticipantSucceededPrompt,
                            "removeParticipantSucceededPromptContext", callConnectionId, callerId);
                    // handlePlayLoopAsync(callConnectionMedia, removeParticipantSucceededPrompt,
                    // "removeParticipantSucceededPromptContext").get();
                    // callConnectionMedia.cancelAllMediaOperationsAsync().get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            else if (event instanceof RemoveParticipantFailed) {

                RemoveParticipantFailed eventData = (RemoveParticipantFailed) event;
                log.info("RemoveParticipantFailed event received for connection id: "
                        + eventData.getCallConnectionId());
                log.info("Received RemoveParticipantFailed event");

            }

            else if (event instanceof ContinuousDtmfRecognitionToneReceived) {

                ContinuousDtmfRecognitionToneReceived eventData = (ContinuousDtmfRecognitionToneReceived) event;
                log.info("Received ContinuousDtmfRecognitionToneReceived event");
                log.info("Tone received:--> " + eventData.getTone());
                log.info("SequenceId:--> " + eventData.getSequenceId());
                stopContinuousDtmf(callConnectionId);

            }

            else if (event instanceof ContinuousDtmfRecognitionToneFailed) {

                ContinuousDtmfRecognitionToneFailed eventData = (ContinuousDtmfRecognitionToneFailed) event;
                log.info("Received ContinuousDtmfRecognitionToneReceived event");
                log.info("Received ContinuousDtmfRecognitionToneFailed event");
                log.info("Message:-->" + eventData.getResultInformation().getMessage());

            } else if (event instanceof ContinuousDtmfRecognitionStopped) {

                log.info("Received ContinuousDtmfRecognitionStopped event");
                startSendingDtmfTone(callConnectionId);

            }

            else if (event instanceof SendDtmfTonesCompleted) {

                log.info("Received SendDtmfTonesCompleted event");
                callConnection.removeParticipant(new PhoneNumberIdentifier(appConfig.getTargetPhonenumber()));
                log.info("Send Dtmf tone completed. " + appConfig.getTargetPhonenumber()
                        + " will be removed from call.");

            }

            else if (event instanceof SendDtmfTonesFailed) {

                SendDtmfTonesFailed evnt = (SendDtmfTonesFailed) event;
                log.info("Received SendDtmfTonesFailed event");
                log.info("Message:-->" + evnt.getResultInformation().getMessage());

            }

            else if (event instanceof RecordingStateChanged) {

                log.info("Received RecordingStateChanged event");

            }

            // else if(event instanceof TeamsComplianceRecordingStateChanged) {
            // TeamsComplianceRecordingStateChanged
            // teamsComplianceRecordingStateChangedEvent =
            // (TeamsComplianceRecordingStateChanged) event;
            // log.info("Received TeamsComplianceRecordingStateChanged event");
            // log.info("CorrelationId:->"
            // + teamsComplianceRecordingStateChangedEvent.getCorrelationId());

            // }

            else if (event instanceof CallDisconnected) {

                log.info("Received CallDisconnected event");
               

            }
        }
        return ResponseEntity.ok().body("");
    }

    private CompletableFuture<Void> handleVoiceMessageNoteAsync(CallMedia callConnectionMedia, String callerId) {
        String textToPlay = "Sorry, all of our agents are busy on a call. Please leave your phone number and your message after the beep sound.";
        TextSource voiceMessageNote = new TextSource()
                .setText(textToPlay)
                .setVoiceName("en-US-NancyNeural");
        callConnectionMedia.playToAll(voiceMessageNote);
        return null;
    }

    private CompletableFuture<Void> downloadRecording(String contentLocation) {
        String downloadsPath = Paths.get(System.getProperty("user.home"), "Downloads").toString();
        try {
            callAutomationClient.getCallRecording().downloadTo(contentLocation,
                    new FileOutputStream(Paths.get(downloadsPath, "test.wav").toString()));
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    private CompletableFuture<Void> downloadMetadata(String contentLocation) {
        String downloadsPath = Paths.get(System.getProperty("user.home"), "Downloads").toString();
        try {
            callAutomationClient.getCallRecording().downloadTo(contentLocation,
                    new FileOutputStream(Paths.get(downloadsPath, "recordingMetadata.json").toString()));
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    private CompletableFuture<String> getRecordingState(String recordingId) {
        RecordingStateResult result = this.callAutomationClient.getCallRecording().getState(recordingId);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            Thread.currentThread().interrupt();
        }
        String state = result.getRecordingState().toString();
        log.info("Recording Status:->  " + state);
        return CompletableFuture.completedFuture(state);
    }

    public void handleRecognize(final String message,
            final String callConnectionId,
            final String callerId,
            boolean dtmf) {
        List<RecognitionChoice> choices = getChoices();

        TextSource greetingPlaySource = new TextSource()
                .setText(message)
                .setVoiceName("en-US-NancyNeural");
        PhoneNumberIdentifier target = new PhoneNumberIdentifier(callerId);

        CallMediaRecognizeOptions recognizeOptions;
        if (dtmf) {
            recognizeOptions = new CallMediaRecognizeDtmfOptions(target, 4);
            ((CallMediaRecognizeDtmfOptions) recognizeOptions).setInterruptPrompt(false);
            ((CallMediaRecognizeDtmfOptions) recognizeOptions).setInitialSilenceTimeout(Duration.ofSeconds(15));
            ((CallMediaRecognizeDtmfOptions) recognizeOptions).setPlayPrompt(greetingPlaySource);
            ((CallMediaRecognizeDtmfOptions) recognizeOptions).setOperationContext("dtmfContext");
            ((CallMediaRecognizeDtmfOptions) recognizeOptions).setInterToneTimeout(Duration.ofSeconds(5));
        } else {
            recognizeOptions = new CallMediaRecognizeChoiceOptions(target, choices);
            ((CallMediaRecognizeChoiceOptions) recognizeOptions).setInterruptPrompt(false);
            ((CallMediaRecognizeChoiceOptions) recognizeOptions).setInitialSilenceTimeout(Duration.ofSeconds(10));
            ((CallMediaRecognizeChoiceOptions) recognizeOptions).setPlayPrompt(greetingPlaySource);
            ((CallMediaRecognizeChoiceOptions) recognizeOptions).setOperationContext("recognizeContext");
        }

        callAutomationClient.getCallConnection(callConnectionId)
                .getCallMedia()
                .startRecognizing(recognizeOptions);
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
            callAutomationClient.getCallConnection(callConnectionId)
                    .getCallMedia()
                    .playWithResponse(playOptions, Context.NONE);
        } catch (Exception e) {
            log.error("Error occurred when playing media to participant {} {}",
                    e.getMessage(),
                    e.getCause());
        }
    }

    public void startContinuousDtmf(final String callConnectionId) {
        callAutomationClient.getCallConnection(callConnectionId)
                .getCallMedia()
                .startContinuousDtmfRecognition(new PhoneNumberIdentifier(appConfig.getTargetPhonenumber()));
        System.out.println("Continuous Dtmf recognition started. Press one on dialpad.");
    }

    public void stopContinuousDtmf(final String callConnectionId) {
        callAutomationClient.getCallConnection(callConnectionId)
                .getCallMedia()
                .stopContinuousDtmfRecognition(new PhoneNumberIdentifier(appConfig.getTargetPhonenumber()));
        System.out.println("Continuous Dtmf recognition stopped. Wait for sending dtmf tones.");
    }

    public void startSendingDtmfTone(final String callConnectionId) {
        List<DtmfTone> tones = Arrays.asList(DtmfTone.ZERO, DtmfTone.ONE);
        callAutomationClient.getCallConnection(callConnectionId)
                .getCallMedia().sendDtmfTones(tones, new PhoneNumberIdentifier(appConfig.getTargetPhonenumber()));
        System.out.println("Send dtmf tones started. Respond over phone.");
    }

    private List<RecognitionChoice> getChoices() {
        List<RecognitionChoice> choices = new ArrayList<>();

        List<String> confirmLabels = Arrays.asList("Confirm", "First", "One");
        RecognitionChoice confirmChoice = new RecognitionChoice().setLabel("Confirm").setPhrases(confirmLabels)
                .setTone(DtmfTone.ONE);
        choices.add(confirmChoice);

        List<String> cancelLabels = Arrays.asList("Cancel", "Second", "Two");
        RecognitionChoice cancelChoice = new RecognitionChoice().setLabel("Cancel").setPhrases(cancelLabels)
                .setTone(DtmfTone.TWO);
        choices.add(cancelChoice);

        return choices;
    }

    private String createOutboundCall() {
        try {
            PhoneNumberIdentifier caller = new PhoneNumberIdentifier(appConfig.getAcsPhonenumber1());
            PhoneNumberIdentifier target = new PhoneNumberIdentifier(appConfig.getAcsPhonenumber2());
            CallInvite callInvite = new CallInvite(target, caller);
            
            CallIntelligenceOptions callIntelligenceOptions = new CallIntelligenceOptions().setCognitiveServicesEndpoint(appConfig.getCognitiveServicesUrl());
            //TranscriptionOptions transcriptionOptions = new TranscriptionOptions(appConfig.getWebSocketUrl(), TranscriptionTransport.WEBSOCKET, "en-US", false);
            //MediaStreamingOptions mediaStreamingOptions = new MediaStreamingOptions(appConfig.getWebSocketUrl(), MediaStreamingTransport.WEBSOCKET, MediaStreamingContentType.AUDIO, MediaStreamingAudioChannel.UNMIXED);
            //mediaStreamingOptions.setStartMediaStreaming(false);
        
            CreateCallOptions createCallOptions = new CreateCallOptions(callInvite, appConfig.getCallBackUri());
            createCallOptions.setCallIntelligenceOptions(callIntelligenceOptions);
            //createCallOptions.setTranscriptionOptions(transcriptionOptions);
            //createCallOptions.setMediaStreamingOptions(mediaStreamingOptions);

            Response<CreateCallResult> result = callAutomationClient.createCallWithResponse(createCallOptions, Context.NONE);
            
            log.info("correlationId... {}", result.getValue().getCallConnectionProperties().getCorrelationId());
            return result.getValue().getCallConnectionProperties().getCallConnectionId();
        } catch (CommunicationErrorResponseException e) {
            log.error("Error when creating call: {} {}",
                    e.getMessage(),
                    e.getCause());
            return "";
        }
    }

    private void startMediaStreamingOptions(final String callConnectionId){
        StartMediaStreamingOptions startOptions = new StartMediaStreamingOptions()
                                                        .setOperationContext("startMediaStreamingContext")
                                                        .setOperationCallbackUrl(appConfig.getBasecallbackuri());
        //without options
        callAutomationClient.getCallConnection(callConnectionId)
                    .getCallMedia()
                    .startMediaStreaming();
        // //with options
        // client.getCallConnection(callConnectionId)
        //             .getCallMedia()
        //             .startMediaStreamingWithResponse(startOptions, Context.NONE);
    }

    private void stopMediaStreamingOptions(final String callConnectionId){
        StopMediaStreamingOptions stopOptions = new StopMediaStreamingOptions()
                                                        .setOperationCallbackUrl(appConfig.getBasecallbackuri());

        // // without options
        // client.getCallConnection(callConnectionId)
        //             .getCallMedia()
        //             .stopMediaStreaming();
        // with options
        callAutomationClient.getCallConnection(callConnectionId)
                    .getCallMedia()
                    .stopMediaStreamingWithResponse(stopOptions, Context.NONE);
    }

    private void startTranscription(String callConnectionId) {
        StartTranscriptionOptions transcriptionOptions = new StartTranscriptionOptions()
                                                        .setOperationContext("startMediaStreamingContext");
        //// with options
        // client.getCallConnection(callConnectionId)
        //         .getCallMedia()
        //         .startTranscriptionWithResponse(transcriptionOptions, Context.NONE);

        // without options
        callAutomationClient.getCallConnection(callConnectionId)
                    .getCallMedia()
                    .startTranscription();
                
    }
    
    private void stopTranscription(String callConnectionId) {
        StopTranscriptionOptions stopTranscriptionOptions = new StopTranscriptionOptions()
                                                            .setOperationContext("stopTranscription");
        //// with options
        // client.getCallConnection(callConnectionId)
        //         .getCallMedia()
        //         .stopTranscriptionWithResponse(stopTranscriptionOptions, Context.NONE);

        // without options
        callAutomationClient.getCallConnection(callConnectionId)
                    .getCallMedia()
                    .stopTranscription();
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
