package com.communication.callautomation;

import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.communication.common.MicrosoftTeamsUserIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.communication.identity.implementation.models.CommunicationErrorResponseException;
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

    private String MainMenu =
    """ 
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
            log.info(
                    "Received call event callConnectionID: {}, serverCallId: {}",
                    callConnectionId,
                    event.getServerCallId());

            if (event instanceof CallConnected) {
                // (Optional) Add a Microsoft Teams user to the call.  Uncomment the below snippet to enable Teams Interop scenario.
                // client.getCallConnection(callConnectionId).addParticipant(
                // {        
				//       new CallInvite(new MicrosoftTeamsUserIdentifier(appConfig.getTargetTeamsUserId()))
                //                 .setSourceDisplayName("Jack (Contoso Tech Support)"));
                
                // prepare recognize tones
                startRecognizingWithChoiceOptions(callConnectionId, MainMenu, appConfig.getTargetphonenumber(), "mainmenu");
            }
            else if (event instanceof RecognizeCompleted) {
                log.info("Recognize Completed event received");
                RecognizeCompleted acsEvent = (RecognizeCompleted) event; 
                var choiceResult = (ChoiceResult) acsEvent.getRecognizeResult().get();
                String labelDetected = choiceResult.getLabel();
                String phraseDetected = choiceResult.getRecognizedPhrase();
                log.info("Recognition completed, labelDetected=" + labelDetected + ", phraseDetected=" + phraseDetected + ", context=" + event.getOperationContext());
                String textToPlay = labelDetected.equals(confirmLabel) ? confirmedText  : cancelText;
                handlePlay(callConnectionId, textToPlay);
            }
            else if(event instanceof RecognizeFailed ) {
                log.error("Received failed event: {}", ((CallAutomationEventBaseWithReasonCode) event)
                        .getResultInformation().getMessage());
                var recognizeFailedEvent = (RecognizeFailed) event;
                var context =recognizeFailedEvent.getOperationContext();
                if(context != null && context.equals(retryContext)){
                    handlePlay(callConnectionId, noResponse);
                }
                else
                {
                    var resultInformation = recognizeFailedEvent.getResultInformation();
                    log.error("Encountered error during recognize, message={}, code={}, subCode={}",
                    resultInformation.getMessage(),
                    resultInformation.getCode(),
                    resultInformation.getSubCode());

                    var reasonCode = recognizeFailedEvent.getReasonCode();
                    String replyText = reasonCode == ReasonCode.Recognize.PLAY_PROMPT_FAILED ||
                        reasonCode == ReasonCode.Recognize.INITIAL_SILENCE_TIMEOUT ? customerQueryTimeout : 
                        reasonCode== ReasonCode.Recognize.INCORRECT_TONE_DETECTED ? invalidAudio: 
                        customerQueryTimeout;
                    
                        // prepare recognize tones
                    startRecognizingWithChoiceOptions(callConnectionId, replyText, appConfig.getTargetphonenumber(), retryContext);
                } 
            }
            else if(event instanceof PlayCompleted || event instanceof PlayFailed) {
                log.info("Received Play Completed event. Terminating call");
                hangUp(callConnectionId);
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
            CallIntelligenceOptions callIntelligenceOptions = new CallIntelligenceOptions().setCognitiveServicesEndpoint(appConfig.getCognitiveServiceEndpoint());
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

    private void handlePlay(final String  callConnectionId, String textToPlay) {
        var textPlay = new TextSource()
                .setText(textToPlay) 
                .setVoiceName("en-US-NancyNeural");

        client.getCallConnection(callConnectionId)
        .getCallMedia()
        .playToAll(textPlay);
    }

    private void startRecognizingWithChoiceOptions(final String callConnectionId, final String content, final String targetParticipant, final String context)
    {
        var playSource = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");

        var recognizeOptions = new CallMediaRecognizeChoiceOptions(new PhoneNumberIdentifier(targetParticipant),  getChoices())
            .setInterruptCallMediaOperation(false)
            .setInterruptPrompt(false)
            .setInitialSilenceTimeout(Duration.ofSeconds(10))
            .setPlayPrompt(playSource)
            .setOperationContext(context);

        client.getCallConnection(callConnectionId)
        .getCallMedia()
        .startRecognizing(recognizeOptions);
    }

    private List<RecognitionChoice> getChoices(){
        var choices = Arrays.asList(
            new RecognitionChoice().setLabel(confirmLabel).setPhrases(Arrays.asList("Confirm", "First", "One")).setTone(DtmfTone.ONE),
            new RecognitionChoice().setLabel(cancelLabel).setPhrases(Arrays.asList("Cancel", "Second", "Two")).setTone(DtmfTone.TWO)
            );
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
