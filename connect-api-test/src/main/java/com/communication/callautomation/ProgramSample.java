package com.communication.callautomation;

import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.CallConnection;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.common.MicrosoftTeamsUserIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.communication.identity.implementation.models.CommunicationErrorResponseException;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@RestController
@Slf4j
public class ProgramSample {
    private AppConfig appConfig;
    private CallAutomationClient client;

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

    public ProgramSample(final AppConfig appConfig) {
        this.appConfig = appConfig;
        client = initClient();
    }

    // @GetMapping(path = "/outboundCall")
    // public ResponseEntity<String> outboundCall() {
    // String callConnectionId = createOutboundCall();
    // return ResponseEntity.ok().body("Target participant: "
    // + appConfig.getTargetphonenumber() +
    // ", CallConnectionId: " + callConnectionId);
    // }
    public String connectCall() {
        // CallLocator callLocator = new RoomCallLocator("99428898261234120");
        CallLocator callLocator = new GroupCallLocator("d0578077-eb16-4c23-9c61-04bef440f1b6");
        // CallLocator callLocator = new ServerCallLocator(
        // "aHR0cHM6Ly9hcGkuZmxpZ2h0cHJveHkuc2t5cGUuY29tL2FwaS92Mi9jcC9jb252LW1hc28tMDItcHJvZC1ha3MuY29udi5za3lwZS5jb20vY29udi85cm1hU0ItZGYwS25SNHh2Umt0Rk5BP2k9MTAtMTI4LTE5MS0xNTkmZT02Mzg1MzczMjE0NDE1MTc1MzE");
        ConnectCallOptions options = new ConnectCallOptions(callLocator, appConfig.getCallBackUri());
        Response<ConnectCallResult> result = client.connectCallWithResponse(options, Context.NONE);
        return result.getValue().getCallConnectionProperties().getCallConnectionId();
    }

    @GetMapping(path = "/connectCall")
    public ResponseEntity<String> createConnectCall() {
        String callConnectionId = connectCall();
        return ResponseEntity.ok().body("CallConnectionId: " + callConnectionId);
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
                // (Optional) Add a Microsoft Teams user to the call. Uncomment the below
                // snippet to enable Teams Interop scenario.
                // client.getCallConnection(callConnectionId).addParticipant(
                // new CallInvite(new
                // MicrosoftTeamsUserIdentifier(appConfig.getTargetTeamsUserId()))
                // .setSourceDisplayName("Jack (Contoso Tech Support)"));

                // prepare recognize tones
                // startRecognizingWithChoiceOptions(callConnectionId, MainMenu,
                // appConfig.getTargetphonenumber(),
                // "mainmenu");

                Response<CallConnectionProperties> properties = client.getCallConnection(callConnectionId)
                        .getCallPropertiesWithResponse(Context.NONE);
                String correlationId = properties.getValue().getCorrelationId();
                String connectCallConnectionId = properties.getValue().getCallConnectionId();
                log.info("*********CORRELATION ID*********:--> {}", correlationId);
                log.info("CALL CONNECTOION ID:--> {}", connectCallConnectionId);

                CallConnection connectCallConnection = client.getCallConnection(connectCallConnectionId);

                PhoneNumberIdentifier caller = new PhoneNumberIdentifier(appConfig.getCallerphonenumber());
                PhoneNumberIdentifier target = new PhoneNumberIdentifier(appConfig.getTargetphonenumber());
                CallInvite callInvite = new CallInvite(target, caller);

                // CommunicationUserIdentifier target = new CommunicationUserIdentifier(
                // "8:acs:19ae37ff-1a44-4e19-aade-198eedddbdf2_00000020-b66f-52a5-2c8a-08482200cc97");
                // CallInvite callInvite = new CallInvite(target);
                AddParticipantOptions participantOptions = new AddParticipantOptions(callInvite)
                        .setOperationContext("pstnUserContext");
                // .setOperationContext("communcationUserContext");
                connectCallConnection.addParticipantWithResponse(participantOptions, Context.NONE);

            } else if (event instanceof AddParticipantSucceeded) {
                log.info("Add participant succeeded event received.");
                var addParticipantSucceededEvent = (AddParticipantSucceeded) event;
                log.info("OPERATION CONTEXT" + addParticipantSucceededEvent.getOperationContext());
                PagedIterable<CallParticipant> response = client
                        .getCallConnection(addParticipantSucceededEvent.getCallConnectionId())
                        .listParticipants();
                log.info("*************************************");
                int count = 0;
                for (CallParticipant participant : response) {
                    count++;
                    System.out.println(participant.getIdentifier().getRawId());
                }
                log.info("TOTAL PARTICIPANTS IN CALL:--> " + count);
                log.info("*************************************");

                if (response != null) {

                    MuteParticipantResult muteResponse = client
                            .getCallConnection(addParticipantSucceededEvent.getCallConnectionId())
                            .muteParticipant(new CommunicationUserIdentifier(
                                    "8:acs:19ae37ff-1a44-4e19-aade-198eedddbdf2_00000020-b6bc-188e-02c3-593a0d00d233"));

                    if (muteResponse != null) {
                        log.info("Participant is muted. Waiting for confirmation...");
                        CallParticipant participant = client.getCallConnection(callConnectionId)
                                .getParticipant(new CommunicationUserIdentifier(
                                        "8:acs:19ae37ff-1a44-4e19-aade-198eedddbdf2_00000020-b6bc-188e-02c3-593a0d00d233"));
                        if (participant != null) {
                            log.info("Is participant muted: " + participant.isMuted());
                            log.info("Mute participant test completed.");
                        }
                    }
                }
                // var participantToRemove = new CommunicationUserIdentifier(
                // "8:acs:19ae37ff-1a44-4e19-aade-198eedddbdf2_00000020-b66f-52a5-2c8a-08482200cc97");

                // var participantToRemove = new
                // PhoneNumberIdentifier(appConfig.getTargetphonenumber());
                // client.getCallConnection(callConnectionId).removeParticipant(participantToRemove);

            } else if (event instanceof RemoveParticipantSucceeded) {
                var removeParticipantEvent = (RemoveParticipantSucceeded) event;
                log.info("Participant removed Successfully" + removeParticipantEvent.getParticipant().getRawId());
                client.getCallConnection(removeParticipantEvent.getCallConnectionId()).hangUp(true);
            } else if (event instanceof ConnectFailed) {
                var connectFailedEvent = (ConnectFailed) event;
                var resultInformation = connectFailedEvent.getResultInformation();
                log.error("Received connect failed event , message={}, code={}, subCode={}",
                        resultInformation.getMessage(),
                        resultInformation.getCode(),
                        resultInformation.getSubCode());
            } else if (event instanceof RecognizeCompleted) {
                log.info("Recognize Completed event received");
                RecognizeCompleted acsEvent = (RecognizeCompleted) event;
                var choiceResult = (ChoiceResult) acsEvent.getRecognizeResult().get();
                String labelDetected = choiceResult.getLabel();
                String phraseDetected = choiceResult.getRecognizedPhrase();
                log.info("Recognition completed, labelDetected=" + labelDetected + ", phraseDetected=" + phraseDetected
                        + ", context=" + event.getOperationContext());
                String textToPlay = labelDetected.equals(confirmLabel) ? confirmedText : cancelText;
                handlePlay(callConnectionId, textToPlay);
            } else if (event instanceof RecognizeFailed) {
                log.error("Received failed event: {}", ((CallAutomationEventBaseWithReasonCode) event)
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
                    startRecognizingWithChoiceOptions(callConnectionId, replyText, appConfig.getTargetphonenumber(),
                            retryContext);
                }
            } else if (event instanceof PlayCompleted || event instanceof PlayFailed) {
                log.info("Received Play Completed event. Terminating call");
                hangUp(callConnectionId);
            } else if (event instanceof CallDisconnected) {
                log.info("RECEIVED CALL DISCONNECTED EVENT");
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

    private CallAutomationClient initClient() {
        CallAutomationClient client;
        try {
            client = new CallAutomationClientBuilder()
                    .endpoint("https://nextpma.plat.skype.com")
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
