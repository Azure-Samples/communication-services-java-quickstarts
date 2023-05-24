package com.communication.callautomation.acs.client;

import com.azure.communication.callautomation.models.*;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.communication.callautomation.config.AcsConfig;
import com.communication.callautomation.core.CallAutomationService;
import com.communication.callautomation.core.model.EventInfo;
import com.communication.callautomation.exceptions.AzureCallAutomationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
@Slf4j
public class AcsClient implements CallAutomationService {
    private final AcsConfig acsConfig;
    private final CallAutomationClientFactory callAutomationClientFactory;

    @Autowired
    public AcsClient(final AcsConfig acsConfig,
                     final CallAutomationClientFactory callAutomationClientFactory) {
        this.acsConfig = acsConfig;
        this.callAutomationClientFactory = callAutomationClientFactory;
    }

    @Override
    public String answerCall(final EventInfo eventInfo) {
        String incomingCallContext = eventInfo.getIncomingCallContext();
        String callbackUri = acsConfig.getCallbackUri(eventInfo.getFromId());

        log.info("Answering media call with following callbackuri: {}", callbackUri);

        try{
           AnswerCallResult answerCallResponse = callAutomationClientFactory.getCallAutomationClient()
                   .answerCall(incomingCallContext, callbackUri);
           return answerCallResponse.getCallConnectionProperties().getCallConnectionId();
        } catch(Exception e) {
            log.error("Error occurred when Answering the call");
            throw new AzureCallAutomationException(e.getMessage(), e);
        }
    }

    @Override
    public String startRecording(final String callConnectionId) {
        try {
            ServerCallLocator serverCallLocator = new ServerCallLocator(callAutomationClientFactory
                    .getCallAutomationClient()
                    .getCallConnection(callConnectionId)
                    .getCallProperties()
                    .getServerCallId());

            StartRecordingOptions recordingOptions = new StartRecordingOptions(serverCallLocator);

            Response<RecordingStateResult> response = callAutomationClientFactory.getCallAutomationClient().getCallRecording().startWithResponse(recordingOptions, Context.NONE);
            String recordingId = response.getValue().getRecordingId();
            log.info("Start Recording with recording ID: {}", recordingId);
            return "Start Recording operation finished";
        } catch(Exception e) {
            log.error("Recording operation failed {} {}", e.getMessage(), e.getCause());
            throw new AzureCallAutomationException(e.getMessage(), e);
        }
    }

    @Override
    public String playAudio(final String callconnectionId, final String target, final String prompt) {
        List<CommunicationIdentifier> listTargets = Arrays.asList(CommunicationIdentifier.fromRawId(target));
        PlaySource playSource = new FileSource().setUrl(acsConfig.getMediaUri(prompt));
        PlayOptions playOptions = new PlayOptions(playSource, listTargets);
        log.info("Play audio operation started");
        try {
            Response response = callAutomationClientFactory.getCallAutomationClient()
                    .getCallConnection(callconnectionId)
                    .getCallMedia()
                    .playWithResponse(playOptions, Context.NONE);
            return "Play audio operation finished";
        } catch(Exception e) {
            log.error("Error when Playing audio to participant {} {}", e.getMessage(), e.getCause());
            throw new AzureCallAutomationException(e.getMessage(), e);
        }
    }

    @Override
    public String singleDigitDtmfRecognitionWithPrompt(final String callconnectionId, final String target, final String prompt) {
        CommunicationIdentifier rectarget = CommunicationIdentifier.fromRawId(target);
        PlaySource playSource = new FileSource().setUrl(acsConfig.getMediaUri(prompt));
        CallMediaRecognizeDtmfOptions recognizeDtmfOptions = new CallMediaRecognizeDtmfOptions(rectarget, 1);
        recognizeDtmfOptions.setInterToneTimeout(Duration.ofSeconds(10))
                .setInitialSilenceTimeout(Duration.ofSeconds(15))
                .setInterruptPrompt(true)
                .setPlayPrompt(playSource);
        log.info("DTMF Recognition operation started");
        try {
            Response response = callAutomationClientFactory.getCallAutomationClient()
                    .getCallConnection(callconnectionId)
                    .getCallMedia()
                    .startRecognizingWithResponse(recognizeDtmfOptions, Context.NONE);
            return "DTMF Recognition operation ended";
        } catch(Exception e) {
            log.error("DTMF Recognition operation failed {} {}", e.getMessage(), e.getCause());
            throw new AzureCallAutomationException(e.getMessage(), e);
        }
    }

    @Override
    public String terminateCall(final String callconnectionId) {
        log.info("Terminating the call");
        try {
            callAutomationClientFactory.getCallAutomationClient().getCallConnection(callconnectionId).hangUp(true);
            return "HangUp call for all participants operation ended";
        } catch(Exception e) {
            log.error("HangUp call for all participants operation failed {} {}", e.getMessage(), e.getCause());
            throw new AzureCallAutomationException(e.getMessage(), e);
        }
    }
}
