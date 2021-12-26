package com.communication.incomingcallsample.Utils;

import com.azure.communication.callingserver.CallConnection;
import com.azure.communication.callingserver.CallingServerClient;
import com.azure.communication.callingserver.models.AnswerCallOptions;
import com.azure.communication.callingserver.models.CallConnectionState;
import com.azure.communication.callingserver.models.CallMediaType;
import com.azure.communication.callingserver.models.CallingEventSubscriptionType;
import com.azure.communication.callingserver.models.CallingOperationStatus;
import com.azure.communication.callingserver.models.PlayAudioOptions;
import com.azure.communication.callingserver.models.PlayAudioResult;
import com.azure.communication.callingserver.models.events.CallConnectionStateChangedEvent;
import com.azure.communication.callingserver.models.events.CallingServerEventType;
import com.azure.communication.callingserver.models.events.PlayAudioResultEvent;
import com.azure.core.http.HttpHeader;
import com.azure.core.http.rest.Response;
import com.azure.cosmos.implementation.changefeed.CancellationToken;
import com.azure.cosmos.implementation.changefeed.CancellationTokenSource;
import com.communication.incomingcallsample.EventHandler.EventDispatcher;
import com.communication.incomingcallsample.EventHandler.NotificationCallback;
import com.communication.incomingcallsample.Log.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class IncomingCallHandler {
    private final CallingServerClient callingServerClient;
    private final CallConfiguration callConfiguration;
    private CallConnection callConnection = null;
    private CancellationTokenSource reportCancellationTokenSource;
    private CancellationToken reportCancellationToken;
    private String targetParticipant;

    private CompletableFuture<Boolean> callConnectedTask;
    private CompletableFuture<Boolean> playAudioCompletedTask;
    private CompletableFuture<Boolean> callTerminatedTask;
    private CompletableFuture<Boolean> toneReceivedCompleteTask;
    private CompletableFuture<Boolean> addParticipantCompleteTask;

    public IncomingCallHandler(CallingServerClient callingServerClient, CallConfiguration callConfiguration) {
        this.callingServerClient = callingServerClient;
        this.callConfiguration = callConfiguration;
    }

    public void report(String incomingCallContext){
        this.reportCancellationTokenSource = new CancellationTokenSource();
        this.reportCancellationToken = this.reportCancellationTokenSource.getToken();

        try {
            //answer the call
            String callConnectionId = answerCall(incomingCallContext);
            registerToCallStateChangeEvent(callConnectionId);
            this.callConnectedTask.get();

            // to do: registerToDtmfResultEvent

            // to do: play audio
            playAudio();
            Boolean playAudioCompleted = this.playAudioCompletedTask.get();

            // Wait for the call to terminate
            this.callTerminatedTask.get();

            Logger.logMessage(Logger.MessageType.INFORMATION, "call terminated.");
        } catch (Exception ex)
        {
            Logger.logMessage(Logger.MessageType.ERROR, "Call ended unexpectedly, reason: " + ex.getMessage());
        }
    }

    private String getResponse(Response<?> response)
    {
        StringBuilder responseString;
        responseString = new StringBuilder("StatusCode: " + response.getStatusCode() + ", Headers: { ");

        for (HttpHeader header : response.getHeaders()) {
            responseString.append(header.getName()).append(":").append(header.getValue()).append(", ");
        }
        responseString.append("} ");
        return responseString.toString();
    }

    private String answerCall(String incomingCallContext) throws Exception{
        //answer the call
        AnswerCallOptions answerCallOptions = new AnswerCallOptions(
            new URI(this.callConfiguration.appCallbackUrl),
            new ArrayList<CallMediaType>() {
                {
                    add(CallMediaType.AUDIO);
                }
            },
            new ArrayList<CallingEventSubscriptionType>() {
                {
                    add(CallingEventSubscriptionType.PARTICIPANTS_UPDATED);
                    add(CallingEventSubscriptionType.TONE_RECEIVED);
                }
            }
        );
        Logger.logMessage(Logger.MessageType.INFORMATION, "Answering call...");
        Response<CallConnection> response = this.callingServerClient.answerCallWithResponse(incomingCallContext, answerCallOptions, null);
        this.callConnection = response.getValue();
        String callConnectionId = this.callConnection.getCallConnectionId();
        Logger.logMessage(Logger.MessageType.INFORMATION, "AnswerCallAsync Response -----> " + getResponse(response));
        Logger.logMessage(Logger.MessageType.INFORMATION, "AnswerCallAsync call -----> callConnectionId: " + callConnectionId);

        return callConnectionId;
    }

    private void registerToCallStateChangeEvent(String callConnectionId) {
        this.callConnectedTask = new CompletableFuture<>();
        this.callTerminatedTask = new CompletableFuture<>();
        // Set the callback method
        NotificationCallback callStateChangeNotificaiton = (callEvent) -> {
            CallConnectionStateChangedEvent callStateChanged = (CallConnectionStateChangedEvent) callEvent;

            Logger.logMessage(
                Logger.MessageType.INFORMATION,
                "Call State changed to -- > " + callStateChanged.getCallConnectionState() + " callConnectionId: " + callConnectionId);

            if (callStateChanged.getCallConnectionState().equals(CallConnectionState.CONNECTED)) {
                this.callConnectedTask.complete(true);
            } else if (callStateChanged.getCallConnectionState().equals(CallConnectionState.DISCONNECTED)) {
                EventDispatcher.getInstance()
                        .unsubscribe(CallingServerEventType.CALL_CONNECTION_STATE_CHANGED_EVENT.toString(), callConnectionId);
                this.reportCancellationTokenSource.cancel();
                this.callTerminatedTask.complete(true);
            }
        };
        // Subscribe to the event
        EventDispatcher.getInstance().subscribe(CallingServerEventType.CALL_CONNECTION_STATE_CHANGED_EVENT.toString(),
            callConnectionId, callStateChangeNotificaiton);
    }

    private void playAudio(){
        if (this.reportCancellationToken.isCancellationRequested()) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Cancellation request, PlayAudio will not be performed");
            return;
        }

        try{
            PlayAudioOptions playAudioOptions = new PlayAudioOptions();
            playAudioOptions.setLoop(true);
            playAudioOptions.setAudioFileId(UUID.randomUUID().toString());
            playAudioOptions.setOperationContext(UUID.randomUUID().toString());
            Logger.logMessage(Logger.MessageType.INFORMATION, "Performing PlayAudio operation");
            Response<PlayAudioResult> playAudioResponse = this.callConnection.playAudioWithResponse(new URI(this.callConfiguration.audioFileUrl), playAudioOptions, null);
            PlayAudioResult response = playAudioResponse.getValue();
            Logger.logMessage(Logger.MessageType.INFORMATION, "playAudioWithResponse -- > " + getResponse(playAudioResponse) +
                ", Id: " + response.getOperationId() + ", OperationContext: " + response.getOperationContext() + ", OperationStatus: " +
                response.getStatus().toString());

            if(response.getStatus().equals(CallingOperationStatus.RUNNING)) {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Audio is playing, registering PlayAudioResultEvent");

               // listen to play audio events
               registerToPlayAudioResultEvent(response.getOperationContext());

               try {
                    Logger.logMessage(Logger.MessageType.INFORMATION, "Waiting for playAudioCompletedTask or timeout");
                    playAudioCompletedTask.get(30, TimeUnit.SECONDS);
                    Logger.logMessage(Logger.MessageType.INFORMATION, "Done playAudioCompletedTask ");
               } catch (TimeoutException e) {
                Logger.logMessage(Logger.MessageType.INFORMATION, "No response from user in 30 sec, initiating hangup");
                playAudioCompletedTask.complete(false);
                //to do toneReceivedCompleteTask.complete(false);
               }
            }
        } catch (CancellationException e) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Play audio operation cancelled");
        } catch (Exception ex) {
            if (playAudioCompletedTask.isCancelled()) {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Play audio operation cancelled");
            } else {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Failure occurred while playing audio on the call. Exception: " + ex.getMessage());
            }
        }
    }

    private void registerToPlayAudioResultEvent(String operationContext) {
        playAudioCompletedTask = new CompletableFuture<>();
        NotificationCallback playPromptResponseNotification = ((callEvent) -> {
            PlayAudioResultEvent playAudioResultEvent = (PlayAudioResultEvent) callEvent;
            Logger.logMessage(Logger.MessageType.INFORMATION, "Play audio status -- > " + playAudioResultEvent.getStatus());

            if (playAudioResultEvent.getStatus().equals(CallingOperationStatus.COMPLETED)) {
                EventDispatcher.getInstance().unsubscribe(CallingServerEventType.PLAY_AUDIO_RESULT_EVENT.toString(),
                        operationContext);
                playAudioCompletedTask.complete(true);
            } else if (playAudioResultEvent.getStatus().equals(CallingOperationStatus.FAILED)) {
                playAudioCompletedTask.complete(false);
            }
        });

        // Subscribe to event
        EventDispatcher.getInstance().subscribe(CallingServerEventType.PLAY_AUDIO_RESULT_EVENT.toString(),
                operationContext, playPromptResponseNotification);
    }
}
