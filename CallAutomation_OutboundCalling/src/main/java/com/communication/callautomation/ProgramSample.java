package com.communication.callautomation;

import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.CallConnection;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.communication.common.CommunicationIdentifier;
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
import java.util.ArrayList;
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
    private String textToPlay = "This audio is testing the feature of playing audio using text-to-speech. Please let us know if you can hear this message clearly.";
    public String newParticipantContact = "+918074859690";

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
                // callConnection = client.getCallConnection(callConnectionId);
                // CallInvite callInvite = new CallInvite(
                // new PhoneNumberIdentifier(newParticipantContact),
                // new PhoneNumberIdentifier(appConfig.getCallerphonenumber()));
                // val addParticipantOptions = new AddParticipantOptions(callInvite)
                // .setOperationContext("addPstnUserContext");
                // Response<AddParticipantResult> addParticipantResult =
                // callConnection.addParticipantWithResponse(
                // addParticipantOptions,
                // Context.NONE);
                // log.info("Adding PstnUser to the call: {}",
                // addParticipantResult.getValue().getInvitationId());

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

                /** Handle Play Media */
                // handlePlay(callConnectionId, textToPlay);
                // handlePlayAudio(callConnectionId);
                // handlePlayInterrupt(callConnectionId, textToPlay);

                /** #region Recognize Prompt List: Different recognizing formats */
                // Prepare recognize tones Choice */
                startRecognizingWithChoiceOptions(callConnectionId, MainMenu,
                appConfig.getTargetphonenumber(), "mainmenu");

                // // prepare recognize tones DTMF
                // getMediaRecognizeDTMFOptions(callConnectionId, MainMenu,
                // appConfig.getTargetphonenumber(), "mainmenu");

                // prepare recognize Speech
                // getMediaRecognizeSpeechOptions(callConnectionId, MainMenu,
                // appConfig.getTargetphonenumber(), "mainmenu");

                // // prepare recognize Speech or dtmf
                // getMediaRecognizeSpeechOrDtmfOptions(callConnectionId, MainMenu,
                // appConfig.getTargetphonenumber(), "mainmenu");
                // #endregion

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
                // .getParticipant(new PhoneNumberIdentifier(newParticipantContact));
                // log.info("Newly added participant details ----->" + participantInfo);

                /** Remove a participant from a call */
                // log.info("Removing the Added participant from the call");
                // PhoneNumberIdentifier target = new
                // PhoneNumberIdentifier(newParticipantContact);
                // RemoveParticipantOptions removeParticipantOptions = new
                // RemoveParticipantOptions(target)
                // .setOperationContext("removePstnUserContext");
                // callConnection.removeParticipant(target);

                /** unhold the participant on the call */
                // unhold(callConnectionId);
                // log.info("Call UnHolded successfully");

                /** Hangup the call */
                // hangUp(callConnectionId);

            } else if (event instanceof RemoveParticipantSucceeded) {
                log.info("Received RemoveParticipantSucceeded event");
            } else if (event instanceof RecognizeCompleted) {
                log.info("Received Recognize Completed event, terminating call");
                hangUp(callConnectionId);
                // RecognizeCompleted acsEvent = (RecognizeCompleted) event;
                // var choiceResult = (ChoiceResult) acsEvent.getRecognizeResult().get();
                // String labelDetected = choiceResult.getLabel();
                // String phraseDetected = choiceResult.getRecognizedPhrase();
                // log.info("Recognition completed, labelDetected=" + labelDetected +
                //         "phraseDetected=" + phraseDetected
                //         + ", context=" + event.getOperationContext());
                // String textToPlay = labelDetected.equals(confirmLabel) ? confirmedText : cancelText;
                // handlePlay(callConnectionId, textToPlay);
            } else if (event instanceof RecognizeFailed) {
                log.error("Received Recognize failed event: {}",
                        ((CallAutomationEventBaseWithReasonCode) event)
                                .getResultInformation().getMessage());
                // var recognizeFailedEvent = (RecognizeFailed) event;
                // var context = recognizeFailedEvent.getOperationContext();
                // if (context != null && context.equals(retryContext)) {
                // handlePlay(callConnectionId, noResponse);
                // } else {
                // var resultInformation = recognizeFailedEvent.getResultInformation();
                // log.error("Encountered error during recognize, message={}, code={},
                // subCode={}",
                // resultInformation.getMessage(),
                // resultInformation.getCode(),
                // resultInformation.getSubCode());

                // var reasonCode = recognizeFailedEvent.getReasonCode();
                // String replyText = reasonCode == ReasonCode.Recognize.PLAY_PROMPT_FAILED ||
                // reasonCode == ReasonCode.Recognize.INITIAL_SILENCE_TIMEOUT ?
                // customerQueryTimeout
                // : reasonCode == ReasonCode.Recognize.INCORRECT_TONE_DETECTED ? invalidAudio
                // : customerQueryTimeout;

                // // prepare recognize tones
                // startRecognizingWithChoiceOptions(callConnectionId, replyText,
                // appConfig.getTargetphonenumber(),
                // retryContext);
                // }
            } else if (event instanceof RecognizeCanceled) {
                log.info("Received Recognition canceled event");
            } else if (event instanceof PlayStarted) {
                log.info("Received PlayStarted event.");
            } else if (event instanceof PlayCompleted) {
                log.info("Received Play Completed even");
                // hangUp(callConnectionId);
                /** Pause the recording the If It is Active */
                // CompletableFuture<String> stateFuture = getRecordingState(recordingId);
                // String state = stateFuture.join();
                // if (state.equals("active")) {
                // client.getCallRecording().pauseWithResponse(recordingId,
                // Context.NONE);
                // log.info("Recording is Paused.");
                // getRecordingState(recordingId).join();
                // try {
                // // Wait for a specific duration before resuming
                // Thread.sleep(3000); // Adjust the sleep duration as needed
                // } catch (InterruptedException e) {
                // log.error(e.getMessage());
                // Thread.currentThread().interrupt();
                // }
                // // Resume the recording after the pause
                // client.getCallRecording().resumeWithResponse(recordingId,
                // Context.NONE);
                // log.info("Recording is Resumed.");
                // getRecordingState(recordingId).join();
                // } else {
                // client.getCallRecording().resumeWithResponse(recordingId,
                // Context.NONE);
                // log.info("Recording is Resumed.");
                // getRecordingState(recordingId).join();
                // }

                /** Stop the recording after a specific duration resuming */
                // try {
                // Thread.sleep(5000);
                // } catch (InterruptedException e) {
                // log.error(e.getMessage());
                // Thread.currentThread().interrupt();
                // }
                // client.getCallRecording().stopWithResponse(recordingId, Context.NONE);
                // log.info("Recording is Stopped.");
            } else if (event instanceof PlayCanceled) {
                log.info("Received Play Cancelled event");
            } else if (event instanceof PlayFailed) {
                log.info("Received Play Failed event , Terminating the call");
                // hangUp(callConnectionId);
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
        List<CommunicationIdentifier> playToList = Arrays.asList(
                new PhoneNumberIdentifier(appConfig.getTargetphonenumber()));

        /** Play source - Audio file */
        // var p1 = new FileSource()
        // .setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/StarWars3.wav");

        /** Play source - Text-To-Speech */
        var p2 = new TextSource()
                .setText(textToPlay)
                .setVoiceName("en-US-NancyNeural");

        /** Play source - Text-to-Speech SSML */
        // String ssmlToPlay = "<speak version=\"1.0\"
        // xmlns=\"http://www.w3.org/2001/10/synthesis\" xml:lang=\"en-US\"><voice
        // name=\"en-US-JennyNeural\">Hello World!</voice></speak>";
        // var p3 = new SsmlSource()
        // .setSsmlText(ssmlToPlay);

        // var playSources = new ArrayList();
        // playSources.add(p1);
        // playSources.add(p2);
        // playSources.add(p3);

        /** Play to all without options */
        client.getCallConnection(callConnectionId)
                .getCallMedia()
                .playToAll(p2);

        /** Play to target participant without options */
        // client.getCallConnection(callConnectionId)
        // .getCallMedia()
        // .play(playSources, playToList);

        /** Play to all with options */
        // var playToAllOptions = new PlayToAllOptions(playSources)
        // .setLoop(false)
        // .setOperationCallbackUrl(appConfig.getBasecallbackuri())
        // .setInterruptCallMediaOperation(false);
        // client.getCallConnection(callConnectionId)
        // .getCallMedia()
        // .playToAllWithResponse(playToAllOptions, Context.NONE);

        /** Play to target Participant with Option */
        // var playOptions = new PlayOptions(playSources, playToList);
        // client.getCallConnection(callConnectionId)
        // .getCallMedia()
        // .playWithResponse(playOptions, Context.NONE);

        /* Multiple TextSource and FileSource and SsmlSource prompt */
        // var p1 = new TextSource().setText("recognize prompt one, hello welcome to
        // contoso solutions. your appointment has been confirmed. thank
        // you").setVoiceName("en-US-NancyNeural");
        // var p2 = new
        // FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/preamble10.wav");
        // var p3 = new SsmlSource().setSsmlText("<speak version=\"1.0\"
        // xmlns=\"http://www.w3.org/2001/10/synthesis\" xml:lang=\"en-US\"><voice
        // name=\"en-US-JennyNeural\">Hello this is SSML play prompt play from form
        // multiple prompts, Played through first SSML handle play</voice></speak>");
        // var p4 = new TextSource().setText("recognize prompt
        // four").setVoiceName("en-US-NancyNeural");
        // var p5 = new TextSource().setText("recognize prompt
        // five").setVoiceName("en-US-NancyNeural");
        // var p6 = new TextSource().setText("recognize prompt
        // six").setVoiceName("en-US-NancyNeural");
        // var p7 = new TextSource().setText("recognize prompt
        // seven").setVoiceName("en-US-NancyNeural");
        // var p8 = new TextSource().setText("recognize prompt
        // eight").setVoiceName("en-US-NancyNeural");
        // var p9 = new TextSource().setText("recognize prompt
        // nine").setVoiceName("en-US-NancyNeural");
        // var p10 = new TextSource().setText("recognize prompt
        // ten").setVoiceName("en-US-NancyNeural");
        // var playSources = new ArrayList<PlaySource>();
        // playSources.add(p1);
        // playSources.add(p2);
        // playSources.add(p3);
        // playSources.add(p4);
        // playSources.add(p5);
        // playSources.add(p6);
        // playSources.add(p7);
        // playSources.add(p8);
        // playSources.add(p9);
        // playSources.add(p10);
        // client.getCallConnection(callConnectionId)
        // .getCallMedia()
        // .playToAll(playSources);

    }

    private void handlePlayInterrupt(final String callConnectionId, String textToPlay) {
        var PlaySource = new TextSource()
                .setText(textToPlay)
                .setVoiceName("en-US-NancyNeural");

        /** Play to all with options */
        var playToAllOptions = new PlayToAllOptions(PlaySource)
                .setLoop(false)
                .setOperationCallbackUrl(appConfig.getCallBackUri())
                .setInterruptCallMediaOperation(true);

        client.getCallConnection(callConnectionId)
                .getCallMedia()
                .playToAllWithResponse(playToAllOptions, Context.NONE);

        // Inturrput text Prompt 1
        var textPrompt1 = new TextSource()
                .setText("First Interrupt prompt message from text source one")
                .setVoiceName("en-US-NancyNeural");

        var playToAllOptions1 = new PlayToAllOptions(textPrompt1)
                .setLoop(false)
                .setOperationCallbackUrl(appConfig.getCallBackUri())
                .setInterruptCallMediaOperation(false);

        client.getCallConnection(callConnectionId)
                .getCallMedia()
                .playToAllWithResponse(playToAllOptions1, Context.NONE);
    }

    private void handlePlayAudio(final String callConnectionId) {
        List<CommunicationIdentifier> playToList = Arrays.asList(
                new PhoneNumberIdentifier(appConfig.getTargetphonenumber()));
        /** Play source - Audio file */
        // var playSource = new
        // FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/StarWars3.wav");

        /** Multiple FileSource Prompts */
        var p1 = new FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/StarWars3.wav");
        var p2 = new FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/preamble10.wav");

        var playSources = new ArrayList();
        playSources.add(p1);
        playSources.add(p2);

        /** Play Audio to All without options */
        // client.getCallConnection(callConnectionId)
        // .getCallMedia()
        // .playToAll(playSources);

        /** Play to all with options */
        var playToAllOptions = new PlayToAllOptions(playSources)
                .setLoop(false)
                .setOperationCallbackUrl(appConfig.getBasecallbackuri())
                .setInterruptCallMediaOperation(false);
        client.getCallConnection(callConnectionId)
                .getCallMedia()
                .playToAllWithResponse(playToAllOptions, Context.NONE);

        /** Play to target Participant with options */
        // var playOptions = new PlayOptions(playSources, playToList);
        // client.getCallConnection(callConnectionId)
        // .getCallMedia()
        // .playWithResponse(playOptions, Context.NONE);

        /** Play to target participant without options */
        // client.getCallConnection(callConnectionId)
        // .getCallMedia()
        // .play(playSources, playToList);
    }

    private void startRecognizingWithChoiceOptions(final String callConnectionId, final String content,
            final String targetParticipant, final String context) {
        // var playSource = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");

        // Multiple TextSource and FileSource and SsmlSource prompt
        var p1 = new FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/preamble10.wav");
        var p2 = new SsmlSource().setSsmlText("<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xml:lang=\"en-US\"><voice name=\"en-US-JennyNeural\">Hello this is SSML play prompt play from form multiple prompts, Played through first SSML handle play</voice></speak>");
        var p3 = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");
        var p4 = new TextSource().setText("recognize prompt four").setVoiceName("en-US-NancyNeural");
        var p5 = new TextSource().setText("recognize prompt five").setVoiceName("en-US-NancyNeural");
        var p6 = new TextSource().setText("recognize prompt six").setVoiceName("en-US-NancyNeural");
        var p7 = new TextSource().setText("recognize prompt seven").setVoiceName("en-US-NancyNeural");
        var p8 = new TextSource().setText("recognize prompt eight").setVoiceName("en-US-NancyNeural");
        var p9 = new TextSource().setText("recognize prompt nine").setVoiceName("en-US-NancyNeural");
        var p10 = new TextSource().setText("recognize prompt ten").setVoiceName("en-US-NancyNeural");

        var playSources = new ArrayList<PlaySource>();
        playSources.add(p1);
        playSources.add(p2);
        playSources.add(p3);
        playSources.add(p4);
        playSources.add(p5);
        playSources.add(p6);
        playSources.add(p7);
        playSources.add(p8);
        playSources.add(p9);
        playSources.add(p10);

        var recognizeOptions = new CallMediaRecognizeChoiceOptions(new PhoneNumberIdentifier(targetParticipant),
                getChoices())
                .setInterruptCallMediaOperation(false)
                .setInterruptPrompt(false)
                .setInitialSilenceTimeout(Duration.ofSeconds(10))
                // .setPlayPrompt(playSource)
                .setPlayPrompts(playSources)
                .setOperationContext(context)
                .setOperationCallbackUrl(appConfig.getCallBackUri());

        client.getCallConnection(callConnectionId)
                .getCallMedia()
                .startRecognizing(recognizeOptions);
    }

    private void getMediaRecognizeDTMFOptions(final String callConnectionId, final String content,
            final String targetParticipant, final String context) {
        var playSource = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");
        
        // Multiple TextSource and FileSource and SsmlSource prompt
        var p1 = new TextSource().setText("recognize prompt one").setVoiceName("en-US-NancyNeural");
        var p2 = new FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/preamble10.wav");
        var p3 = new SsmlSource().setSsmlText(
                "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xml:lang=\"en-US\"><voice name=\"en-US-JennyNeural\">Hello this is SSML play prompt play from form multiple prompts, Played through first SSML recognize, please say confirm</voice></speak>");
        var p4 = new TextSource().setText("recognize prompt four text").setVoiceName("en-US-NancyNeural");
        var playSources = new ArrayList<PlaySource>();
        playSources.add(p1);
        playSources.add(p2);
        playSources.add(p3);
        playSources.add(p4);

        var recognizeOptions = new CallMediaRecognizeDtmfOptions(new PhoneNumberIdentifier(targetParticipant), 8)
                .setInterruptCallMediaOperation(false)
                .setInterruptPrompt(false)
                .setInterToneTimeout(Duration.ofSeconds(5))
                .setInitialSilenceTimeout(Duration.ofSeconds(15))
                // .setPlayPrompt(playSource)
                .setPlayPrompts(playSources)
                .setOperationContext(context)
                .setOperationCallbackUrl(appConfig.getCallBackUri());

        client.getCallConnection(callConnectionId)
                .getCallMedia()
                .startRecognizing(recognizeOptions);
    }

    private void getMediaRecognizeSpeechOptions(final String callConnectionId, final String content,
            final String targetParticipant, final String context) {

        var playSource = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");

        // Multiple TextSource and FileSource and SsmlSource prompt
        var p1 = new TextSource().setText("recognize prompt one").setVoiceName("en-US-NancyNeural");
        var p2 = new FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/preamble10.wav");
        var p3 = new SsmlSource().setSsmlText("<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xml:lang=\"en-US\"><voice name=\"en-US-JennyNeural\">Hello this is SSML play prompt play from form multiple prompts, Played through first SSML recognize, please say confirm</voice></speak>");
        var p4 = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");
        var playSources = new ArrayList<PlaySource>();
        playSources.add(p1);
        playSources.add(p2);
        playSources.add(p3);
        playSources.add(p4);
        
        var recognizeOptions = new CallMediaRecognizeSpeechOptions(new PhoneNumberIdentifier(targetParticipant), Duration.ofSeconds(15))
                .setInterruptPrompt(false)
                .setInitialSilenceTimeout(Duration.ofSeconds(15))
                .setPlayPrompt(playSource)
                .setPlayPrompts(playSources)
                .setOperationContext(context)
                .setOperationCallbackUrl(appConfig.getCallBackUri());

        client.getCallConnection(callConnectionId)
                .getCallMedia()
                .startRecognizing(recognizeOptions);
    }

    private void getMediaRecognizeSpeechOrDtmfOptions(final String callConnectionId, final String content,
            final String targetParticipant, final String context) {
        
        var playSource = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");

        // Multiple TextSource and FileSource and SsmlSource prompt
        var p1 = new TextSource().setText("recognize prompt one").setVoiceName("en-US-NancyNeural");
        var p2 = new FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/preamble10.wav");
        var p3 = new SsmlSource().setSsmlText(
                "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xml:lang=\"en-US\"><voice name=\"en-US-JennyNeural\">Hello this is SSML play prompt play from form multiple prompts, Played through first SSML recognize, please say confirm</voice></speak>");
        var p4 = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");
        var playSources = new ArrayList<PlaySource>();
        playSources.add(p1);
        playSources.add(p2);
        playSources.add(p3);
        playSources.add(p4);

        var recognizeOptions = new CallMediaRecognizeSpeechOrDtmfOptions(new PhoneNumberIdentifier(targetParticipant),
                8, Duration.ofSeconds(15))
                .setInterruptPrompt(false)
                .setInitialSilenceTimeout(Duration.ofSeconds(10))
                .setPlayPrompt(playSource)
                .setPlayPrompts(playSources)
                .setOperationContext(context)
                .setOperationCallbackUrl(appConfig.getCallBackUri());

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
        // client.getCallConnection(callConnectionId).getCallMedia().hold(target1,
        // target2, textPlay);
    }

    private void holdAllParticipantsOnCall(final String callConnectionId) {
        //
    }

    private void unhold(final String callConnectionId) {
        PhoneNumberIdentifier target = new PhoneNumberIdentifier(appConfig.getTargetphonenumber());

        // with options
        // client.getCallConnection(callConnectionId).getCallMedia().unholdWithResponse(target,
        // "unholdPstnParticipant",Context.NONE);

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
