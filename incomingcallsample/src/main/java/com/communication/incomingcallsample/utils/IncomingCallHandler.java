package com.communication.incomingcallsample.Utils;

import com.azure.communication.callingserver.CallConnection;
import com.azure.communication.callingserver.CallingServerClient;
import com.azure.communication.callingserver.models.AnswerCallOptions;
import com.azure.communication.callingserver.models.CallMediaType;
import com.azure.communication.callingserver.models.CallingEventSubscriptionType;
import com.azure.core.http.HttpHeader;
import com.azure.core.http.rest.Response;
import com.azure.cosmos.implementation.changefeed.CancellationToken;
import com.azure.cosmos.implementation.changefeed.CancellationTokenSource;
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
            Logger.logMessage(Logger.MessageType.INFORMATION, "AnswerCallAsync call -----> callConnectionId: " + callConnection.getCallConnectionId());
            Logger.logMessage(Logger.MessageType.INFORMATION, "AnswerCallAsync Response -----> " + getResponse(response));
            // to do: registerToCallStateChangeEvent(callConnection.getCallConnectionId());
            callConnectedTask.get();

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
}
