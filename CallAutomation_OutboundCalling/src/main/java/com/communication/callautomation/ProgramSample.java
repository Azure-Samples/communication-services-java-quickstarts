package com.communication.callautomation;

import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.CallConnection;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.communication.identity.implementation.models.CommunicationErrorResponseException;
import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
public class ProgramSample {
    private AppConfig appConfig;
    private CallAutomationClient client;
    private static String recordingId = "";

    private String MainMenu = """
            Hello this is Contoso Bank, we’re calling in regard to your appointment tomorrow
            at 9am to open a new account. Please say confirm if this time is still suitable for you or say cancel
            if you would like to cancel this appointment.
            """;
    private String confirmLabel = "Confirm";
    private String cancelLabel = "Cancel";
    private String confirmedText = "Thank you for confirming your appointment tomorrow at 9am, we look forward to meeting with you.";
    private String cancelText = "Your appointment tomorrow at 9am has been cancelled. Please call the bank directly if you would like to rebook for another date and time.";
    private String customerQueryTimeout = "I am sorry I didn't receive a response, please try again.";
    private String noResponse = "I didn't receive an input, we will go ahead and confirm your appointment. Goodbye";
    private String invalidAudio = "I'm sorry, I didn't understand your response, please try again.";
    private String retryContext = "Retry";
    public CallConnection callConnection = null;
    public String newParticipantContact = null;

    public ProgramSample(final AppConfig appConfig) {
        this.appConfig = appConfig;
        client = initClient();
    }

    @GetMapping(path = "/outboundCall")
    public ResponseEntity<String> outboundCall() {
        String callConnectionId = createOutboundCall();
        return ResponseEntity.ok().body("Target participant: "
                + appConfig.getTargetphonenumber() +
                ", CallConnectionId: " + callConnectionId);
    }

    @PostMapping(path = "/api/callback")
    public ResponseEntity<String> callbackEvents(@RequestBody final String reqBody) {
        List<CallAutomationEventBase> events = CallAutomationEventParser.parseEvents(reqBody);
        for (CallAutomationEventBase event : events) {
            String callConnectionId = event.getCallConnectionId();

            if (event instanceof CallConnected) {
                log.info(
                        "Received call event callConnectionID: {}, serverCallId: {}, correlation Id: {}",
                        callConnectionId,
                        event.getServerCallId(),
                        event.getCorrelationId());
                log.info("Received Call Connected event");

                /* Add a PSTN user to the call */
                callConnection = client.getCallConnection(callConnectionId);
                CallInvite callInvite = new CallInvite(
                new PhoneNumberIdentifier(newParticipantContact),
                new PhoneNumberIdentifier(appConfig.getCallerphonenumber()));
                val addParticipantOptions = new AddParticipantOptions(callInvite)
                .setOperationContext("addPstnUserContext");
                Response<AddParticipantResult> addParticipantResult =
                callConnection.addParticipantWithResponse(
                addParticipantOptions,
                Context.NONE);
                log.info("Adding PstnUser to the call: {}",
                addParticipantResult.getValue().getInvitationId());

                /** prepare recognize tones */
                // startRecognizingWithChoiceOptions(callConnectionId, MainMenu,
                // appConfig.getTargetphonenumber(),
                // "mainmenu");

                /** Start Call Recording */
                // String serverCallId = callConnection.getCallProperties().getServerCallId();
                // StartRecordingOptions recordingOptions = new StartRecordingOptions(new
                // ServerCallLocator(
                // serverCallId))
                // .setRecordingContent(RecordingContent.AUDIO)
                // .setRecordingChannel(RecordingChannel.UNMIXED)
                // .setRecordingFormat(RecordingFormat.WAV)
                // .setPauseOnStart(Boolean.parseBoolean(appConfig.getIsPauseOnStart()))
                // .setRecordingStorage(Boolean.parseBoolean(appConfig.getIsByos())
                // && appConfig.getBringYourOwnStorageUrl() != null
                // ? new
                // AzureBlobContainerRecordingStorage(appConfig.getBringYourOwnStorageUrl())
                // : null);
                // log.info("Pause On Start-->: " + recordingOptions.isPauseOnStart());
                // recordingId = client.getCallRecording().startWithResponse(recordingOptions,
                // Context.NONE)
                // .getValue().getRecordingId();
                // log.info("Call recording id--> " + recordingId);

                /** hold the call */
                // hold(callConnectionId);
                // log.info("Call On Hold successfully");
                // try {
                // TimeUnit.SECONDS.sleep(10);
                // } catch (Exception e) {
                // }

                /** unhold the call */
                // unhold(callConnectionId);
                // log.info("Call UnHolded successfully");

                /** Hangup the call */
                // hangUp(callConnectionId);

            } else if (event instanceof AddParticipantSucceeded) {
                log.info("Recieved add participant Succeeded event");
                try {
                    // Wait for a specific duration before resuming
                    Thread.sleep(5000); // Adjust the sleep duration as needed
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                    Thread.currentThread().interrupt();
                }

                /** hold the participant on a call */
                // hold(callConnectionId);
                // log.info("Call On Hold successfully");
                // try {
                // TimeUnit.SECONDS.sleep(10);
                // } catch (Exception e) {
                // }

                /** Get Participant of a call */
                // CallParticipant participantInfo = client.getCallConnection(callConnectionId)
                //         .getParticipant(new PhoneNumberIdentifier(newParticipantContact));
                // log.info("Newly added participant details ----->" + participantInfo);

                /** Remove a participant from a call */
                // log.info("Removing the Added participant from the call");
                // PhoneNumberIdentifier target = new PhoneNumberIdentifier(newParticipantContact);
                // RemoveParticipantOptions removeParticipantOptions = new RemoveParticipantOptions(target)
                //         .setOperationContext("removePstnUserContext");
                // callConnection.removeParticipant(target);

                /** unhold the participant on the call */
                // unhold(callConnectionId);
                // log.info("Call UnHolded successfully");

                /** Hangup the call */
                // hangUp(callConnectionId);

            } else if (event instanceof RemoveParticipantSucceeded) {
                log.info("Received RemoveParticipantSucceeded event");
            } else if (event instanceof RecognizeCompleted) {
                log.info("Received Recognize Completed event");
                RecognizeCompleted acsEvent = (RecognizeCompleted) event;
                var choiceResult = (ChoiceResult) acsEvent.getRecognizeResult().get();
                String labelDetected = choiceResult.getLabel();
                String phraseDetected = choiceResult.getRecognizedPhrase();
                log.info("Recognition completed, labelDetected=" + labelDetected + "phraseDetected=" + phraseDetected
                        + ", context=" + event.getOperationContext());
                String textToPlay = labelDetected.equals(confirmLabel) ? confirmedText : cancelText;
                handlePlay(callConnectionId, textToPlay);
            } else if (event instanceof RecognizeFailed) {
                log.error("Received Recognize failed event: {}",
                        ((CallAutomationEventBaseWithReasonCode) event)
                                .getResultInformation().getMessage());
                var recognizeFailedEvent = (RecognizeFailed) event;
                var context = recognizeFailedEvent.getOperationContext();
                if (context != null && context.equals(retryContext)) {
                    handlePlay(callConnectionId, noResponse);
                } else {
                    var resultInformation = recognizeFailedEvent.getResultInformation();
                    log.error("Encountered error during recognize, message={}, code={}, subCode={}",
                            resultInformation.getMessage(),
                            resultInformation.getCode(),
                            resultInformation.getSubCode());

                    var reasonCode = recognizeFailedEvent.getReasonCode();
                    String replyText = reasonCode == ReasonCode.Recognize.PLAY_PROMPT_FAILED ||
                            reasonCode == ReasonCode.Recognize.INITIAL_SILENCE_TIMEOUT ? customerQueryTimeout
                                    : reasonCode == ReasonCode.Recognize.INCORRECT_TONE_DETECTED ? invalidAudio
                                            : customerQueryTimeout;

                    // prepare recognize tones
                    startRecognizingWithChoiceOptions(callConnectionId, replyText,
                            appConfig.getTargetphonenumber(),
                            retryContext);
                }
            } else if (event instanceof PlayCompleted || event instanceof PlayFailed) {
                log.info("Received Play Completed event");
                CompletableFuture<String> stateFuture = getRecordingState(recordingId);
                String state = stateFuture.join();

                /** Pause the recording the If It is Active */
                if (state.equals("active")) {
                    client.getCallRecording().pauseWithResponse(recordingId,
                            Context.NONE);
                    log.info("Recording is Paused.");
                    getRecordingState(recordingId).join();

                    try {
                        // Wait for a specific duration before resuming
                        Thread.sleep(3000); // Adjust the sleep duration as needed
                    } catch (InterruptedException e) {
                        log.error(e.getMessage());
                        Thread.currentThread().interrupt();
                    }

                    // Resume the recording after the pause
                    client.getCallRecording().resumeWithResponse(recordingId,
                            Context.NONE);
                    log.info("Recording is Resumed.");
                    getRecordingState(recordingId).join();

                } else {
                    client.getCallRecording().resumeWithResponse(recordingId,
                            Context.NONE);
                    log.info("Recording is Resumed.");
                    getRecordingState(recordingId).join();
                }

                /** Stop the recording after a specific duration resuming */
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                    Thread.currentThread().interrupt();
                }
                client.getCallRecording().stopWithResponse(recordingId, Context.NONE);
                log.info("Recording is Stopped.");
            }
        }
        return ResponseEntity.ok().body("");
    }

    private String createOutboundCall() {
        try {
            PhoneNumberIdentifier caller = new PhoneNumberIdentifier(appConfig.getCallerphonenumber());
            PhoneNumberIdentifier target = new PhoneNumberIdentifier(appConfig.getTargetphonenumber());
            CallInvite callInvite = new CallInvite(target, caller);
            CreateCallOptions createCallOptions = new CreateCallOptions(callInvite, appConfig.getCallBackUri());
            CallIntelligenceOptions callIntelligenceOptions = new CallIntelligenceOptions()
                    .setCognitiveServicesEndpoint(appConfig.getCognitiveServiceEndpoint());
            createCallOptions = createCallOptions.setCallIntelligenceOptions(callIntelligenceOptions);
            Response<CreateCallResult> result = client.createCallWithResponse(createCallOptions, Context.NONE);
            return result.getValue().getCallConnectionProperties().getCallConnectionId();
        } catch (CommunicationErrorResponseException e) {
            log.error("Error when creating call: {} {}",
                    e.getMessage(),
                    e.getCause());
            return "";
        }
    }

    private CompletableFuture<String> getRecordingState(String recordingId) {
        RecordingStateResult result = this.client.getCallRecording().getState(recordingId);

        String state = result.getRecordingState().toString();
        log.info("Recording Status:->  " + state);
        return CompletableFuture.completedFuture(state);
    }

    private void handlePlay(final String callConnectionId, String textToPlay) {
        var textPlay = new TextSource()
                .setText(textToPlay)
                .setVoiceName("en-US-NancyNeural");

        client.getCallConnection(callConnectionId)
                .getCallMedia()
                .playToAll(textPlay);
    }

    private void startRecognizingWithChoiceOptions(final String callConnectionId, final String content,
            final String targetParticipant, final String context) {
        var playSource = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");

        var recognizeOptions = new CallMediaRecognizeChoiceOptions(new PhoneNumberIdentifier(targetParticipant),
                getChoices())
                .setInterruptCallMediaOperation(false)
                .setInterruptPrompt(false)
                .setInitialSilenceTimeout(Duration.ofSeconds(10))
                .setPlayPrompt(playSource)
                .setOperationContext(context);

        client.getCallConnection(callConnectionId)
                .getCallMedia()
                .startRecognizing(recognizeOptions);
    }

    private List<RecognitionChoice> getChoices() {
        var choices = Arrays.asList(
                new RecognitionChoice().setLabel(confirmLabel).setPhrases(Arrays.asList("Confirm", "First", "One"))
                        .setTone(DtmfTone.ONE),
                new RecognitionChoice().setLabel(cancelLabel).setPhrases(Arrays.asList("Cancel", "Second", "Two"))
                        .setTone(DtmfTone.TWO));
        return choices;
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

    private void hold(final String callConnectionId) {
        PhoneNumberIdentifier target = new PhoneNumberIdentifier(appConfig.getTargetphonenumber());
        HoldOptions holdOptions = new HoldOptions(target)
                // .setOperationCallbackUri(appConfig.getBasecallbackuri())
                .setOperationContext("holdPstnParticipant");
        var textPlay = new TextSource()
                .setText("Call is on hold, please wait for some time")
                .setVoiceName("en-US-NancyNeural");
        holdOptions.setPlaySource(textPlay);

        // with options
        client.getCallConnection(callConnectionId).getCallMedia().holdWithResponse(holdOptions, Context.NONE);

        // without options
        // client.getCallConnection(callConnectionId).getCallMedia().hold(target1, target2, textPlay);
    }

    private void holdAllParticipantsOnCall(final String callConnectionId) {
        //
    }
    private void unhold(final String callConnectionId) {
        PhoneNumberIdentifier target = new PhoneNumberIdentifier(appConfig.getTargetphonenumber());

        // with options
        // client.getCallConnection(callConnectionId).getCallMedia().unholdWithResponse(target, "unholdPstnParticipant",Context.NONE);
        
        // without options
        client.getCallConnection(callConnectionId).getCallMedia().unhold(target);
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
