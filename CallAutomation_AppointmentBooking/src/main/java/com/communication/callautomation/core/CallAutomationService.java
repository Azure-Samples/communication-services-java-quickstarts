package com.communication.callautomation.core;

import com.communication.callautomation.core.model.EventInfo;
import com.communication.callautomation.exceptions.AzureCallAutomationException;

/**
 * Interface for the CallAutomation SDK services.
 */
public interface CallAutomationService {

    String answerCall(final EventInfo eventInfo);

    String startRecording(final EventInfo eventInfo);

    String playAudio(final EventInfo eventInfo, final String prompt);

    String singleDigitDtmfRecognitionWithPrompt(final EventInfo eventInfo, final String prompt);

    String terminateCall(final EventInfo eventInfo);
}
