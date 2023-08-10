package com.communication.callautomation;

import com.azure.communication.callautomation.CallAutomationAsyncClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.communication.common.PhoneNumberIdentifier;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@Slf4j
public class ProgramSample {
    private AppConfig appConfig;
    private CallAutomationAsyncClient callAutomationClient;
    private String c2Target = appConfig.getTargetphonenumber();

    public ProgramSample(final AppConfig appConfig) {
        this.appConfig = appConfig;
        callAutomationClient = new CallAutomationClientBuilder()
                .connectionString(appConfig.getConnectionString())
                .buildAsyncClient();
    }

    @GetMapping(path = "/outboundCall")
    public ResponseEntity<String> outboundCall() {
        PhoneNumberIdentifier target = new PhoneNumberIdentifier(c2Target);
        PhoneNumberIdentifier caller = new PhoneNumberIdentifier(appConfig.getCallerphonenumber());
        CallInvite callInvite = new CallInvite(target, caller);
        callAutomationClient.createCall(callInvite, appConfig.getCallBackUri());
        log.info("createCall");

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/index.html")).build();
    }

    @PostMapping(path = "/api/callback")
    public ResponseEntity<String> callbackEvents(@RequestBody final String reqBody) {
        List<CallAutomationEventBase> events = CallAutomationEventParser.parseEvents(reqBody);
        for (CallAutomationEventBase acsEvent : events) {
            String callConnectionId = acsEvent.getCallConnectionId();
            log.info("Received event {} for call connection id {}", acsEvent.getClass().getName(), callConnectionId);

            if (acsEvent instanceof CallConnected) {
                // Start continuous DTMF recognition
                callAutomationClient.getCallConnectionAsync(callConnectionId)
                        .getCallMediaAsync()
                        .startContinuousDtmfRecognitionWithResponse(new PhoneNumberIdentifier(c2Target), "dtmf-reco-on-c2")
                        .block();
                                log.info("startContinuousDtmfRecognition");
            }

            if (acsEvent instanceof ContinuousDtmfRecognitionToneReceived) {
                ContinuousDtmfRecognitionToneReceived event = (ContinuousDtmfRecognitionToneReceived) acsEvent;
                log.info("Tone detected: sequenceId=" + event.getToneInfo().getSequenceId()
                    + ", tone=" + event.getToneInfo().getTone().convertToString()
                    + ", context=" + event.getOperationContext());
                            
            callAutomationClient.getCallConnectionAsync(callConnectionId)
                    .getCallMediaAsync()
                    .stopContinuousDtmfRecognitionWithResponse(new PhoneNumberIdentifier(c2Target), "dtmf-reco-on-c2")
                    .block();
            }

            if (acsEvent instanceof ContinuousDtmfRecognitionToneFailed) {
                ContinuousDtmfRecognitionToneFailed event = (ContinuousDtmfRecognitionToneFailed) acsEvent;
                log.info("Tone failed: result="+ event.getResultInformation().getMessage()
                    + ", context=" + event.getOperationContext());
            }

            if (acsEvent instanceof ContinuousDtmfRecognitionStopped) {
                ContinuousDtmfRecognitionStopped event = (ContinuousDtmfRecognitionStopped) acsEvent;
                log.info("Tone stopped, context=" + event.getOperationContext());
            }
        }
        return ResponseEntity.ok().body("");
    }
}
