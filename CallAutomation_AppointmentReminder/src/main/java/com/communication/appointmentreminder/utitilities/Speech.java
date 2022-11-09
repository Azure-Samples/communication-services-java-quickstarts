package com.communication.appointmentreminder.utitilities;

import com.communication.appointmentreminder.ConfigurationManager;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import java.util.Map;

/** Class that could generate messages using Azure Cognitive Speech **/
public class Speech {
    private enum Messages  {
        REMINDER,
        CONFIRMATION,
        CANCELLATION,
        NO_OP,
        INVALID
    }

    private static final Map<Messages, String> APP_SETTINGS_KEYS = Map.of(
        Messages.REMINDER, "ReminderMessage",
        Messages.CANCELLATION, "CancellationMessage",
        Messages.CONFIRMATION, "ConfirmationMessage",
        Messages.NO_OP, "NoInputMessage",
        Messages.INVALID, "InvalidInputMessage"
    );

    private static final String CUSTOM_REMINDER_MESSAGE_FILE_NAME = "custom-reminder-message.wav";
    private static final String REMINDER_MESSAGE = "reminder-message.wav";
    private static final String CUSTOM_CONFIRMATION_MESSAGE_FILE_NAME = "custom-confirmation-message.wav";
    private static final String CONFIRMATION_MESSAGE = "confirmation-message.wav";
    private static final String CUSTOM_CANCELLATION_MESSAGE_FILE_NAME = "custom-cancellation-message.wav";
    private static final String CANCELLATION_MESSAGE = "cancellation-message.wav";
    private static final String CUSTOM_NO_INPUT_MESSAGE_FILE_NAME = "custom-no-input-message.wav";
    private static final String NO_INPUT_MESSAGE = "no-input-message.wav";
    private static final String CUSTOM_INVALID_INPUT_MESSAGE_FILE_NAME = "custom-invalid-input-message.wav";
    private static final String INVALID_INPUT_MESSAGE = "invalid-input-message.wav";

    /// <summary>
    /// Get .wav Audio file for Reminder message.
    /// </summary>
    public static String getReminderMessage() {
        return getMessage(Messages.REMINDER, CUSTOM_REMINDER_MESSAGE_FILE_NAME, REMINDER_MESSAGE);
    }

    /// <summary>
    /// Get .wav Audio file for Appointment Confirmed message.
    /// </summary>
    public static String getConfirmationMessage() {
        return getMessage(Messages.CONFIRMATION, CUSTOM_CONFIRMATION_MESSAGE_FILE_NAME, CONFIRMATION_MESSAGE);
    }

    /// <summary>
    /// Get .wav Audio file for Appointment Cancellation message.
    /// </summary>
    public static String getCancellationMessage() {
        return getMessage(Messages.CANCELLATION, CUSTOM_CANCELLATION_MESSAGE_FILE_NAME, CANCELLATION_MESSAGE);
    }

    /// <summary>
    /// Get .wav Audio file for No Input received message.
    /// </summary>
    public static String getNoInputMessage() {
        return getMessage(Messages.NO_OP, CUSTOM_NO_INPUT_MESSAGE_FILE_NAME, NO_INPUT_MESSAGE);
    }

    /// <summary>
    /// Get .wav Audio file for Invalid Input received message.
    /// </summary>
    public static String getInvalidInputMessage() {
        return getMessage(Messages.INVALID, CUSTOM_INVALID_INPUT_MESSAGE_FILE_NAME, INVALID_INPUT_MESSAGE);
    }

    private static String getMessage(Messages messageKind, String customName, String defaultName) {
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        String key = configurationManager.getAppSettings("CognitiveServiceKey");
        String region = configurationManager.getAppSettings("CognitiveServiceRegion");
        String customMessage = configurationManager.getAppSettings(APP_SETTINGS_KEYS.get(messageKind));

        try {
            if (key != null && !key.isEmpty() && region != null && !region.isEmpty() && customMessage != null
                    && !customMessage.isEmpty()) {
                SpeechConfig config = SpeechConfig.fromSubscription(key, region);
                config.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Riff16Khz16BitMonoPcm);

                AudioConfig audioConfig = AudioConfig.fromWavFileInput(
                        Constants.AUDIO_FILES_ROUTE + customName);
                SpeechSynthesizer synthesizer = new SpeechSynthesizer(config, audioConfig);
                synthesizer.SpeakTextAsync(customMessage).get();
                synthesizer.close();
                return customName;
            }
            return defaultName;
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR,"Exception while generating text to speech, falling back to sample audio. Exception -- > " + ex.getMessage());
            return defaultName;
        }
    }
}
