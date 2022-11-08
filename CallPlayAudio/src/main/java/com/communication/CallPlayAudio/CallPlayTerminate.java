package com.communication.CallPlayAudio;

import com.azure.communication.callautomation.CallConnection;
import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.models.CreateCallOptions;
import com.azure.communication.callautomation.models.CreateCallResult;
import com.azure.communication.callautomation.models.FileSource;
import com.azure.communication.callautomation.models.HangUpOptions;
import com.azure.communication.callautomation.models.PlayOptions;
import com.azure.communication.callautomation.models.PlaySource;
import com.azure.communication.callautomation.models.events.CallConnected;
import com.azure.communication.callautomation.models.events.CallDisconnected;
import com.azure.communication.callautomation.models.events.PlayCanceled;
import com.azure.communication.callautomation.models.events.PlayCompleted;
import com.azure.communication.callautomation.models.events.PlayFailed;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.cosmos.implementation.changefeed.CancellationToken;
import com.azure.cosmos.implementation.changefeed.CancellationTokenSource;
import com.communication.CallPlayAudio.EventHandler.EventDispatcher;
import com.communication.CallPlayAudio.EventHandler.NotificationCallback;
import com.azure.core.http.HttpHeader;
import com.azure.core.http.rest.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CallPlayTerminate {

    private final CallConfiguration callConfiguration;
    private final CallAutomationClient callAutomationClient;
    private CallConnection callConnection = null;
    private CancellationTokenSource reportCancellationTokenSource;
    private CancellationToken reportCancellationToken;
    private CompletableFuture<Boolean> callConnectedTask;
    private CompletableFuture<Boolean> playAudioCompletedTask;
    private CompletableFuture<Boolean> callTerminatedTask;

    public CallPlayTerminate(CallConfiguration callConfiguration) {
        this.callConfiguration = callConfiguration;
        this.callAutomationClient = new CallAutomationClientBuilder().connectionString(this.callConfiguration.connectionString)
        .buildClient();
    }

    public void report(String targetPhoneNumber) {
        reportCancellationTokenSource = new CancellationTokenSource();
        reportCancellationToken = reportCancellationTokenSource.getToken();

        try {
            createCallAsync(targetPhoneNumber);

            registerToPlayAudioResultEvent(this.callConnection.getCallProperties().getCallConnectionId());

            playAudioAsync();

            // Wait for audio play
            playAudioCompletedTask.get();

            // Hang up the call
            hangupAsync();

            // Wait for the call to terminate
            callTerminatedTask.get();
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR, "Call ended unexpectedly, reason -- > " + ex.getMessage());
        }
    }

    private void createCallAsync(String targetPhoneNumber) {
        try {
            // Preparing request data
            CommunicationUserIdentifier source = new CommunicationUserIdentifier(this.callConfiguration.sourceIdentity);
            List<CommunicationIdentifier> targets = new ArrayList<CommunicationIdentifier>() {
                {
                    add(new PhoneNumberIdentifier(targetPhoneNumber));
                }
            };

            CreateCallOptions createCallOption = new CreateCallOptions(source, targets,
                    this.callConfiguration.appCallbackUrl);
            createCallOption.setSourceCallerId(this.callConfiguration.sourcePhoneNumber);
            Logger.logMessage(Logger.MessageType.INFORMATION, "Performing CreateCall operation");

            Response<CreateCallResult> response = this.callAutomationClient.createCallWithResponse(createCallOption,
                    null);
            CreateCallResult callResult = response.getValue();
            callConnection = callResult.getCallConnection();

            Logger.logMessage(Logger.MessageType.INFORMATION,
                    "createCallConnectionWithResponse -- > " + getResponse(response) + ", Call connection ID: "
                            + callResult.getCallConnectionProperties().getCallConnectionId());
            Logger.logMessage(Logger.MessageType.INFORMATION, "Call initiated with Call Leg id -- >"
                    + callResult.getCallConnectionProperties().getCallConnectionId());

            registerToCallStateChangeEvent(callResult.getCallConnectionProperties().getCallConnectionId());
            callConnectedTask.get();

        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR,
                    "Failure occured while creating/establishing the call. Exception -- >" + ex.getMessage());
        }
    }

    private void registerToCallStateChangeEvent(String callLegId) {
        callTerminatedTask = new CompletableFuture<>();
        callConnectedTask = new CompletableFuture<>();
        // Set the callback method
        NotificationCallback callConnectedNotificaiton = ((callEvent) -> {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Call State successfully connected");
            callConnectedTask.complete(true);
            EventDispatcher.getInstance().unsubscribe(CallConnected.class.getName(), callLegId);
        });

        NotificationCallback callDisconnectedNotificaiton = ((callEvent) -> {
            EventDispatcher.getInstance().unsubscribe(CallDisconnected.class.getName(), callLegId);
            reportCancellationTokenSource.cancel();
            callTerminatedTask.complete(true);
        });

        // Subscribe to the event
        EventDispatcher.getInstance().subscribe(CallConnected.class.getName(), callLegId, callConnectedNotificaiton);
        EventDispatcher.getInstance().subscribe(CallDisconnected.class.getName(), callLegId, callDisconnectedNotificaiton);
    }

    private void playAudioAsync() {
        if (reportCancellationToken.isCancellationRequested()) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Cancellation request, PlayAudio will not be performed");
            return;
        }

        try {
            // Preparing data for request
            String audioFileUri = this.callConfiguration.audioFileUrl;
            PlaySource playSource = new FileSource().setUri(audioFileUri);
            PlayOptions playAudioOptions = new PlayOptions();
            playAudioOptions.setLoop(false);
            playAudioOptions.setOperationContext(UUID.randomUUID().toString());
            
            Logger.logMessage(Logger.MessageType.INFORMATION, "Performing Play Audio operation");
            Response<Void> playAudioResponse = this.callConnection.getCallMedia().
            playToAllWithResponse(playSource, playAudioOptions, null);
            
            Logger.logMessage(Logger.MessageType.INFORMATION, "playAudioWithResponse -- > " + getResponse(playAudioResponse));

            if (playAudioResponse.getStatusCode() == 202) {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Play Audio state is running ");
            }
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Failure occured while playing audio on the call. Exception: " + ex.getMessage());
        }
    }

    private void hangupAsync() {
        if (reportCancellationToken.isCancellationRequested()) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Cancellation request, Hangup will not be performed");
            return;
        }

        Logger.logMessage(Logger.MessageType.INFORMATION, "Performing Hangup operation");

        HangUpOptions hangUpOptions = new HangUpOptions(true);
        Response<Void> response = this.callConnection.hangUpWithResponse(hangUpOptions, null);
        Logger.logMessage(Logger.MessageType.INFORMATION, "hangupWithResponse -- > " + getResponse(response));
    }

    public final String userIdentityRegex = "8:acs:[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}_[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}";
    public final String phoneIdentityRegex = "^\\+\\d{10,14}$";

    public String getResponse(Response<?> response)
    {
        StringBuilder responseString;
        responseString = new StringBuilder("StatusCode: " + response.getStatusCode() + ", Headers: { ");

        for (HttpHeader header : response.getHeaders()) {
            responseString.append(header.getName()).append(":").append(header.getValue()).append(", ");
        }
        responseString.append("} ");
        return responseString.toString();
    }

    private void registerToPlayAudioResultEvent(String callConnectionId) {
        playAudioCompletedTask = new CompletableFuture<>();
        NotificationCallback playCompleted = ((callEvent) -> {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Play audio status completed");

            EventDispatcher.getInstance().unsubscribe(PlayCompleted.class.getName(), callConnectionId);
            playAudioCompletedTask.complete(true);
        });

        NotificationCallback playFailed = ((callEvent) -> {
            EventDispatcher.getInstance().unsubscribe(PlayFailed.class.getName(), callConnectionId);
            reportCancellationTokenSource.cancel();
            playAudioCompletedTask.complete(false);
        });

        NotificationCallback playCanceled = ((callEvent) -> {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Play audio status Canceled");
            EventDispatcher.getInstance().unsubscribe(PlayCanceled.class.getName(), callConnectionId);
            reportCancellationTokenSource.cancel();
            playAudioCompletedTask.complete(false);
        });

        // Subscribe to event
        EventDispatcher.getInstance().subscribe(PlayCompleted.class.getName(), callConnectionId, playCompleted);
        EventDispatcher.getInstance().subscribe(PlayFailed.class.getName(), callConnectionId, playFailed);
        EventDispatcher.getInstance().subscribe(PlayCanceled.class.getName(), callConnectionId, playCanceled);
    }
}