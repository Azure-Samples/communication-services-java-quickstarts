package com.communication.callautomation;

import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.implementation.models.UnholdRequest;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.MicrosoftTeamsUserIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.communication.identity.implementation.models.CommunicationErrorResponseException;
import com.azure.core.http.rest.Response;
import com.azure.core.implementation.util.ListByteBufferContent;
import com.azure.core.util.Context;

import ch.qos.logback.core.read.ListAppender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.Thread.UncaughtExceptionHandler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
                    "Received call event callConnectionID: {}, serverCallId: {}, correlation Id: {}",
                    callConnectionId,
                    event.getServerCallId(),
                    event.getCorrelationId());

            if (event instanceof CallConnected) {
                // (Optional) Add a Microsoft Teams user to the call.  Uncomment the below snippet to enable Teams Interop scenario.
                // client.getCallConnection(callConnectionId).addParticipant(
                //       new CallInvite(new MicrosoftTeamsUserIdentifier(appConfig.getTargetTeamsUserId()))
                //                 .setSourceDisplayName("Jack (Contoso Tech Support)"));

                // //#region Transfer Call
                // PhoneNumberIdentifier caller = new PhoneNumberIdentifier(appConfig.getCallerphonenumber());
                // PhoneNumberIdentifier target = new PhoneNumberIdentifier("+919160985789");
                // TransferCallToParticipantOptions transferOption = new TransferCallToParticipantOptions(target);
                // transferOption.setOperationContext("transferCallContext");
                // transferOption.setSourceCallerIdNumber(caller);

                // // Sending event to a non-default endpoint.
                // transferOption.setOperationCallbackUrl(appConfig.getBasecallbackuri());
                // //TransferCallResult result = client.getCallConnection(callConnectionId).transferCallToParticipant(target);
                // var result = client.getCallConnection(callConnectionId).transferCallToParticipantWithResponse(transferOption, Context.NONE);
                // log.info("Call Transferred successfully");
                // //#endregion


                //#region Media Streaming
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
                //#endregion

                // call on hold
                // hold(callConnectionId);
                // log.info("Call On Hold successfully");
                // try {
                //     TimeUnit.SECONDS.sleep(5);    
                // } catch (Exception e) {
                    
                // }

                // unhold the call
                //unhold(callConnectionId);
                //log.info("Call UnHolded successfully");
                


                // //Start Transcription
                // startTranscription(callConnectionId);
                // try {
                //     TimeUnit.SECONDS.sleep(5);    
                // } catch (Exception e) {
                    
                // }
                
                // stopTranscription(callConnectionId);
                // try {
                //     TimeUnit.SECONDS.sleep(5);    
                // } catch (Exception e) {
                    
                // }

                // startTranscription(callConnectionId);
                // try {
                //     TimeUnit.SECONDS.sleep(5);    
                // } catch (Exception e) {
                    
                // }

                // stopTranscription(callConnectionId);
                // try {
                //     TimeUnit.SECONDS.sleep(5);    
                // } catch (Exception e) {
                    
                // }

                // startTranscription(callConnectionId);
                // try {
                //     TimeUnit.SECONDS.sleep(5);    
                // } catch (Exception e) {
                    
                // }

                log.info("Fetching recognize options...");
                // #region Recognize Prompt List
                // Different recognizing formats
                // // prepare recognize tones Choice
                startRecognizingWithChoiceOptions(callConnectionId, MainMenu, appConfig.getTargetphonenumber(), "mainmenu");

                // // prepare recognize tones DTMF
                // GetMediaRecognizeDTMFOptions(callConnectionId, MainMenu, appConfig.getTargetphonenumber(), "mainmenu");

                // // prepare recognize Speech
                // GetMediaRecognizeSpeechOptions(callConnectionId, MainMenu, appConfig.getTargetphonenumber(), "mainmenu");

                // // prepare recognize Speech or dtmf
                // GetMediaRecognizeSpeechOrDtmfOptions(callConnectionId, MainMenu, appConfig.getTargetphonenumber(), "mainmenu");
                //#endregion
            }
            else if (event instanceof RecognizeCompleted) {
                log.info("Recognize Completed event received");
                RecognizeCompleted acsEvent = (RecognizeCompleted) event; 
                var recognizeResult = acsEvent.getRecognizeResult().get();
                String labelDetected = "";
                String phraseDetected = "";
                
                if(recognizeResult instanceof ChoiceResult) {
                    var choiceResult = (ChoiceResult) acsEvent.getRecognizeResult().get();
                    labelDetected = choiceResult.getLabel();
                    phraseDetected = choiceResult.getRecognizedPhrase();
    
                }
                if(recognizeResult instanceof SpeechResult) {
                    var speechResult = (SpeechResult) acsEvent.getRecognizeResult().get();
                    phraseDetected = speechResult.getSpeech();
    
                }
                if(recognizeResult instanceof DtmfResult) {
                    var dtmfResult = (DtmfResult) acsEvent.getRecognizeResult().get();
                    phraseDetected = dtmfResult.getTones().get(0).convertToString();
    
                }           
                
                log.info("Recognition completed, labelDetected=" + labelDetected + ", phraseDetected=" + phraseDetected + ", context=" + event.getOperationContext());
                String textToPlay = labelDetected.equals(confirmLabel) ? confirmedText  : cancelText;
                // stop Media Streaming
                // stopMediaStreamingOptions(callConnectionId);
                // var callConnectionProperties = client.getCallConnection(callConnectionId).getCallProperties();
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
            else if(event instanceof TranscriptionStarted) {
                log.info("TranscriptionStarted event triggered");
            }
            else if(event instanceof TranscriptionStopped) {
                log.info("TranscriptionStopped event triggered");                
            }
            else if(event instanceof TranscriptionFailed) {
                log.info("TranscriptionFailed event triggered");
            }
            else if(event instanceof MediaStreamingStarted) {
            log.info("MediaStreamingStarted event triggered.");
            }
            else if(event instanceof MediaStreamingStopped) {
                log.info("MediaStreamingStopped event triggered.");
            }
            else if(event instanceof MediaStreamingFailed) {
                log.info("MediaStreamingFailed event triggered.");
            }
            else if(event instanceof PlayStarted) {
                log.info("PlayStarted event triggered.");
            }
            else if(event instanceof CallTransferAccepted) {
                log.info("CallTransferAccepted event triggered.");
            }
            else if(event instanceof CallTransferFailed) {
                log.info("CallTransFailded event triggered.");
            }
            else if(event instanceof PlayCompleted || event instanceof PlayFailed) {
                log.info("Received Play Completed event. Terminating call");
                hangUp(callConnectionId);
            }
            else if(event instanceof CallDisconnected) {
                log.info("The Call got Disconnected");
                            
            }

        }
        return ResponseEntity.ok().body("");
    }

    private String createOutboundCall() {
        try {
            PhoneNumberIdentifier caller = new PhoneNumberIdentifier(appConfig.getCallerphonenumber());
            PhoneNumberIdentifier target = new PhoneNumberIdentifier(appConfig.getTargetphonenumber());
            CallInvite callInvite = new CallInvite(target, caller);
            
            CallIntelligenceOptions callIntelligenceOptions = new CallIntelligenceOptions().setCognitiveServicesEndpoint(appConfig.getCognitiveServiceEndpoint());
            TranscriptionOptions transcriptionOptions = new TranscriptionOptions(appConfig.getWebSocketUrl(), TranscriptionTransport.WEBSOCKET, "en-US", false);
            MediaStreamingOptions mediaStreamingOptions = new MediaStreamingOptions(appConfig.getWebSocketUrl(), MediaStreamingTransport.WEBSOCKET, MediaStreamingContentType.AUDIO, MediaStreamingAudioChannel.UNMIXED);
            mediaStreamingOptions.setStartMediaStreaming(false);
        
            CreateCallOptions createCallOptions = new CreateCallOptions(callInvite, appConfig.getCallBackUri());
            createCallOptions.setCallIntelligenceOptions(callIntelligenceOptions);
            createCallOptions.setTranscriptionOptions(transcriptionOptions);
            createCallOptions.setMediaStreamingOptions(mediaStreamingOptions);

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
        List<CommunicationIdentifier> playToList = Arrays.asList(
             new PhoneNumberIdentifier(appConfig.getTargetphonenumber())
        );

        var textPlay = new TextSource()
                .setText(textToPlay) 
                .setVoiceName("en-US-NancyNeural");
        var playOptions = new PlayOptions(textPlay, playToList);

        client.getCallConnection(callConnectionId)
        .getCallMedia()
        .playWithResponse(playOptions, Context.NONE);

        // // Inturrput Prompt test
        // var textPlay = new TextSource()
        // .setText("First Interrupt prompt message") 
        // .setVoiceName("en-US-NancyNeural");
        
        // var playToAllOptions = new PlayToAllOptions(textPlay)
        //             .setLoop(false)
        //             .setOperationCallbackUrl(appConfig.getBasecallbackuri())
        //             .setInterruptCallMediaOperation(false);
        
        // //with options
        // client.getCallConnection(callConnectionId)
        // .getCallMedia()
        // .playToAllWithResponse(playToAllOptions, Context.NONE);
        // // // with out options
        // // client.getCallConnection(callConnectionId)
        // // .getCallMedia()
        // // .playToAll(textPlay);

        // var textPlay1 = new TextSource()
        // .setText("Interrupt second prompt message") 
        // .setVoiceName("en-US-NancyNeural");
        
        // var playToAllOptions1 = new PlayToAllOptions(textPlay1)
        //             .setLoop(false)
        //             .setOperationCallbackUrl(appConfig.getBasecallbackuri())
        //             .setInterruptCallMediaOperation(true);
        
        // //with options
        // client.getCallConnection(callConnectionId)
        // .getCallMedia()
        // .playToAllWithResponse(playToAllOptions1, Context.NONE);

        // //File source
        // var interruptFile = new FileSource()
        //         .setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/StarWars3.wav");

        // var playFileOptions = new PlayToAllOptions(interruptFile)
        //         .setLoop(false)
        //         .setOperationCallbackUrl(appConfig.getBasecallbackuri())
        //         .setInterruptCallMediaOperation(false);

        // //with options
        // client.getCallConnection(callConnectionId)
        //         .getCallMedia()
        //         .playToAllWithResponse(playFileOptions, Context.NONE);
        // // //with out options
        // // client.getCallConnection(callConnectionId)
        // //         .getCallMedia()
        // //         .playToAll(interruptFile);
     }

    // private void startRecognizingWithChoiceOptions(final String callConnectionId, final String content, final String targetParticipant, final String context)
    // {
    //     var playSource = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");

    //     var recognizeOptions = new CallMediaRecognizeChoiceOptions(new PhoneNumberIdentifier(targetParticipant),  getChoices())
    //         .setInterruptCallMediaOperation(false)
    //         .setInterruptPrompt(false)
    //         .setInitialSilenceTimeout(Duration.ofSeconds(10))
    //         .setPlayPrompt(playSource)
    //         .setOperationContext(context);

    //     client.getCallConnection(callConnectionId)
    //     .getCallMedia()
    //     .startRecognizing(recognizeOptions);
    // }

    // RecognizingWithChoiceOptions for Multiple Prompts
    private void startRecognizingWithChoiceOptions(final String callConnectionId, final String content, final String targetParticipant, final String context)
    {
        var playSource = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");
        // //Multiple TextSource prompt
        // var p1 = new TextSource().setText("recognize prompt one").setVoiceName("en-US-NancyNeural");
        // var p2 = new TextSource().setText("recognize prompt two").setVoiceName("en-US-NancyNeural");
        // var p3 = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");
        
        // var playSources = new ArrayList<PlaySource>();
        // playSources.add(p1);
        // playSources.add(p2);
        // playSources.add(p3);

        // //Multiple FileSource Prompts
        // var p1 = new FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/StarWars3.wav");

        // var p2 = new FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/preamble10.wav");

        // var playSources = new ArrayList<PlaySource>();
        // playSources.add(p1);
        // playSources.add(p2);

        //Multiple TextSource and FileSource prompt
        var p1 = new TextSource().setText("recognize prompt one").setVoiceName("en-US-NancyNeural");
        var p2 = new FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/preamble10.wav");
        var p3 = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");
        var playSources = new ArrayList<PlaySource>();
        playSources.add(p1);
        playSources.add(p2);
        playSources.add(p3);
        
        // //Empty play sources
        //var playSources = new ArrayList<PlaySource>();

        var recognizeOptions = new CallMediaRecognizeChoiceOptions(new PhoneNumberIdentifier(targetParticipant),  getChoices())
            .setInterruptCallMediaOperation(false)
            .setInterruptPrompt(false)
            .setInitialSilenceTimeout(Duration.ofSeconds(10))
            //.setPlayPrompt(playSource)
            .setPlayPrompts(playSources)
            .setOperationContext(context);

        client.getCallConnection(callConnectionId)
        .getCallMedia()
        .startRecognizing(recognizeOptions);
    }

    // MediaRecognizeDTMFOptions for Multiple Prompts
    private void GetMediaRecognizeDTMFOptions(final String callConnectionId, final String content, final String targetParticipant, final String context)
    {
        var playSource = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");
        // //Multiple TextSource prompt
        // var p1 = new TextSource().setText("recognize prompt one").setVoiceName("en-US-NancyNeural");
        // var p2 = new TextSource().setText("recognize prompt two").setVoiceName("en-US-NancyNeural");
        // var p3 = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");
        
        // var playSources = new ArrayList<PlaySource>();
        // playSources.add(p1);
        // playSources.add(p2);
        // playSources.add(p3);

        // //Multiple FileSource Prompts
        // var p1 = new FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/StarWars3.wav");

        // var p2 = new FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/preamble10.wav");

        // var playSources = new ArrayList<PlaySource>();
        // playSources.add(p1);
        // playSources.add(p2);

        //Multiple TextSource prompt
        var p1 = new TextSource().setText("recognize prompt one").setVoiceName("en-US-NancyNeural");
        var p2 = new FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/preamble10.wav");
        var p3 = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");
        var playSources = new ArrayList<PlaySource>();
        playSources.add(p1);
        playSources.add(p2);
        playSources.add(p3);
        
        //Empty play sources
        //var playSources = new ArrayList<PlaySource>();

        var recognizeOptions = new CallMediaRecognizeDtmfOptions(new PhoneNumberIdentifier(targetParticipant),8 )
            .setInterruptCallMediaOperation(false)
            .setInterruptPrompt(false)
            .setInterToneTimeout(Duration.ofSeconds(5))
            .setInitialSilenceTimeout(Duration.ofSeconds(15))
            //.setPlayPrompt(playSource)
            .setPlayPrompts(playSources)
            .setOperationContext(context);

        client.getCallConnection(callConnectionId)
        .getCallMedia()
        .startRecognizing(recognizeOptions);
    }

    // MediaRecognizeSpeechOptions for Multiple Prompts
    private void GetMediaRecognizeSpeechOptions(final String callConnectionId, final String content, final String targetParticipant, final String context)
    {
        var playSource = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");
        // //Multiple TextSource prompt
        // var p1 = new TextSource().setText("recognize prompt one").setVoiceName("en-US-NancyNeural");
        // var p2 = new TextSource().setText("recognize prompt two").setVoiceName("en-US-NancyNeural");
        // var p3 = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");
        
        // var playSources = new ArrayList<PlaySource>();
        // playSources.add(p1);
        // playSources.add(p2);
        // playSources.add(p3);

        // //Multiple FileSource Prompts
        // var p1 = new FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/StarWars3.wav");

        // var p2 = new FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/preamble10.wav");

        // var playSources = new ArrayList<PlaySource>();
        // playSources.add(p1);
        // playSources.add(p2);

        //Multiple TextSource prompt
        var p1 = new TextSource().setText("recognize prompt one").setVoiceName("en-US-NancyNeural");
        var p2 = new FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/preamble10.wav");
        var p3 = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");
        var playSources = new ArrayList<PlaySource>();
        playSources.add(p1);
        playSources.add(p2);
        playSources.add(p3);
        
        //Empty play sources
        //var playSources = new ArrayList<PlaySource>();
        var target = new PhoneNumberIdentifier(targetParticipant);
        var recognizeOptions = new CallMediaRecognizeSpeechOptions(target, Duration.ofSeconds(15))
            .setInterruptPrompt(false)
            .setInitialSilenceTimeout(Duration.ofSeconds(15))
            //.setPlayPrompt(playSource)
            .setPlayPrompts(playSources)
            .setOperationContext(context);

        client.getCallConnection(callConnectionId)
        .getCallMedia()
        .startRecognizing(recognizeOptions);
    }

    // MediaRecognizeSpeechOrDtmfOptions for Multiple Prompts
    private void GetMediaRecognizeSpeechOrDtmfOptions(final String callConnectionId, final String content, final String targetParticipant, final String context)
    {
        var playSource = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");
        //Multiple TextSource prompt
        // var p1 = new TextSource().setText("recognize prompt one").setVoiceName("en-US-NancyNeural");
        // var p2 = new TextSource().setText("recognize prompt two").setVoiceName("en-US-NancyNeural");
        // var p3 = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");
        
        // var playSources = new ArrayList<PlaySource>();
        // playSources.add(p1);
        // playSources.add(p2);
        // playSources.add(p3);

        // //Multiple FileSource Prompts
        // var p1 = new FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/StarWars3.wav");

        // var p2 = new FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/preamble10.wav");

        // var playSources = new ArrayList<PlaySource>();
        // playSources.add(p1);
        // playSources.add(p2);

        //Multiple TextSource prompt
        var p1 = new TextSource().setText("recognize prompt one").setVoiceName("en-US-NancyNeural");
        var p2 = new FileSource().setUrl("https://www2.cs.uic.edu/~i101/SoundFiles/preamble10.wav");
        var p3 = new TextSource().setText(content).setVoiceName("en-US-NancyNeural");
        var playSources = new ArrayList<PlaySource>();
        playSources.add(p1);
        playSources.add(p2);
        playSources.add(p3);
        
        //Empty play sources
        //var playSources = new ArrayList<PlaySource>();

        var recognizeOptions = new CallMediaRecognizeSpeechOrDtmfOptions(new PhoneNumberIdentifier(targetParticipant), 8,Duration.ofSeconds(15))
            .setInterruptPrompt(false)
            .setInitialSilenceTimeout(Duration.ofSeconds(10))
            //.setPlayPrompt(playSource)
            .setPlayPrompts(playSources)
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

    private void startTranscription(String callConnectionId) {
        StartTranscriptionOptions transcriptionOptions = new StartTranscriptionOptions()
                                                        .setOperationContext("startMediaStreamingContext");
        //// with options
        // client.getCallConnection(callConnectionId)
        //         .getCallMedia()
        //         .startTranscriptionWithResponse(transcriptionOptions, Context.NONE);

        // without options
        client.getCallConnection(callConnectionId)
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
        client.getCallConnection(callConnectionId)
                    .getCallMedia()
                    .stopTranscription();
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
    
    private void hold(final String callConnectionId){
        PhoneNumberIdentifier target = new PhoneNumberIdentifier(appConfig.getTargetphonenumber());
        HoldOptions holdOptions = new HoldOptions(target)
                                    .setOperationCallbackUrl(appConfig.getBasecallbackuri())
                                    .setOperationContext("holdPstnParticipant");
                                    var textPlay = new TextSource()
                                    .setText("i am on hold, please wait") 
                                    .setVoiceName("en-US-NancyNeural");
        holdOptions.setPlaySource(textPlay);

        //PhoneNumberIdentifier target = new PhoneNumberIdentifier(appConfig.getTargetphonenumber());
        
        // without options                           
        //client.getCallConnection(callConnectionId).getCallMedia().hold(target);

        // with options
        client.getCallConnection(callConnectionId).getCallMedia().holdWithResponse(holdOptions, Context.NONE);
    }

    private void unhold(final String callConnectionId){

        PhoneNumberIdentifier target = new PhoneNumberIdentifier(appConfig.getTargetphonenumber());
        //without options
        //client.getCallConnection(callConnectionId).getCallMedia().unhold(target);

        //with options
        client.getCallConnection(callConnectionId).getCallMedia().unholdWithResponse(target, "unholdPstnParticipant", Context.NONE);
    }

    private void startMediaStreamingOptions(final String callConnectionId){
        StartMediaStreamingOptions startOptions = new StartMediaStreamingOptions()
                                                        .setOperationContext("startMediaStreamingContext")
                                                        .setOperationCallbackUrl(appConfig.getBasecallbackuri());
        //without options
        client.getCallConnection(callConnectionId)
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

        // without options
        client.getCallConnection(callConnectionId)
                    .getCallMedia()
                    .stopMediaStreaming();
        // // with options
        // client.getCallConnection(callConnectionId)
        //             .getCallMedia()
        //             .stopMediaStreamingWithResponse(stopOptions, Context.NONE);
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
