package com.communication.callautomation;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.azure.communication.callautomation.CallAutomationAsyncClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.models.CallInvite;
import com.azure.communication.callautomation.models.CallMediaRecognizeChoiceOptions;
import com.azure.communication.callautomation.models.CallMediaRecognizeDtmfOptions;
import com.azure.communication.callautomation.models.CallMediaRecognizeSpeechOptions;
import com.azure.communication.callautomation.models.CallMediaRecognizeSpeechOrDtmfOptions;
import com.azure.communication.callautomation.models.ChoiceResult;
import com.azure.communication.callautomation.models.CreateCallOptions;
import com.azure.communication.callautomation.models.DtmfResult;
import com.azure.communication.callautomation.models.DtmfTone;
import com.azure.communication.callautomation.models.FileSource;
import com.azure.communication.callautomation.models.PlayOptions;
import com.azure.communication.callautomation.models.PlayToAllOptions;
import com.azure.communication.callautomation.models.RecognitionChoice;
import com.azure.communication.callautomation.models.RecognizeResult;
import com.azure.communication.callautomation.models.SpeechResult;
import com.azure.communication.callautomation.models.SsmlSource;
import com.azure.communication.callautomation.models.TextSource;
import com.azure.communication.callautomation.models.VoiceKind;
import com.azure.communication.callautomation.models.events.CallAutomationEventBase;
import com.azure.communication.callautomation.models.events.CallConnected;
import com.azure.communication.callautomation.models.events.PlayCanceled;
import com.azure.communication.callautomation.models.events.PlayCompleted;
import com.azure.communication.callautomation.models.events.PlayFailed;
import com.azure.communication.callautomation.models.events.ReasonCode;
import com.azure.communication.callautomation.models.events.RecognizeCanceled;
import com.azure.communication.callautomation.models.events.RecognizeCompleted;
import com.azure.communication.callautomation.models.events.RecognizeFailed;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class ProgramSample {
    private AppConfig appConfig;
    private CallAutomationAsyncClient callAutomationClient;
    private CommunicationIdentifier targetParticipant;
    private CallInvite callInvite;
    private String audioUri;
    private String operation = "RecognizeSpeechOrDtmf";

    public ProgramSample(final AppConfig appConfig) {
        this.appConfig = appConfig;
        if (appConfig.getUsephone()) {
            PhoneNumberIdentifier targetPhoneNumberIdentifier = new PhoneNumberIdentifier(appConfig.getTargetphonenumber());
            targetParticipant = targetPhoneNumberIdentifier;
            PhoneNumberIdentifier callerPhoneNumberIdentifier = new PhoneNumberIdentifier(appConfig.getCallerphonenumber());
            callInvite = new CallInvite(targetPhoneNumberIdentifier, callerPhoneNumberIdentifier);
        }
        else
        {
            CommunicationUserIdentifier targetCommunicationUserIdentifier = new CommunicationUserIdentifier(appConfig.getTargetuserid());
            targetParticipant = targetCommunicationUserIdentifier;
            callInvite = new CallInvite(targetCommunicationUserIdentifier);
        }
        audioUri = appConfig.getCallBackUri() + "/prompt.wav";
        callAutomationClient = new CallAutomationClientBuilder()
                .connectionString(appConfig.getConnectionString())
                .buildAsyncClient();
    }

    @GetMapping(path = "/outboundCall")
    public ResponseEntity<String> outboundCall() {
        var createCallOptions = new CreateCallOptions(callInvite, appConfig.getCallBackUri());
        createCallOptions.setCognitiveServicesEndpoint(appConfig.getCognitiveserviceendpoint());
        var createCallResult = callAutomationClient
            .createCallWithResponse(createCallOptions)
            .block();
        log.info("createCall, result: " + createCallResult.getStatusCode());

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/index.html")).build();
    }

    @PostMapping(path = "/api/callback")
    public ResponseEntity<String> callbackEvents(@RequestBody final String reqBody) {
        List<CallAutomationEventBase> events = CallAutomationEventParser.parseEvents(reqBody);
        for (CallAutomationEventBase acsEvent : events) {
            String callConnectionId = acsEvent.getCallConnectionId();
            log.info("Received event {} for call connection id {}", acsEvent.getClass().getName(), callConnectionId);
            
            if (acsEvent instanceof CallConnected) {
                if (operation == "PlayFile") {
                    var playSource = new FileSource().setUrl(audioUri);
                    var playTo = Arrays.asList(targetParticipant);
                    var playOptions = new PlayOptions(playSource, playTo);
                    var playResponse = callAutomationClient.getCallConnectionAsync(callConnectionId)
                            .getCallMediaAsync()
                            .playWithResponse(playOptions)
                            .block();
                    log.info("Play result: " + playResponse.getStatusCode());
                } else if (operation == "PlayTextWithKind") {
                    String textToPlay = "Welcome to Contoso";

                    // Provide SourceLocale and VoiceKind to select an appropriate voice.
                    // SourceLocale or VoiceName needs to be provided.
                    var playSource = new TextSource()
                            .setText(textToPlay)
                            .setSourceLocale("en-US")
                            .setVoiceKind(VoiceKind.FEMALE);
                    var playTo = Arrays.asList(targetParticipant);
                    var playOptions = new PlayOptions(playSource, playTo);
                    var playResponse = callAutomationClient.getCallConnectionAsync(callConnectionId)
                            .getCallMediaAsync()
                            .playWithResponse(playOptions)
                            .block();
                    log.info("Play result: " + playResponse.getStatusCode());
                } else if (operation == "PlayTextWithVoice") {
                    String textToPlay = "Welcome to Contoso";

                    // Provide VoiceName to select a specific voice. SourceLocale or VoiceName needs
                    // to be provided.
                    var playSource = new TextSource()
                            .setText(textToPlay)
                            .setVoiceName("en-US-ElizabethNeural");
                    var playTo = Arrays.asList(targetParticipant);
                    var playOptions = new PlayOptions(playSource, playTo);
                    var playResponse = callAutomationClient.getCallConnectionAsync(callConnectionId)
                            .getCallMediaAsync()
                            .playWithResponse(playOptions)
                            .block();
                    log.info("Play result: " + playResponse.getStatusCode());
                } else if (operation == "PlaySSML") {
                    String ssmlToPlay = "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xml:lang=\"en-US\"><voice name=\"en-US-JennyNeural\">Hello World!</voice></speak>";
                    var playSource = new SsmlSource()
                            .setSsmlText(ssmlToPlay);
                    var playTo = Arrays.asList(targetParticipant);
                    var playOptions = new PlayOptions(playSource, playTo);
                    var playResponse = callAutomationClient.getCallConnectionAsync(callConnectionId)
                            .getCallMediaAsync()
                            .playWithResponse(playOptions)
                            .block();
                    log.info("Play result: " + playResponse.getStatusCode());
                } else if (operation == "PlayToAllAsync") {
                    String textToPlay = "Welcome to Contoso";
                    var playSource = new TextSource()
                            .setText(textToPlay)
                            .setVoiceName("en-US-ElizabethNeural");
                    var playOptions = new PlayToAllOptions(playSource);
                    var playResponse = callAutomationClient.getCallConnectionAsync(callConnectionId)
                            .getCallMediaAsync()
                            .playToAllWithResponse(playOptions)
                            .block();
                    log.info("Play result: " + playResponse.getStatusCode());
                } else if (operation == "PlayLoop") {
                    String textToPlay = "Welcome to Contoso";
                    var playSource = new TextSource()
                            .setText(textToPlay)
                            .setVoiceName("en-US-ElizabethNeural");
                    var playOptions = new PlayToAllOptions(playSource)
                            .setLoop(true);
                    var playResponse = callAutomationClient.getCallConnectionAsync(callConnectionId)
                            .getCallMediaAsync()
                            .playToAllWithResponse(playOptions)
                            .block();
                    log.info("Play result: " + playResponse.getStatusCode());
                } else if (operation == "PlayWithCache") {
                    var playTo = Arrays.asList(targetParticipant);
                    var playSource = new FileSource()
                            .setUrl(audioUri)
                            .setPlaySourceCacheId("<playSourceId>");
                    var playOptions = new PlayOptions(playSource, playTo);
                    var playResponse = callAutomationClient.getCallConnectionAsync(callConnectionId)
                            .getCallMediaAsync()
                            .playWithResponse(playOptions)
                            .block();
                    log.info("Play result: " + playResponse.getStatusCode());
                } else if (operation == "CancelMedia") {
                    var cancelResponse = callAutomationClient.getCallConnectionAsync(callConnectionId)
                            .getCallMediaAsync()
                            .cancelAllMediaOperationsWithResponse()
                            .block();
                    log.info("Cancel result: " + cancelResponse.getStatusCode());
                } else if (operation == "RecognizeDTMF") {
                    var maxTonesToCollect = 3;
                    String textToPlay = "Welcome to Contoso, please enter 3 DTMF.";
                    var playSource = new TextSource()
                            .setText(textToPlay)
                            .setVoiceName("en-US-ElizabethNeural");
                    var recognizeOptions = new CallMediaRecognizeDtmfOptions(targetParticipant, maxTonesToCollect)
                            .setInitialSilenceTimeout(Duration.ofSeconds(30))
                            .setPlayPrompt(playSource)
                            .setInterToneTimeout(Duration.ofSeconds(5))
                            .setInterruptPrompt(true)
                            .setStopTones(Arrays.asList(DtmfTone.POUND));
                    var recognizeResponse = callAutomationClient.getCallConnectionAsync(callConnectionId)
                            .getCallMediaAsync()
                            .startRecognizingWithResponse(recognizeOptions)
                            .block();
                    log.info("Start recognizing result: " + recognizeResponse.getStatusCode());
                } else if (operation == "RecognizeChoice") {
                    var choices = Arrays.asList(
                            new RecognitionChoice()
                                    .setLabel("Confirm")
                                    .setPhrases(Arrays.asList("Confirm", "First", "One"))
                                    .setTone(DtmfTone.ONE),
                            new RecognitionChoice()
                                    .setLabel("Cancel")
                                    .setPhrases(Arrays.asList("Cancel", "Second", "Two"))
                                    .setTone(DtmfTone.TWO));
                    String textToPlay = "Hello, This is a reminder for your appointment at 2 PM, Say Confirm to confirm your appointment or Cancel to cancel the appointment. Thank you!";
                    var playSource = new TextSource()
                            .setText(textToPlay)
                            .setVoiceName("en-US-ElizabethNeural");
                    var recognizeOptions = new CallMediaRecognizeChoiceOptions(targetParticipant, choices)
                            .setInterruptPrompt(true)
                            .setInitialSilenceTimeout(Duration.ofSeconds(30))
                            .setPlayPrompt(playSource)
                            .setOperationContext("AppointmentReminderMenu");
                    var recognizeResponse = callAutomationClient.getCallConnectionAsync(callConnectionId)
                            .getCallMediaAsync()
                            .startRecognizingWithResponse(recognizeOptions)
                            .block();
                    log.info("Start recognizing result: " + recognizeResponse.getStatusCode());
                } else if (operation == "RecognizeSpeech") {
                    String textToPlay = "Hi, how can I help you today?";
                    var playSource = new TextSource()
                            .setText(textToPlay)
                            .setVoiceName("en-US-ElizabethNeural");
                    var recognizeOptions = new CallMediaRecognizeSpeechOptions(targetParticipant,
                            Duration.ofMillis(1000))
                            .setPlayPrompt(playSource)
                            .setOperationContext("OpenQuestionSpeech");
                    var recognizeResponse = callAutomationClient.getCallConnectionAsync(callConnectionId)
                            .getCallMediaAsync()
                            .startRecognizingWithResponse(recognizeOptions)
                            .block();
                    log.info("Start recognizing result: " + recognizeResponse.getStatusCode());
                } else if (operation == "RecognizeSpeechOrDtmf") {
                    var maxTonesToCollect = 1;
                    String textToPlay = "Hi, how can I help you today, you can press 0 to speak to an agent?";
                    var playSource = new TextSource()
                            .setText(textToPlay)
                            .setVoiceName("en-US-ElizabethNeural");
                    var recognizeOptions = new CallMediaRecognizeSpeechOrDtmfOptions(targetParticipant,
                            maxTonesToCollect, Duration.ofMillis(1000))
                            .setPlayPrompt(playSource)
                            .setInitialSilenceTimeout(Duration.ofSeconds(30))
                            .setInterruptPrompt(true)
                            .setOperationContext("OpenQuestionSpeechOrDtmf");
                    var recognizeResponse = callAutomationClient.getCallConnectionAsync(callConnectionId)
                            .getCallMediaAsync()
                            .startRecognizingWithResponse(recognizeOptions)
                            .block();
                    log.info("Start recognizing result: " + recognizeResponse.getStatusCode());
                }

            }
            if (acsEvent instanceof PlayCompleted) {
                PlayCompleted event = (PlayCompleted) acsEvent;
                log.info("Play completed, context=" + event.getOperationContext());
            }
            if (acsEvent instanceof PlayFailed) {
                PlayFailed event = (PlayFailed) acsEvent;
                if (ReasonCode.Play.DOWNLOAD_FAILED.equals(event.getReasonCode())) {
                    log.info("Play failed: download failed, context=" + event.getOperationContext());
                } else if (ReasonCode.Play.INVALID_FILE_FORMAT.equals(event.getReasonCode())) {
                    log.info("Play failed: invalid file format, context=" + event.getOperationContext());
                } else {
                    log.info("Play failed, result=" + event.getResultInformation().getMessage() + ", context="
                            + event.getOperationContext());
                }
            }
            if (acsEvent instanceof PlayCanceled) {
                PlayCanceled event = (PlayCanceled) acsEvent;
                log.info("Play canceled, context=" + event.getOperationContext());
            }
            if (acsEvent instanceof RecognizeCompleted) {
                RecognizeCompleted event = (RecognizeCompleted) acsEvent;
                RecognizeResult recognizeResult = event.getRecognizeResult().get();
                if (recognizeResult instanceof DtmfResult) {
                    // Take action on collect tones
                    DtmfResult dtmfResult = (DtmfResult) recognizeResult;
                    List<DtmfTone> tones = dtmfResult.getTones();
                    log.info("Recognition completed, tones=" + tones + ", context=" + event.getOperationContext());
                } else if (recognizeResult instanceof ChoiceResult) {
                    ChoiceResult collectChoiceResult = (ChoiceResult) recognizeResult;
                    String labelDetected = collectChoiceResult.getLabel();
                    String phraseDetected = collectChoiceResult.getRecognizedPhrase();
                    log.info("Recognition completed, labelDetected=" + labelDetected + ", phraseDetected="
                            + phraseDetected + ", context=" + event.getOperationContext());
                } else if (recognizeResult instanceof SpeechResult) {
                    SpeechResult speechResult = (SpeechResult) recognizeResult;
                    String text = speechResult.getSpeech();
                    log.info("Recognition completed, text=" + text + ", context=" + event.getOperationContext());
                } else {
                    log.info("Recognition completed, result=" + recognizeResult + ", context="
                            + event.getOperationContext());
                }
            }
            if (acsEvent instanceof RecognizeFailed) {
                RecognizeFailed event = (RecognizeFailed) acsEvent;
                if (ReasonCode.Recognize.INITIAL_SILENCE_TIMEOUT.equals(event.getReasonCode())) {
                    // Take action for time out
                    log.info("Recognition failed: initial silence time out");
                } else if (ReasonCode.Recognize.SPEECH_OPTION_NOT_MATCHED.equals(event.getReasonCode())) {
                    // Take action for option not matched
                    log.info("Recognition failed: speech option not matched");
                } else if (ReasonCode.Recognize.DMTF_OPTION_MATCHED.equals(event.getReasonCode())) {
                    // Take action for incorrect tone
                    log.info("Recognition failed: incorrect tone detected");
                } else {
                    log.info("Recognition failed, result=" + event.getResultInformation().getMessage() + ", context="
                            + event.getOperationContext());
                }
            }
            if (acsEvent instanceof RecognizeCanceled) {
                RecognizeCanceled event = (RecognizeCanceled) acsEvent;
                log.info("Recognition canceled, context=" + event.getOperationContext());
            }
        }
        return ResponseEntity.ok().body("");
    }
}
