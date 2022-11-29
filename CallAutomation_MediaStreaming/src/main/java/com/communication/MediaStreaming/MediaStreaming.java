package com.communication.MediaStreaming;

import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.models.AnswerCallOptions;
import com.azure.communication.callautomation.models.AnswerCallResult;
import com.azure.communication.callautomation.models.MediaStreamingAudioChannel;
import com.azure.communication.callautomation.models.MediaStreamingContent;
import com.azure.communication.callautomation.models.MediaStreamingOptions;
import com.azure.communication.callautomation.models.MediaStreamingTransport;
import com.azure.communication.callautomation.models.events.CallConnectedEvent;
import com.azure.communication.callautomation.models.events.CallDisconnectedEvent;

import com.azure.cosmos.implementation.changefeed.CancellationTokenSource;
import com.communication.MediaStreaming.EventHandler.EventDispatcher;
import com.communication.MediaStreaming.EventHandler.NotificationCallback;
import com.azure.core.http.HttpHeader;
import com.azure.core.http.rest.Response;

import java.util.concurrent.CompletableFuture;

public class MediaStreaming {

    private final CallConfiguration callConfiguration;
    private final CallAutomationClient callAutomationClient;
    private CancellationTokenSource reportCancellationTokenSource;
    private CompletableFuture<Boolean> callConnectedTask;
    private CompletableFuture<Boolean> callTerminatedTask;

    public MediaStreaming(CallConfiguration callConfiguration) {
        this.callConfiguration = callConfiguration;
        this.callAutomationClient = new CallAutomationClientBuilder().connectionString(this.callConfiguration.connectionString)
        .buildClient();
    }

    public void report(String incomingCallContext) {
        reportCancellationTokenSource = new CancellationTokenSource();

        try {
            AnswerCallOptions answerCallOptions = new AnswerCallOptions(incomingCallContext, this.callConfiguration.appCallbackUrl);
            
            MediaStreamingOptions mediaStreamingOptions = new MediaStreamingOptions(this.callConfiguration.mediaStreamingTransportURI, 
                MediaStreamingTransport.WEBSOCKET, MediaStreamingContent.AUDIO, MediaStreamingAudioChannel.UNMIXED);
            answerCallOptions.setMediaStreamingConfiguration(mediaStreamingOptions);

            Response<AnswerCallResult> response = this.callAutomationClient.answerCallWithResponse(answerCallOptions, null);
            AnswerCallResult answerCallResult = response.getValue();

            Logger.logMessage(Logger.MessageType.INFORMATION, "AnswerCallWithResponse -- > " + getResponse(response));

            registerToCallStateChangeEvent(answerCallResult.getCallConnectionProperties().getCallConnectionId());
            //Wait for the call to get connected
            callConnectedTask.get();
            // Wait for the call to terminate
            callTerminatedTask.get();
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR, "Call ended unexpectedly, reason -- > " + ex.getMessage());
        }
    }

    private void registerToCallStateChangeEvent(String callLegId) {
        callTerminatedTask = new CompletableFuture<>();
        callConnectedTask = new CompletableFuture<>();
        // Set the callback method
        NotificationCallback callConnectedNotificaiton = ((callEvent) -> {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Call State successfully connected");
            callConnectedTask.complete(true);
            EventDispatcher.getInstance().unsubscribe(CallConnectedEvent.class.getName(), callLegId);
        });

        NotificationCallback callDisconnectedNotificaiton = ((callEvent) -> {
            EventDispatcher.getInstance().unsubscribe(CallDisconnectedEvent.class.getName(), callLegId);
            reportCancellationTokenSource.cancel();
            callTerminatedTask.complete(true);
        });

        // Subscribe to the event
        EventDispatcher.getInstance().subscribe(CallConnectedEvent.class.getName(), callLegId, callConnectedNotificaiton);
        EventDispatcher.getInstance().subscribe(CallDisconnectedEvent.class.getName(), callLegId, callDisconnectedNotificaiton);
    }

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