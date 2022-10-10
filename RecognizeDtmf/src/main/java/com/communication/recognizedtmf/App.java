package com.communication.recognizedtmf;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.identity.CommunicationIdentityClient;
import com.azure.communication.identity.CommunicationIdentityClientBuilder;
import com.communication.recognizedtmf.Ngrok.NgrokService;
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

    public enum AudioName
    {
        GeneralAudio,
        SalesAudio,
        MarketingAudio,
        CustomerCareAudio,
        InvalidAudio
    }

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
        String outboundCallPairs = configurationManager.getAppSettings("DestinationIdentities");

        try {
            if (outboundCallPairs != null && !outboundCallPairs.isEmpty()) {
                String[] identities = outboundCallPairs.split(";");
                ExecutorService executorService = Executors.newCachedThreadPool();
                Set<Callable<Boolean>> tasks = new HashSet<>();

                for (String identity : identities) {
                    tasks.add(() -> {
                        new RecognizeDtmf(callConfiguration).report(identity);
                        return true;
                    });
                }
                executorService.invokeAll(tasks);
                executorService.shutdown();
            }
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR, "Failed to initiate the outbound call Exception -- > " + ex.getMessage());
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
        String audioFileName = generateCustomAudioMessage(AudioName.GeneralAudio);
        String salesAudioFileName = generateCustomAudioMessage(AudioName.SalesAudio);
        String marketingAudioFileName = generateCustomAudioMessage(AudioName.MarketingAudio);
        String customerCareAudioFileName = generateCustomAudioMessage(AudioName.CustomerCareAudio);
        String invalidAudioFileName = generateCustomAudioMessage(AudioName.InvalidAudio);

        return new CallConfiguration(connectionString, sourceIdentity, sourcePhoneNumber, 
        appBaseUrl, audioFileName, maxRetryAttemptCount, salesAudioFileName, marketingAudioFileName,
        customerCareAudioFileName, invalidAudioFileName);
    }

    /// <summary>
    /// Get .wav Audio file
    /// </summary>
    private static String generateCustomAudioMessage(AudioName audioName) {
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        String key = configurationManager.getAppSettings("CognitiveServiceKey");
        String region = configurationManager.getAppSettings("CognitiveServiceRegion");

        try {
            if (key != null && !key.isEmpty() && region != null && !region.isEmpty()) 
            {
                SpeechConfig config = SpeechConfig.fromSubscription(key, region);
                config.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Riff16Khz16BitMonoPcm);
                AudioConfig audioConfig = null;
                if(audioName == AudioName.GeneralAudio)
                {
                    String customMessage = configurationManager.getAppSettings("CustomMessage");
                    audioConfig = AudioConfig.fromWavFileInput("src/main/java/com/communication/recognizedtmf/audio/custom-message.wav");
                    SpeechSynthesizer synthesizer = new SpeechSynthesizer(config, audioConfig);
                    synthesizer.SpeakTextAsync(customMessage);
                    synthesizer.close();
                    return "custom-message.wav";
                }
                else if(audioName == AudioName.SalesAudio)
                {
                    String customMessage = configurationManager.getAppSettings("SalesCustomMessage");
                    audioConfig = AudioConfig.fromWavFileInput("src/main/java/com/communication/recognizedtmf/audio/custom-message.wav");
                    SpeechSynthesizer synthesizer = new SpeechSynthesizer(config, audioConfig);
                    synthesizer.SpeakTextAsync(customMessage);
                    synthesizer.close();
                    return "sales-message.wav";
                }
                else if(audioName == AudioName.MarketingAudio)
                {
                    String customMessage = configurationManager.getAppSettings("MarketingCustomMessage");
                    audioConfig = AudioConfig.fromWavFileInput("src/main/java/com/communication/recognizedtmf/audio/custom-message.wav");
                    SpeechSynthesizer synthesizer = new SpeechSynthesizer(config, audioConfig);
                    synthesizer.SpeakTextAsync(customMessage);
                    synthesizer.close();
                    return "marketing-message.wav";
                }
                else if(audioName == AudioName.CustomerCareAudio)
                {
                    String customMessage = configurationManager.getAppSettings("CustomerCustomMessage");
                    audioConfig = AudioConfig.fromWavFileInput("src/main/java/com/communication/recognizedtmf/audio/custom-message.wav");
                    SpeechSynthesizer synthesizer = new SpeechSynthesizer(config, audioConfig);
                    synthesizer.SpeakTextAsync(customMessage);
                    synthesizer.close();
                    return "customercare-message.wav";
                }
                else if(audioName == AudioName.InvalidAudio)
                {
                    String customMessage = configurationManager.getAppSettings("InvalidCustomMessage");
                    audioConfig = AudioConfig.fromWavFileInput("src/main/java/com/communication/recognizedtmf/audio/custom-message.wav");
                    SpeechSynthesizer synthesizer = new SpeechSynthesizer(config, audioConfig);
                    synthesizer.SpeakTextAsync(customMessage);
                    synthesizer.close();
                    return "invalid-message.wav";
                }
            }
            else
            {
                if (audioName == AudioName.GeneralAudio)
                {
                    return "sample-message.wav";
                }
                else if (audioName == AudioName.SalesAudio)
                {
                    return "sales.wav";
                }
                else if (audioName == AudioName.MarketingAudio)
                {
                    return "marketing.wav";
                }
                else if (audioName == AudioName.CustomerCareAudio)
                {
                    return "customercare.wav";
                }
                else if (audioName == AudioName.InvalidAudio)
                {
                    return "invalid.wav";
                }
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
