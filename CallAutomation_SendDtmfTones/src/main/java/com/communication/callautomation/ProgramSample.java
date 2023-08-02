package com.communication.callautomation;

import com.azure.communication.callautomation.CallAutomationAsyncClient;
import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.CallConnection;
import com.azure.communication.callautomation.CallConnectionAsync;
import com.azure.communication.callautomation.CallMedia;
import com.azure.communication.callautomation.CallMediaAsync;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.communication.common.PhoneNumberIdentifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        return ResponseEntity.ok().body("");
    }

    @PostMapping(path = "/api/callback")
    public ResponseEntity<String> callbackEvents(@RequestBody final String reqBody) {
        List<CallAutomationEventBase> events = CallAutomationEventParser.parseEvents(reqBody);
        for (CallAutomationEventBase event : events) {
            log.info("Received event {} for call connection id {}", event.getClass().getName(), event.getCallConnectionId());
            CallConnection callConnection = client.getCallConnection(event.getCallConnectionId());
            CallMedia callMedia = callConnection.getCallMedia();

            if (event instanceof CallConnected) {
                // Send DTMF tones
                List<DtmfTone> tones = Arrays.asList(DtmfTone.ONE, DtmfTone.TWO, DtmfTone.THREE);
                PhoneNumberIdentifier targetParticipant = new PhoneNumberIdentifier(appConfig.getTargetphonenumber());
                callMedia.sendDtmf(tones, targetParticipant);
                log.info("sendDtmf");
            } else if (event instanceof SendDtmfCompleted) {
                log.info("sendDtmf completed successfully");
                callConnection.hangUp(true);
            } else if (event instanceof SendDtmfFailed) {
                SendDtmfFailed sendDtmfFailed = (SendDtmfFailed)event;
                log.info("sendDtmf failed with resultInformation: {}", sendDtmfFailed.getResultInformation().getMessage());
                callConnection.hangUp(true);
            }
        }
        return ResponseEntity.ok().body("");
    }
}
