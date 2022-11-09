package com.communication.appointmentreminder.utitilities;

import com.communication.appointmentreminder.ConfigurationManager;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

public class Speech {
    private static final String CUSTOM_REMINDER_MESSAGE_FILE_NAME = "custom-reminder-message.wav";
    private static final String REMINDER_MESSAGE = "reminder-message.wav";
    private static final String CUSTOM_CONFIRMATION_MESSAGE_FILE_NAME = "custom-confirmation-message.wav";
    private static final String CONFIRMATION_MESSAGE = "confirmation-message.wav";
    private static final String CUSTOM_CANCELLATION_MESSAGE_FILE_NAME = "custom-cancellation-message.wav";
    private static final String CANCELLATION_MESSAGE = "cancellation-message.wav";
    private static final String CUSTOM_NO_INPUT_MESSAGE_FILE_NAME = "custom-no-input-message.wav";
    private static final String NO_INPUT_MESSAGE = "no-input-message.wav";
    /// <summary>
    /// Get .wav Audio file
    /// </summary>
    public static String getReminderMessage() {
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        String key = configurationManager.getAppSettings("CognitiveServiceKey");
        String region = configurationManager.getAppSettings("CognitiveServiceRegion");
        String customMessage = configurationManager.getAppSettings("ReminderMessage");

        try {
            if (key != null && !key.isEmpty() && region != null && !region.isEmpty() && customMessage != null
                    && !customMessage.isEmpty()) {
                SpeechConfig config = SpeechConfig.fromSubscription(key, region);
                config.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Riff16Khz16BitMonoPcm);

                AudioConfig audioConfig = AudioConfig.fromWavFileInput(
                        Constants.AUDIO_FILES_ROUTE + CUSTOM_REMINDER_MESSAGE_FILE_NAME);
                SpeechSynthesizer synthesizer = new SpeechSynthesizer(config, audioConfig);
                synthesizer.SpeakTextAsync(customMessage).get();
                synthesizer.close();
                return CUSTOM_REMINDER_MESSAGE_FILE_NAME;
            }
            return REMINDER_MESSAGE;
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR,"Exception while generating text to speech, falling back to sample audio. Exception -- > " + ex.getMessage());
            return REMINDER_MESSAGE;
        }
    }
    public static String getConfirmationMessage() {
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        String key = configurationManager.getAppSettings("CognitiveServiceKey");
        String region = configurationManager.getAppSettings("CognitiveServiceRegion");
        String customMessage = configurationManager.getAppSettings("ConfirmationMessage");

        try {
            if (key != null && !key.isEmpty() && region != null && !region.isEmpty() && customMessage != null
                    && !customMessage.isEmpty()) {
                SpeechConfig config = SpeechConfig.fromSubscription(key, region);
                config.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Riff16Khz16BitMonoPcm);

                AudioConfig audioConfig = AudioConfig.fromWavFileInput(
                        Constants.AUDIO_FILES_ROUTE + CUSTOM_CONFIRMATION_MESSAGE_FILE_NAME);
                SpeechSynthesizer synthesizer = new SpeechSynthesizer(config, audioConfig);
                synthesizer.SpeakTextAsync(customMessage).get();
                synthesizer.close();
                return CUSTOM_CONFIRMATION_MESSAGE_FILE_NAME;
            }
            return CONFIRMATION_MESSAGE;
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR,"Exception while generating text to speech, falling back to sample audio. Exception -- > " + ex.getMessage());
            return CONFIRMATION_MESSAGE;
        }
    }

    public static String getCancellationMessage() {
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        String key = configurationManager.getAppSettings("CognitiveServiceKey");
        String region = configurationManager.getAppSettings("CognitiveServiceRegion");
        String customMessage = configurationManager.getAppSettings("CancellationMessage");

        try {
            if (key != null && !key.isEmpty() && region != null && !region.isEmpty() && customMessage != null
                    && !customMessage.isEmpty()) {
                SpeechConfig config = SpeechConfig.fromSubscription(key, region);
                config.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Riff16Khz16BitMonoPcm);

                AudioConfig audioConfig = AudioConfig.fromWavFileInput(
                        Constants.AUDIO_FILES_ROUTE + CUSTOM_CANCELLATION_MESSAGE_FILE_NAME);
                SpeechSynthesizer synthesizer = new SpeechSynthesizer(config, audioConfig);
                synthesizer.SpeakTextAsync(customMessage).get();
                synthesizer.close();
                return CUSTOM_CANCELLATION_MESSAGE_FILE_NAME;
            }
            return CANCELLATION_MESSAGE;
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR,"Exception while generating text to speech, falling back to sample audio. Exception -- > " + ex.getMessage());
            return CANCELLATION_MESSAGE;
        }
    }

    public static String getNoInputMessage() {
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        String key = configurationManager.getAppSettings("CognitiveServiceKey");
        String region = configurationManager.getAppSettings("CognitiveServiceRegion");
        String customMessage = configurationManager.getAppSettings("NoInputMessage");

        try {
            if (key != null && !key.isEmpty() && region != null && !region.isEmpty() && customMessage != null
                    && !customMessage.isEmpty()) {
                SpeechConfig config = SpeechConfig.fromSubscription(key, region);
                config.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Riff16Khz16BitMonoPcm);

                AudioConfig audioConfig = AudioConfig.fromWavFileInput(
                        Constants.AUDIO_FILES_ROUTE + CUSTOM_NO_INPUT_MESSAGE_FILE_NAME);
                SpeechSynthesizer synthesizer = new SpeechSynthesizer(config, audioConfig);
                synthesizer.SpeakTextAsync(customMessage).get();
                synthesizer.close();
                return CUSTOM_NO_INPUT_MESSAGE_FILE_NAME;
            }
            return NO_INPUT_MESSAGE;
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR,"Exception while generating text to speech, falling back to sample audio. Exception -- > " + ex.getMessage());
            return NO_INPUT_MESSAGE;
        }
    }
}
