package com.communication.callautomation;

import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.CallConnection;
import com.azure.communication.callautomation.CallMedia;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.communication.common.PhoneNumberIdentifier;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@RestController
@Slf4j
public class ProgramSample {
    private AppConfig appConfig;
    private CallAutomationClient client;

    public ProgramSample(final AppConfig appConfig) {
        this.appConfig = appConfig;
        client = new CallAutomationClientBuilder()
                .connectionString(appConfig.getConnectionString())
                .buildClient();
    }

    @GetMapping(path = "/outboundCall")
    public ResponseEntity<String> outboundCall() {
        PhoneNumberIdentifier caller = new PhoneNumberIdentifier(appConfig.getCallerphonenumber());
        PhoneNumberIdentifier target = new PhoneNumberIdentifier(appConfig.getTargetphonenumber());
        CallInvite callInvite = new CallInvite(target, caller);
        client.createCall(callInvite, appConfig.getCallBackUri());
        log.info("createCall");

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/index.html")).build();
    }

    @PostMapping(path = "/api/callback")
    public ResponseEntity<String> callbackEvents(@RequestBody final String reqBody) {
        List<CallAutomationEventBase> events = CallAutomationEventParser.parseEvents(reqBody);
        for (CallAutomationEventBase event : events) {
            log.info("Received event {} for call connection id {}", event.getClass().getName(), event.getCallConnectionId());
            CallConnection callConnection = client.getCallConnection(event.getCallConnectionId());
            CallMedia callMedia = callConnection.getCallMedia();

            if (event instanceof CallConnected) {
                // Start continuous DTMF recognition
                PhoneNumberIdentifier targetParticipant = new PhoneNumberIdentifier(appConfig.getTargetphonenumber());
                callMedia.startContinuousDtmfRecognition(targetParticipant);
                log.info("startContinuousDtmfRecognition");
            } else if (event instanceof ContinuousDtmfRecognitionToneReceived) {
                ContinuousDtmfRecognitionToneReceived continuousDtmfRecognitionToneReceived = (ContinuousDtmfRecognitionToneReceived)event;
                log.info("DTMF tone received: {}", continuousDtmfRecognitionToneReceived.getToneInfo().getTone().convertToString());
                callConnection.hangUp(true);
            } else if (event instanceof ContinuousDtmfRecognitionToneFailed) {
                ContinuousDtmfRecognitionToneFailed continuousDtmfRecognitionToneFailed = (ContinuousDtmfRecognitionToneFailed)event;
                log.info("startContinuousDtmfRecognition failed with resultInformation: {}", continuousDtmfRecognitionToneFailed.getResultInformation().getMessage());
                callConnection.hangUp(true);
            }
        }
        return ResponseEntity.ok().body("");
    }
}
