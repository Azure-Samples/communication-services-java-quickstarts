package com.communication.callautomation.core;

import com.communication.callautomation.core.model.EventInfo;
import com.communication.callautomation.exceptions.AzureCallAutomationException;

/**
 * Interface for the CallAutomation SDK services.
 */
public interface CallAutomationService {

    String answerCall(final EventInfo eventInfo);

    String startRecording(final String callConnectionId);

    String playAudio(final String callconnectionId, final String target, final String prompt);

    String singleDigitDtmfRecognitionWithPrompt(final String callconnectionId, final String target, final String prompt);

    String terminateCall(final String callconnectionId);
}
