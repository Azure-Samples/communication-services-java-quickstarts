package com.communication.CallPlayAudio;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.identity.CommunicationIdentityClient;
import com.azure.communication.identity.CommunicationIdentityClientBuilder;
import com.communication.CallPlayAudio.Ngrok.NgrokService;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {
    private static NgrokService ngrokService;
    final static String url = "http://localhost:9007";
    final static String serverPort = "9007";

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(App.class);
        application.setDefaultProperties(Collections.singletonMap("server.port", serverPort));
        application.run(args);

        Logger.logMessage(Logger.MessageType.INFORMATION, "Starting ACS Sample App ");

        // Get configuration properties
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        configurationManager.loadAppSettings();

        // Start Ngrok service
        String ngrokUrl = startNgrokService();
        try {
            if (ngrokUrl != null && !ngrokUrl.isEmpty()) {
                Logger.logMessage(Logger.MessageType.INFORMATION,"Server started at -- > " + url);
                Thread runSample = new Thread(() -> runSample(ngrokUrl));
                runSample.start();
                runSample.join();
            } else {
                Logger.logMessage(Logger.MessageType.INFORMATION,"Failed to start Ngrok service");
            }
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR,"Failed to start Ngrok service -- > " + ex.getMessage());
        }
        Logger.logMessage(Logger.MessageType.INFORMATION, "Press 'Ctrl + C' to exit the sample");
        ngrokService.dispose();
    }

    private static String startNgrokService() {
        try {
            ConfigurationManager configurationManager = ConfigurationManager.getInstance();
            String ngrokPath = configurationManager.getAppSettings("NgrokExePath");

            if (ngrokPath.isEmpty()) {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Ngrok path not provided");
                return null;
            }

            Logger.logMessage(Logger.MessageType.INFORMATION,"Starting Ngrok");
            ngrokService = new NgrokService(ngrokPath, null);

            Logger.logMessage(Logger.MessageType.INFORMATION,"Fetching Ngrok Url");
            String ngrokUrl = ngrokService.getNgrokUrl();

            Logger.logMessage(Logger.MessageType.INFORMATION,"Ngrok Started with url -- > " + ngrokUrl);
            return ngrokUrl;
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.INFORMATION,"Ngrok service got failed -- > " + ex.getMessage());
            return null;
        }
    }

    private static void runSample(String appBaseUrl) {
        CallConfiguration callConfiguration = initiateConfiguration(appBaseUrl);
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        String callplayaudioPairs = configurationManager.getAppSettings("DestinationIdentities");

        try {
            if (callplayaudioPairs != null && !callplayaudioPairs.isEmpty()) {
                String[] identities = callplayaudioPairs.split(";");
                ExecutorService executorService = Executors.newCachedThreadPool();
                Set<Callable<Boolean>> tasks = new HashSet<>();

                for (String identity : identities) {
                    if(!identity.isEmpty()) {
                        tasks.add(() -> {
                            new CallPlayTerminate(callConfiguration).report(identity.trim());
                            return true;
                        });
                    } else {
                        Logger.logMessage(Logger.MessageType.ERROR, "Malformed destination identitity --> " + identity);            
                    }
                }
                executorService.invokeAll(tasks);
                executorService.shutdown();
            }
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR, "Failed to initiate the callplayaudio call Exception -- > " + ex.getMessage());
        }
        deleteUser(callConfiguration.connectionString, callConfiguration.sourceIdentity);
    }

    /// <summary>
    /// Fetch configurations from App Settings and create source identity
    /// </summary>
    /// <param name="appBaseUrl">The base url of the app.</param>
    /// <returns>The <c CallConfiguration object.</returns>
    private static CallConfiguration initiateConfiguration(String appBaseUrl) {
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        String connectionString = configurationManager.getAppSettings("Connectionstring");
        String sourcePhoneNumber = configurationManager.getAppSettings("SourcePhone");
        String maxRetryAttemptCount = configurationManager.getAppSettings("MaxRetryCount");
        String sourceIdentity = createUser(connectionString);
        String audioFileName = generateCustomAudioMessage();
        return new CallConfiguration(connectionString, sourceIdentity, sourcePhoneNumber, appBaseUrl, audioFileName, maxRetryAttemptCount);
    }

    /// <summary>
    /// Get .wav Audio file
    /// </summary>
    private static String generateCustomAudioMessage() {
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        String key = configurationManager.getAppSettings("CognitiveServiceKey");
        String region = configurationManager.getAppSettings("CognitiveServiceRegion");
        String customMessage = configurationManager.getAppSettings("CustomMessage");

        try {
            if (key != null && !key.isEmpty() && region != null && !region.isEmpty() && customMessage != null
                    && !customMessage.isEmpty()) {
                SpeechConfig config = SpeechConfig.fromSubscription(key, region);
                config.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Riff16Khz16BitMonoPcm);

                AudioConfig audioConfig = AudioConfig.fromWavFileInput(
                        "src/main/java/com/communication/CallPlayAudio/audio/custom-message.wav");
                SpeechSynthesizer synthesizer = new SpeechSynthesizer(config, audioConfig);
                synthesizer.SpeakTextAsync(customMessage);
                synthesizer.close();
                return "custom-message.wav";
            }
            return "sample-message.wav";
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR,"Exception while generating text to speech, falling back to sample audio. Exception -- > " + ex.getMessage());
            return "sample-message.wav";
        }
    }

    /// <summary>
    /// Create new user
    /// </summary>
    private static String createUser(String connectionString) {
        Logger.logMessage(Logger.MessageType.INFORMATION,"createUser method");
        Logger.logMessage(Logger.MessageType.INFORMATION,"connection string is="+connectionString);

        CommunicationIdentityClient client = new CommunicationIdentityClientBuilder().connectionString(connectionString)
                .buildClient();
        CommunicationUserIdentifier user = client.createUser();
        return user.getId();
    }

    /// <summary>
    /// Delete the user
    /// </summary>
    private static void deleteUser(String connectionString, String source) {
        CommunicationIdentityClient client = new CommunicationIdentityClientBuilder().connectionString(connectionString)
                .buildClient();
        client.deleteUser(new CommunicationUserIdentifier(source));
    }
}
