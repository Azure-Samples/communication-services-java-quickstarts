package com.communication.incomingcallsample.Utils;

import com.azure.communication.callingserver.CallConnection;
import com.azure.communication.callingserver.CallingServerClient;
import com.azure.communication.callingserver.models.AnswerCallOptions;
import com.azure.communication.callingserver.models.CallConnectionState;
import com.azure.communication.callingserver.models.CallMediaType;
import com.azure.communication.callingserver.models.CallingEventSubscriptionType;
import com.azure.communication.callingserver.models.events.CallConnectionStateChangedEvent;
import com.azure.communication.callingserver.models.events.CallingServerEventType;
import com.azure.core.http.HttpHeader;
import com.azure.core.http.rest.Response;
import com.azure.cosmos.implementation.changefeed.CancellationToken;
import com.azure.cosmos.implementation.changefeed.CancellationTokenSource;
import com.communication.incomingcallsample.EventHandler.EventDispatcher;
import com.communication.incomingcallsample.EventHandler.NotificationCallback;
import com.communication.incomingcallsample.Log.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

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
        this.reportCancellationToken = reportCancellationTokenSource.getToken();

        try {
            //answer the call
            answerCall(incomingCallContext);

            // to do: play audio

            // Wait for the call to terminate
            callTerminatedTask.get();

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

    private void answerCall(String incomingCallContext) throws Exception{
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
        callConnection = response.getValue();
        String callConnectionId = callConnection.getCallConnectionId();
        Logger.logMessage(Logger.MessageType.INFORMATION, "AnswerCallAsync Response -----> " + getResponse(response));
        Logger.logMessage(Logger.MessageType.INFORMATION, "AnswerCallAsync call -----> callConnectionId: " + callConnectionId);

        registerToCallStateChangeEvent(callConnectionId);
        callConnectedTask.get();
    }

    private void registerToCallStateChangeEvent(String callConnectionId) {
        callConnectedTask = new CompletableFuture<>();
        callTerminatedTask = new CompletableFuture<>();
        // Set the callback method
        NotificationCallback callStateChangeNotificaiton = (callEvent) -> {
            CallConnectionStateChangedEvent callStateChanged = (CallConnectionStateChangedEvent) callEvent;

            Logger.logMessage(
                Logger.MessageType.INFORMATION,
                "Call State changed to -- > " + callStateChanged.getCallConnectionState() + " callConnectionId: " + callConnectionId);

            if (callStateChanged.getCallConnectionState().equals(CallConnectionState.CONNECTED)) {
                callConnectedTask.complete(true);
            } else if (callStateChanged.getCallConnectionState().equals(CallConnectionState.DISCONNECTED)) {
                EventDispatcher.getInstance()
                        .unsubscribe(CallingServerEventType.CALL_CONNECTION_STATE_CHANGED_EVENT.toString(), callConnectionId);
                reportCancellationTokenSource.cancel();
                callTerminatedTask.complete(true);
            }
        };
        // Subscribe to the event
        EventDispatcher.getInstance().subscribe(CallingServerEventType.CALL_CONNECTION_STATE_CHANGED_EVENT.toString(),
            callConnectionId, callStateChangeNotificaiton);
    }
}
