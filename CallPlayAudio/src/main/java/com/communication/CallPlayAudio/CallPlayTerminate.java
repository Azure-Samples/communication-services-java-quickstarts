package com.communication.CallPlayAudio;

import com.azure.communication.callingserver.CallConnection;
import com.azure.communication.callingserver.CallingServerClient;
import com.azure.communication.callingserver.CallingServerClientBuilder;
import com.azure.communication.callingserver.models.CallConnectionState;
import com.azure.communication.callingserver.models.CreateCallOptions;
import com.azure.communication.callingserver.models.EventSubscriptionType;
import com.azure.communication.callingserver.models.MediaType;
import com.azure.communication.callingserver.models.OperationStatus;
import com.azure.communication.callingserver.models.PlayAudioOptions;
import com.azure.communication.callingserver.models.PlayAudioResult;
import com.azure.communication.callingserver.models.events.CallConnectionStateChangedEvent;
import com.azure.communication.callingserver.models.events.CallingServerEventType;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
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
import java.util.concurrent.TimeUnit;

public class CallPlayTerminate {

    private final CallConfiguration callConfiguration;
    private final CallingServerClient callingServerClient;
    private CallConnection callConnection = null;
    private CancellationTokenSource reportCancellationTokenSource;
    private CancellationToken reportCancellationToken;
    private CompletableFuture<Boolean> callConnectedTask;
    private CompletableFuture<Boolean> callTerminatedTask;

    public CallPlayTerminate(CallConfiguration callConfiguration) {
        this.callConfiguration = callConfiguration;

        NettyAsyncHttpClientBuilder httpClientBuilder = new NettyAsyncHttpClientBuilder();
        CallingServerClientBuilder callClientBuilder = new CallingServerClientBuilder().httpClient(httpClientBuilder.build())
                .connectionString(this.callConfiguration.connectionString);

        this.callingServerClient = callClientBuilder.buildClient();
    }

    public void report(String targetPhoneNumber) {
        reportCancellationTokenSource = new CancellationTokenSource();
        reportCancellationToken = reportCancellationTokenSource.getToken();

        try {
            createCallAsync(targetPhoneNumber);
            playAudioAsync();
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
            PhoneNumberIdentifier target = new PhoneNumberIdentifier(targetPhoneNumber);

            List<MediaType> callModality = new ArrayList<>() {
                {
                    add(MediaType.AUDIO);
                }
            };

            List<EventSubscriptionType> eventSubscriptionType = new ArrayList<>() {
                {
                    add(EventSubscriptionType.PARTICIPANTS_UPDATED);
                    add(EventSubscriptionType.DTMF_RECEIVED);
                }
            };

            CreateCallOptions createCallOption = new CreateCallOptions(this.callConfiguration.appCallbackUrl,
                    callModality, eventSubscriptionType);

            createCallOption.setAlternateCallerId(new PhoneNumberIdentifier(this.callConfiguration.sourcePhoneNumber));

            Logger.logMessage(Logger.MessageType.INFORMATION,"Performing CreateCall operation");

            List<CommunicationIdentifier> targets = new ArrayList<>() {
                {
                    add(target);
                }
            };

            Logger.logMessage(Logger.MessageType.INFORMATION,"this.callConfiguration.sourcePhoneNumber"+this.callConfiguration.sourcePhoneNumber);
            Logger.logMessage(Logger.MessageType.INFORMATION,"targetPhoneNumber"+targetPhoneNumber);
            
            Response<CallConnection> response = this.callingServerClient.createCallConnectionWithResponse(source, targets, createCallOption, null);
            callConnection = response.getValue(); 

            Logger.logMessage(Logger.MessageType.INFORMATION, "createCallConnectionWithResponse -- > " + getResponse(response) + ", Call connection ID: " + callConnection.getCallConnectionId());
            Logger.logMessage(Logger.MessageType.INFORMATION, "Call initiated with Call Leg id -- >" + callConnection.getCallConnectionId());

            registerToCallStateChangeEvent(callConnection.getCallConnectionId());
            callConnectedTask.get();

        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR, "Failure occured while creating/establishing the call. Exception -- >" + ex.getMessage());
        }
    }

    private void registerToCallStateChangeEvent(String callLegId) {
        callTerminatedTask = new CompletableFuture<>();
        callConnectedTask = new CompletableFuture<>();
        // Set the callback method
        NotificationCallback callStateChangeNotificaiton = ((callEvent) -> {
            CallConnectionStateChangedEvent callStateChanged = (CallConnectionStateChangedEvent) callEvent;

            Logger.logMessage(Logger.MessageType.INFORMATION,"Call State changed to -- > " + callStateChanged.getCallConnectionState());

            if (callStateChanged.getCallConnectionState().equals(CallConnectionState.CONNECTED)) {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Call State successfully connected");
                callConnectedTask.complete(true);
            } else if (callStateChanged.getCallConnectionState().equals(CallConnectionState.DISCONNECTED)) {
                EventDispatcher.getInstance()
                        .unsubscribe(CallingServerEventType.CALL_CONNECTION_STATE_CHANGED_EVENT.toString(), callLegId);
                reportCancellationTokenSource.cancel();
                callTerminatedTask.complete(true);
            }
        });

        // Subscribe to the event
        EventDispatcher.getInstance().subscribe(CallingServerEventType.CALL_CONNECTION_STATE_CHANGED_EVENT.toString(),
                callLegId, callStateChangeNotificaiton);
    }

    private void playAudioAsync() {
        if (reportCancellationToken.isCancellationRequested()) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Cancellation request, PlayAudio will not be performed");
            return;
        }

        try {
            // Preparing data for request
            String audioFileUri = callConfiguration.audioFileUrl;
            Boolean loop = true;
            String operationContext = UUID.randomUUID().toString();
            String audioFileId = UUID.randomUUID().toString();
            PlayAudioOptions playAudioOptions = new PlayAudioOptions();
            playAudioOptions.setLoop(loop);
            playAudioOptions.setAudioFileId(audioFileId);
            playAudioOptions.setOperationContext(operationContext);

            Logger.logMessage(Logger.MessageType.INFORMATION, "Performing PlayAudio operation");
            Response<PlayAudioResult> playAudioResponse = this.callConnection.playAudioWithResponse(audioFileUri, playAudioOptions, null);
            
            PlayAudioResult response = playAudioResponse.getValue();

            Logger.logMessage(Logger.MessageType.INFORMATION, "playAudioWithResponse -- > " + getResponse(playAudioResponse) + 
            ", Id: " + response.getOperationId() + ", OperationContext: " + response.getOperationContext() + ", OperationStatus: " +
            response.getStatus().toString());

            if (response.getStatus().equals(OperationStatus.RUNNING)) {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Play Audio state -- > " + OperationStatus.RUNNING);
                TimeUnit.SECONDS.sleep(20);
                hangupAsync();
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
        Response<Void> response = this.callConnection.hangupWithResponse(null);
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
}