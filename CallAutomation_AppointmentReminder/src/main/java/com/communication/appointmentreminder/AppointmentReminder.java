package com.communication.appointmentreminder;

import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallConnection;
import com.azure.communication.callautomation.CallMedia;
import com.azure.communication.callautomation.EventHandler;
import com.azure.communication.callautomation.models.CallMediaRecognizeDtmfOptions;
import com.azure.communication.callautomation.models.CallMediaRecognizeOptions;
import com.azure.communication.callautomation.models.CreateCallOptions;
import com.azure.communication.callautomation.models.CreateCallResult;
import com.azure.communication.callautomation.models.DtmfTone;
import com.azure.communication.callautomation.models.FileSource;
import com.azure.communication.callautomation.models.PlayOptions;
import com.azure.communication.callautomation.models.PlaySource;
import com.azure.communication.callautomation.models.events.CallAutomationEventBase;
import com.azure.communication.callautomation.models.events.CallConnected;
import com.azure.communication.callautomation.models.events.PlayCompleted;
import com.azure.communication.callautomation.models.events.RecognizeCompleted;
import com.azure.communication.callautomation.models.events.RecognizeFailed;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.core.http.HttpHeader;
import com.azure.core.http.rest.Response;
import com.communication.appointmentreminder.models.CallAutomationClientConfiguration;
import com.communication.appointmentreminder.utitilities.Constants;
import com.communication.appointmentreminder.utitilities.Identity;
import com.communication.appointmentreminder.utitilities.Logger;
import com.communication.appointmentreminder.utitilities.Speech;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;

@RestController
public class AppointmentReminder {
    private static CallAutomationClientConfiguration callConfiguration;
    private static CallAutomationClient callAutomationClient;

    private static CallConnection callConnection;
    private static String targetPhoneNumber;

    public static void setCConfiguration() {
        String callBackUrl = ConfigurationManager.getInstance().getAppSettings("CallbackUrl");
        callConfiguration = initiateConfiguration(callBackUrl);
        callAutomationClient = new CallAutomationClientBuilder()
                .connectionString(callConfiguration.getConnectionString())
                .buildClient();
    }

    public static void executeReminder(String targetPhoneNumber) {
        try {
            setTargetPhoneNumber(targetPhoneNumber);
            // Preparing request data
            CommunicationUserIdentifier source = new CommunicationUserIdentifier(callConfiguration.getSourceIdentity());
            List<CommunicationIdentifier> targets = Collections.singletonList(new PhoneNumberIdentifier(targetPhoneNumber));

            CreateCallOptions createCallOption = new CreateCallOptions(source, targets, callConfiguration.getAppCallbackUrl());
            createCallOption.setSourceCallerId(callConfiguration.getSourcePhoneNumber());
            Logger.logMessage(Logger.MessageType.INFORMATION,"Performing CreateCall operation");

            Response<CreateCallResult> response = callAutomationClient.createCallWithResponse(createCallOption, null);
            CreateCallResult callResult = response.getValue();

            Logger.logMessage(
                    Logger.MessageType.INFORMATION,
                    "createCallConnectionWithResponse -- > " + getResponse(response) + ", Call connection ID: " +
                            callResult.getCallConnectionProperties().getCallConnectionId()
            );
            Logger.logMessage(
                    Logger.MessageType.INFORMATION,
                    "Call initiated with Call Leg id -- >" +
                            callResult.getCallConnectionProperties().getCallConnectionId());

            callConnection = callResult.getCallConnection();
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR, "Failure occured while creating/establishing the call. Exception -- >" + ex.getMessage());
        }
    }

    public static String getResponse(Response<?> response)
    {
        StringBuilder responseString;
        responseString = new StringBuilder("StatusCode: " + response.getStatusCode() + ", Headers: { ");

        for (HttpHeader header : response.getHeaders()) {
            responseString.append(header.getName()).append(":").append(header.getValue()).append(", ");
        }
        responseString.append("} ");
        return responseString.toString();
    }

    @RequestMapping("/api/outboundcall/callback")
    public static void handleIncomingEvents(@RequestBody(required = false) String event) {
        CallAutomationEventBase callEvent = EventHandler.parseEvent(event);
        CallMedia callMedia = callConnection.getCallMedia();
        if(callEvent instanceof CallConnected) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Call successfully connected");
            PlaySource reminderMessage = new FileSource()
                    .setUri(callConfiguration.getAppBaseUrl() + "/audio/" + Speech.getReminderMessage())
                    .setPlaySourceId("ReminderMessage");
            CallMediaRecognizeOptions recognizeOptions = new CallMediaRecognizeDtmfOptions(
                    new PhoneNumberIdentifier(targetPhoneNumber),
                    1
            )
                    .setPlayPrompt(reminderMessage)
                    .setOperationContext("ReminderMenu");
            Response<?> response = callMedia.startRecognizingWithResponse(recognizeOptions, null);
            Logger.logMessage(
                    Logger.MessageType.INFORMATION,
                    "startRecognizingWithResponse -- > " + getResponse(response)
            );
        } else if (callEvent instanceof RecognizeCompleted && callEvent.getOperationContext().equals("ReminderMenu")) {
            RecognizeCompleted recognizeCompleted = (RecognizeCompleted)callEvent;
            DtmfTone tone = recognizeCompleted.getCollectTonesResult().getTones().get(0);
            Logger.logMessage(Logger.MessageType.INFORMATION, "DTMF tones received: " + tone);
            PlaySource playSource;
            if (tone == DtmfTone.ONE) {
                playSource = new FileSource()
                        .setUri(callConfiguration.getAppBaseUrl() + "/audio/" + Speech.getConfirmationMessage())
                        .setPlaySourceId("ConfirmationMessage");
            } else {
                playSource = new FileSource()
                        .setUri(callConfiguration.getAppBaseUrl() + "/audio/" + Speech.getCancellationMessage())
                        .setPlaySourceId("CancellationMessage");
            }
            Response<?> response = callMedia.playToAllWithResponse(playSource, new PlayOptions(), null);
            Logger.logMessage(
                    Logger.MessageType.INFORMATION,
                    "PlayToAllWithResponse -- > " + getResponse(response)
            );
        } else if (callEvent instanceof RecognizeFailed && callEvent.getOperationContext().equals("ReminderMenu")) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Recognize timed out");
            PlaySource playSource = new FileSource()
                        .setUri(callConfiguration.getAppBaseUrl() + "/audio/" + Speech.getNoInputMessage())
                        .setPlaySourceId("NoInputMessage");
            Response<?> response = callMedia.playToAllWithResponse(playSource, new PlayOptions(), null);
            Logger.logMessage(
                    Logger.MessageType.INFORMATION,
                    "PlayToAllWithResponse -- > " + getResponse(response)
            );
        } else if (callEvent instanceof PlayCompleted) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Play completed! Hanging up the call");
            callConnection.hangUp(true);
        }
    }

    @RequestMapping("/audio/{fileName}")
    public ResponseEntity<Object> loadFile(@PathVariable(value = "fileName", required = false) String fileName) {
        String filePath = Constants.AUDIO_FILES_ROUTE + fileName;
        File file = new File(filePath);
        InputStreamResource resource = null;

        try {
            resource = new InputStreamResource(new FileInputStream(file));
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");

        return ResponseEntity.ok().headers(headers).contentLength(file.length())
                .contentType(MediaType.parseMediaType("audio/x-wav")).body(resource);
    }

    /// <summary>
    /// Fetch configurations from App Settings and create source identity
    /// </summary>
    /// <param name="appBaseUrl">The base url of the app.</param>
    /// <returns>The <c CallConfiguration object.</returns>
    private static CallAutomationClientConfiguration initiateConfiguration(String appBaseUrl) {
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        String connectionString = configurationManager.getAppSettings("Connectionstring");
        String sourcePhoneNumber = configurationManager.getAppSettings("SourcePhone");
        String sourceIdentity = Identity.createUser(connectionString);
        return new CallAutomationClientConfiguration(connectionString, sourceIdentity, sourcePhoneNumber, appBaseUrl);
    }

    public static void setTargetPhoneNumber(String targetPhoneNumber) {
        AppointmentReminder.targetPhoneNumber = targetPhoneNumber;
    }
}
