package com.communication.incomingcallsample.utils;

import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import com.azure.communication.callingserver.CallConnection;
import com.azure.communication.callingserver.CallingServerClient;
import com.azure.communication.callingserver.models.AnswerCallOptions;
import com.azure.communication.callingserver.models.CallConnectionState;
import com.azure.communication.callingserver.models.CallMediaType;
import com.azure.communication.callingserver.models.CallingEventSubscriptionType;
import com.azure.communication.callingserver.models.CallingOperationStatus;
import com.azure.communication.callingserver.models.PlayAudioOptions;
import com.azure.communication.callingserver.models.PlayAudioResult;
import com.azure.communication.callingserver.models.ToneInfo;
import com.azure.communication.callingserver.models.ToneValue;
import com.azure.communication.callingserver.models.TransferCallResult;
import com.azure.communication.callingserver.models.events.CallConnectionStateChangedEvent;
import com.azure.communication.callingserver.models.events.CallingServerEventType;
import com.azure.communication.callingserver.models.events.PlayAudioResultEvent;
import com.azure.communication.callingserver.models.events.ToneReceivedEvent;
import com.azure.communication.callingserver.models.events.TransferCallResultEvent;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.core.http.HttpHeader;
import com.azure.core.http.rest.Response;
import com.azure.cosmos.implementation.changefeed.CancellationToken;
import com.azure.cosmos.implementation.changefeed.CancellationTokenSource;
import com.communication.incomingcallsample.EventHandler.EventDispatcher;
import com.communication.incomingcallsample.EventHandler.NotificationCallback;
import com.communication.incomingcallsample.Log.Logger;

public class IncomingCallHandler {
    private final String userIdentityRegex = "8:acs:[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}_[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}";
    private final String phoneIdentityRegex = "^\\+\\d{10,14}$";
    private final int MaxRetryAttemptCount = 3;

    private final CallingServerClient callingServerClient;
    private final CallConfiguration callConfiguration;
    private CallConnection callConnection;
    private CancellationTokenSource reportCancellationTokenSource;
    private CancellationToken reportCancellationToken;
    private String targetParticipant;

    private CompletableFuture<Boolean> callConnectedTask;
    private CompletableFuture<Boolean> playAudioCompletedTask;
    private CompletableFuture<Boolean> callTerminatedTask;
    private CompletableFuture<Boolean> toneReceivedCompleteTask;
    private CompletableFuture<Boolean> transferToParticipantCompleteTask;

    public IncomingCallHandler(CallingServerClient callingServerClient, CallConfiguration callConfiguration) {
        this.callingServerClient = callingServerClient;
        this.callConfiguration = callConfiguration;
        this.callConnection = null;
        this.reportCancellationTokenSource = new CancellationTokenSource();
        this.reportCancellationToken = this.reportCancellationTokenSource.getToken();
        this.targetParticipant = this.callConfiguration.targetParticipant;

        this.callConnectedTask = new CompletableFuture<>();
        this.playAudioCompletedTask = new CompletableFuture<>();
        this.callTerminatedTask = new CompletableFuture<>();
        this.toneReceivedCompleteTask = new CompletableFuture<>();
        this.transferToParticipantCompleteTask = new CompletableFuture<>();
    }

    public void report(String incomingCallContext){
        try {
            // answer the call
            String callConnectionId = answerCall(incomingCallContext);
            registerToCallStateChangeEvent(callConnectionId);
            // wait for the call to get connected
            this.callConnectedTask.get();

            registerToDtmfResultEvent(callConnectionId);

            // play audio
            playAudio();
            // wait for audio play complete
            this.playAudioCompletedTask.get();

            // No ToneReceived event fired, and since audio playing is done, drop the call
            if(!this.toneReceivedCompleteTask.isDone()){
                Logger.logMessage(Logger.MessageType.INFORMATION, "dtmf tone not sent from caller, dropped the call");
                hangup();
            }

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
               // listen to play audio events
               registerToPlayAudioResultEvent(response.getOperationContext());
               try {
                   Logger.logMessage(Logger.MessageType.INFORMATION, "Audio is playing for 30 seconds, it can be interrupted by pressing 1 to transfer the call");
                   this.playAudioCompletedTask.get(30, TimeUnit.SECONDS);
                   Logger.logMessage(Logger.MessageType.INFORMATION, "Audio playing done.");
               } catch (TimeoutException e) {
                   Logger.logMessage(Logger.MessageType.INFORMATION, "No response from user in 30 sec.");
                   cancelMediaProcessing();
               }
            }
        } catch (CancellationException e) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Play audio operation cancelled");
            this.playAudioCompletedTask.complete(false);
        } catch (Exception ex) {
            if (playAudioCompletedTask.isCancelled()) {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Play audio operation cancelled");
            } else {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Failure occurred while playing audio on the call. Exception: " + ex.getMessage());
            }
            this.playAudioCompletedTask.complete(false);
        }
    }

    private void registerToPlayAudioResultEvent(String operationContext) {
        // Set the callback method
        NotificationCallback playPromptResponseNotification = ((callEvent) -> {
            PlayAudioResultEvent playAudioResultEvent = (PlayAudioResultEvent) callEvent;
            Logger.logMessage(Logger.MessageType.INFORMATION, "Play audio status -- > " + playAudioResultEvent.getStatus());

            if (playAudioResultEvent.getStatus().equals(CallingOperationStatus.COMPLETED)) {
                EventDispatcher.getInstance().unsubscribe(CallingServerEventType.PLAY_AUDIO_RESULT_EVENT.toString(),
                        operationContext);
                this.playAudioCompletedTask.complete(true);
            } else if (playAudioResultEvent.getStatus().equals(CallingOperationStatus.FAILED)) {
                this.playAudioCompletedTask.complete(false);
            }
        });

        // Subscribe to event
        EventDispatcher.getInstance().subscribe(CallingServerEventType.PLAY_AUDIO_RESULT_EVENT.toString(),
                operationContext, playPromptResponseNotification);
    }

    private void hangup() {
        if (reportCancellationToken.isCancellationRequested()) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Cancellation request, Hangup will not be performed");
            return;
        }

        Logger.logMessage(Logger.MessageType.INFORMATION, "Performing Hangup operation");
        Response<Void> response = this.callConnection.hangupWithResponse(null);
        Logger.logMessage(Logger.MessageType.INFORMATION, "hangupWithResponse -- > " + getResponse(response));
    }

    private void cancelMediaProcessing() {
        if (reportCancellationToken.isCancellationRequested()) {
            Logger.logMessage(Logger.MessageType.INFORMATION,"Cancellation request, CancelMediaProcessing will not be performed");
            return;
        }

        Logger.logMessage(Logger.MessageType.INFORMATION, "Performing cancel media processing operation to stop playing audio");

        Response<Void> cancelMediaResponse = this.callConnection.cancelAllMediaOperationsWithResponse(null);

        Logger.logMessage(Logger.MessageType.INFORMATION, "cancelAllMediaOperationsWithResponse -- > " + getResponse(cancelMediaResponse));
    }

    private void registerToDtmfResultEvent(String callLegId) {
        NotificationCallback dtmfReceivedEvent = ((callEvent) -> {
            ToneReceivedEvent toneReceivedEvent = (ToneReceivedEvent) callEvent;
            ToneInfo toneInfo = toneReceivedEvent.getToneInfo();
            Logger.logMessage(Logger.MessageType.INFORMATION, "Tone received -- > : " + toneInfo.getTone());

            if (toneInfo.getTone().equals(ToneValue.TONE1)) {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Transferring call to participant" +  this.targetParticipant);
                Boolean transferToParticipantCompleted = transferToParticipant(this.targetParticipant);
                if (!transferToParticipantCompleted) {
                    retryTransferToParticipant(this.targetParticipant);
                }
                this.toneReceivedCompleteTask.complete(true);

                this.playAudioCompletedTask.complete(true);

            } else {
                this.toneReceivedCompleteTask.complete(false);
            }

            EventDispatcher.getInstance().unsubscribe(CallingServerEventType.TONE_RECEIVED_EVENT.toString(), callLegId);
        });
        // Subscribe to event
        EventDispatcher.getInstance().subscribe(CallingServerEventType.TONE_RECEIVED_EVENT.toString(), callLegId,
                dtmfReceivedEvent);
    }

    private boolean transferToParticipant(String targetParticipant) {
        CommunicationIdentifier identifier = getCommunicationIdentifier(targetParticipant);

        if(identifier == null) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Unknown identity provided. Enter valid phone number or communication user id");
            return true;
        }

        String operationContext = UUID.randomUUID().toString();
        RegisterToTransferParticipantsResultEvent(operationContext);
        Response<TransferCallResult> response = this.callConnection.transferToParticipantWithResponse(identifier, null, null, operationContext, null);
        Logger.logMessage(Logger.MessageType.INFORMATION, "Transfer to participant response -- > " + getResponse(response));

        Boolean transferToParticipantCompleted = false;
        try {
            transferToParticipantCompleted = this.transferToParticipantCompleteTask.get();
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR, "Failed to add participant InterruptedException -- > " + ex.getMessage());
        }
        return transferToParticipantCompleted;
    }

    private void RegisterToTransferParticipantsResultEvent(String operationContext) {
        NotificationCallback transferParticipantsResultEvent = ((callEvent) -> {
            TransferCallResultEvent transferCallResultEvent = (TransferCallResultEvent) callEvent;
            CallingOperationStatus operationStatus = transferCallResultEvent.getStatus();
            Logger.logMessage(Logger.MessageType.INFORMATION, "transfer to participant status -- > " + operationStatus);
            if (operationStatus.equals(CallingOperationStatus.COMPLETED)) {
                this.transferToParticipantCompleteTask.complete(true);
            } else if (operationStatus.equals(CallingOperationStatus.FAILED)) {
                this.transferToParticipantCompleteTask.complete(false);
            }
            EventDispatcher.getInstance().unsubscribe(CallingServerEventType.TRANSFER_CALL_RESULT_EVENT.toString(),
                    operationContext);
        });

        // Subscribe to event
        EventDispatcher.getInstance().subscribe(CallingServerEventType.TRANSFER_CALL_RESULT_EVENT.toString(),
                operationContext, transferParticipantsResultEvent);
    }

    private void retryTransferToParticipant(String targetParticipant) {
        int retryAttemptCount = 1;
        while (retryAttemptCount <= this.MaxRetryAttemptCount) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Retrying transfer participant attempt -- > " + retryAttemptCount + " is in progress");
            Boolean transferToParticipantCompleted = transferToParticipant(targetParticipant);

            if (transferToParticipantCompleted) {
                return;
            } else {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Retry transfer participant attempt -- > " + retryAttemptCount + " has failed");
                retryAttemptCount++;
            }
        }
    }

    private CommunicationIdentifier getCommunicationIdentifier(String targetParticipant) {
        if(Pattern.matches(userIdentityRegex, targetParticipant)) {
            return new CommunicationUserIdentifier(targetParticipant);
        } else if (Pattern.matches(phoneIdentityRegex, targetParticipant)) {
            return new PhoneNumberIdentifier(targetParticipant);
        } else {
            return null;
        }
    }
}
