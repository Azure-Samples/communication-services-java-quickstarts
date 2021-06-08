package com.communication.outboundcallreminder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.identity.CommunicationIdentityClient;
import com.azure.communication.identity.CommunicationIdentityClientBuilder;
import com.communication.outboundcallreminder.Ngrok.NgrokService;
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

        System.out.println("Starting ACS Sample App \n");

        // Get configuration properties
        ConfigurationManager configurationManager = ConfigurationManager.GetInstance();
        configurationManager.LoadAppSettings();

        // Start Ngrok service
        String ngrokUrl = StartNgrokService();
        try {
            if (ngrokUrl != null && !ngrokUrl.isEmpty()) {
                System.out.println("Server started at:" + url);
                Thread runSample = new Thread(() -> {
                    RunSample(ngrokUrl);
                });
                runSample.start();
                runSample.join();
            } else {
                System.out.println("Failed to start Ngrok service");
            }
        } catch (Exception ex) {
            System.out.println("Failed to start Ngrok service : " + ex.getMessage());
        }
        System.out.println("Press 'Ctrl + C' to exit the sample");
        ngrokService.Dispose();
    }

    private static String StartNgrokService() {
        try {
            ConfigurationManager configurationManager = ConfigurationManager.GetInstance();
            String ngrokPath = configurationManager.GetAppSettings("NgrokExePath");

            if (ngrokPath.isEmpty()) {
                System.out.println("Ngrok path not provided");
                return null;
            }

            System.out.println("Starting Ngrok");
            ngrokService = new NgrokService(ngrokPath, null);

            System.out.println("Fetching Ngrok Url");
            String ngrokUrl = ngrokService.GetNgrokUrl();

            System.out.println("Ngrok Started with url: " + ngrokUrl);
            return ngrokUrl;
        } catch (Exception ex) {
            System.out.println("Ngrok service got failed : " + ex.getMessage());
            return null;
        }
    }

    private static void RunSample(String appBaseUrl) {
        CallConfiguration callConfiguration = InitiateConfiguration(appBaseUrl);
        ConfigurationManager configurationManager = ConfigurationManager.GetInstance();
        String outboundCallPairs = configurationManager.GetAppSettings("DestinationIdentities");

        try {
            if (outboundCallPairs != null && !outboundCallPairs.isEmpty()) {
                String[] identities = outboundCallPairs.split(";");
                ExecutorService executorService = Executors.newCachedThreadPool();
                Set<Callable<Boolean>> tasks = new HashSet<Callable<Boolean>>();

                for (String identity : identities) {
                    String[] pair = identity.split(",");
                    tasks.add(new Callable<Boolean>() {
                        public Boolean call() {
                            new OutboundCallReminder(callConfiguration).Report(pair[0].trim(), pair[1].trim());
                            return true;
                        }
                    });
                }
                executorService.invokeAll(tasks);
                executorService.shutdown();
            }
        } catch (Exception ex) {
            System.out.printf("Failed to initiate the outbound call Exception: " + ex.getMessage());
        }
        DeleteUser(callConfiguration.ConnectionString, callConfiguration.SourceIdentity);
    }

    /// <summary>
    /// Fetch configurations from App Settings and create source identity
    /// </summary>
    /// <param name="appBaseUrl">The base url of the app.</param>
    /// <returns>The <c CallConfiguration object.</returns>
    private static CallConfiguration InitiateConfiguration(String appBaseUrl) {
        ConfigurationManager configurationManager = ConfigurationManager.GetInstance();
        String connectionString = configurationManager.GetAppSettings("Connectionstring");
        String sourcePhoneNumber = configurationManager.GetAppSettings("SourcePhone");
        String sourceIdentity = CreateUser(connectionString);
        String audioFileName = GenerateCustomAudioMessage();
        return new CallConfiguration(connectionString, sourceIdentity, sourcePhoneNumber, appBaseUrl, audioFileName);
    }

    /// <summary>
    /// Get .wav Audio file
    /// </summary>
    private static String GenerateCustomAudioMessage() {
        ConfigurationManager configurationManager = ConfigurationManager.GetInstance();
        String key = configurationManager.GetAppSettings("CognitiveServiceKey");
        String region = configurationManager.GetAppSettings("CognitiveServiceRegion");
        String customMessage = configurationManager.GetAppSettings("CustomMessage");

        try {
            if (key != null && !key.isEmpty() && region != null && !region.isEmpty() && customMessage != null
                    && !customMessage.isEmpty()) {
                SpeechConfig config = SpeechConfig.fromSubscription(key, region);
                config.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Riff16Khz16BitMonoPcm);

                AudioConfig audioConfig = AudioConfig.fromWavFileInput(
                        "src/main/java/com/communication/outboundcallreminder/audio/custom-message.wav");
                SpeechSynthesizer synthesizer = new SpeechSynthesizer(config, audioConfig);
                synthesizer.SpeakTextAsync(customMessage);
                synthesizer.close();
                return "custom-message.wav";
            }
            return "sample-message.wav";
        } catch (Exception ex) {
            System.out.println("Exception while generating text to speech, falling back to sample audio. Exception: "
                    + ex.getMessage());
            return "sample-message.wav";
        }
    }

    /// <summary>
    /// Create new user
    /// </summary>
    private static String CreateUser(String connectionString) {
        CommunicationIdentityClient client = new CommunicationIdentityClientBuilder().connectionString(connectionString)
                .buildClient();
        CommunicationUserIdentifier user = client.createUser();
        return user.getId();
    }

    /// <summary>
    /// Delete the user
    /// </summary>
    private static void DeleteUser(String connectionString, String source) {
        CommunicationIdentityClient client = new CommunicationIdentityClientBuilder().connectionString(connectionString)
                .buildClient();
        client.deleteUser(new CommunicationUserIdentifier(source));
    }
}
