package com.communication.callautomation;

import com.azure.communication.callautomation.CallAutomationAsyncClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.communication.identity.implementation.models.CommunicationErrorResponseException;
import com.azure.core.http.rest.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@RestController
@Slf4j
public class ProgramSample {
    private AppConfig appConfig;
    private CallAutomationAsyncClient asyncClient;

    public ProgramSample(final AppConfig appConfig) {
        this.appConfig = appConfig;
        asyncClient = initClient();
    }

    @GetMapping(path = "/api/outbound_call")
    public ResponseEntity<String> handleOutboundCall() {
        String callConnectionId = createOutboundCall();
        return ResponseEntity.ok().body("Target participant: "
                + appConfig.getTargetphonenumber() +
                ", CallConnectionId: " + callConnectionId);
    }

    @PostMapping(path = "/api/callback")
    public ResponseEntity<String> handleCallbackEvents(@RequestBody final String reqBody) {
        List<CallAutomationEventBase> events = CallAutomationEventParser.parseEvents(reqBody);
        for (CallAutomationEventBase event : events) {
            String callConnectionId = event.getCallConnectionId();
            if (event instanceof CallConnected) {
                //start recording
                String callRecordingId = callRecording(callConnectionId);
                log.info("Call recording started with ID: {}", callRecordingId);
                // recognize tones
                String statusCode = recognizeDtmfTones(callConnectionId, "MainMenu.wav");
                log.info("DTMF recognition started. Request status code {}", statusCode);
            }
            else if (event instanceof RecognizeCompleted) {
                log.info("Recognize Completed event received");
                RecognizeCompleted recognizeEvent = (RecognizeCompleted) event;
                DtmfResult dtmfResult = (DtmfResult) recognizeEvent
                        .getRecognizeResult().get();
                DtmfTone selectedTone = dtmfResult.getTones().get(0);

                switch(selectedTone.convertToString()) {
                    case "1":
                        log.info("Received DTMF tone 1.");
                        log.info("Played media file with {}", playTo(callConnectionId, "Confirmed.wav"));
                        break;

                    case "2":
                        log.info("Received DTMF tone 2.");
                        log.info("Played media file with {}", playTo(callConnectionId, "Goodbye.wav"));
                        log.info("{}", hangUp(callConnectionId));
                        break;

                    default:
                        log.info("Another DTMF received. {}.", hangUp(callConnectionId));
                }
            }
            else if(event instanceof RecognizeFailed || event instanceof PlayFailed) {
                log.error("Received failed event: {}", ((CallAutomationEventBaseWithReasonCode) event)
                        .getResultInformation().getMessage());
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
            Response<CreateCallResult> result = asyncClient.createCallWithResponse(createCallOptions).block();
            return result.getValue().getCallConnectionProperties().getCallConnectionId();
        } catch (CommunicationErrorResponseException e) {
            log.error("Error when creating call: {} {}",
                    e.getMessage(),
                    e.getCause());
            return "";
        }
    }

    private String callRecording(final String callConnectionId) {
        try {
            log.info("Received Call Connected event, start call recording initiated.");
            ServerCallLocator serverCallLocator = new ServerCallLocator(
                    asyncClient.getCallConnectionAsync(callConnectionId)
                            .getCallProperties().block()
                            .getServerCallId());
            StartRecordingOptions startRecordingOptions = new StartRecordingOptions(serverCallLocator);
            Response<RecordingStateResult> response = asyncClient.getCallRecordingAsync()
                    .startWithResponse(startRecordingOptions).block();
            return response.getValue().getRecordingId();
        } catch (Exception e) {
            log.error("An error occurred when starting call recording: {} {}",
                    e.getMessage(),
                    e.getCause());
            return "";
        }
    }

    private String recognizeDtmfTones(final String callConnectionId, final String mediaFile) {
        try {
            CommunicationIdentifier target = CommunicationIdentifier
                    .fromRawId("4:" + appConfig.getTargetphonenumber());
            CallMediaRecognizeDtmfOptions recognizeDtmfOptions = new CallMediaRecognizeDtmfOptions(target, 1);
            PlaySource playSource = new FileSource()
                    .setUrl(appConfig.getBasecallbackuri() + "/" + mediaFile)
                    .setPlaySourceCacheId(mediaFile);
            recognizeDtmfOptions.setPlayPrompt(playSource)
                    .setInterruptPrompt(true)
                    .setInitialSilenceTimeout(Duration.ofSeconds(15));
            Response response = asyncClient.getCallConnectionAsync(callConnectionId)
                    .getCallMediaAsync()
                    .startRecognizingWithResponse(recognizeDtmfOptions).block();
            return "Response status code: " + response.getStatusCode();
        } catch (Exception e) {
            log.error("Error occurred when attempting to recognize DTMF tones {} {}",
                    e.getMessage(),
                    e.getCause());
            return "";
        }
    }

    private String playTo(final String callConnectionId, final String mediaFile) {
        try {
            List<CommunicationIdentifier> target = Arrays.asList(CommunicationIdentifier
                    .fromRawId("4:" + appConfig.getTargetphonenumber()));
            PlaySource playSource = new FileSource()
                    .setUrl(appConfig.getBasecallbackuri() + "/" + mediaFile)
                    .setPlaySourceCacheId(mediaFile);
            PlayOptions playOptions = new PlayOptions(playSource, target);
            Response response = asyncClient.getCallConnectionAsync(callConnectionId)
                    .getCallMediaAsync()
                    .playWithResponse(playOptions).block();
            return "Response status code:" + response.getStatusCode();
        } catch (Exception e) {
            log.error("Error occurred when playing media to participant {} {}",
                    e.getMessage(),
                    e.getCause());
            return "";
        }
    }

    private String hangUp(final String callConnectionId) {
        try {
            asyncClient.getCallConnectionAsync(callConnectionId).hangUp(true);
            return "Terminated call";
        } catch (Exception e) {
            log.error("Error when terminating the call for all participants {} {}",
                    e.getMessage(),
                    e.getCause());
            return "";
        }
    }

    private CallAutomationAsyncClient initClient() {
        CallAutomationAsyncClient asyncClient;
        try {
            asyncClient = new CallAutomationClientBuilder()
                    .connectionString(appConfig.getConnectionString())
                    .buildAsyncClient();
            return asyncClient;
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
