package com.communication.callautomation;

import java.net.URI;
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
import com.azure.communication.callautomation.models.DtmfTone;
import com.azure.communication.callautomation.models.events.CallAutomationEventBase;
import com.azure.communication.callautomation.models.events.CallConnected;
import com.azure.communication.callautomation.models.events.SendDtmfTonesCompleted;
import com.azure.communication.callautomation.models.events.SendDtmfTonesFailed;
import com.azure.communication.common.PhoneNumberIdentifier;

import lombok.extern.slf4j.Slf4j;

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
                // Send DTMF tones
                List<DtmfTone> tones = Arrays.asList(DtmfTone.ONE, DtmfTone.TWO, DtmfTone.THREE, DtmfTone.POUND);
                callAutomationClient.getCallConnectionAsync(callConnectionId)
                        .getCallMediaAsync()
                        .sendDtmfTonesWithResponse(tones, new PhoneNumberIdentifier(c2Target), "dtmfs-to-ivr")
                        .block();
                log.info("sendDtmfTones");
            }
            if (acsEvent instanceof SendDtmfTonesCompleted) {
                SendDtmfTonesCompleted event = (SendDtmfTonesCompleted) acsEvent;
                log.info("Send dtmf succeeded: context=" + event.getOperationContext());
            }
            if (acsEvent instanceof SendDtmfTonesFailed) {
                SendDtmfTonesFailed event = (SendDtmfTonesFailed) acsEvent;
                log.info("Send dtmf failed: result=" + event.getResultInformation().getMessage() + ", context="
                        + event.getOperationContext());
            }
        }
        return ResponseEntity.ok().body("");
    }
}
