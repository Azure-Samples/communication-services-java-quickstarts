package com.communication.outboundcallreminder;

import com.communication.outboundcallreminder.EventHandler.EventDispatcher;
import com.communication.outboundcallreminder.EventHandler.NotificationCallback;
import com.azure.communication.callingserver.*;
import com.azure.communication.callingserver.models.*;
import com.azure.communication.callingserver.models.events.*;
import com.azure.communication.common.*;
import com.azure.core.http.HttpHeader;
import com.azure.core.http.netty.*;
import com.azure.core.http.rest.Response;
import com.azure.cosmos.implementation.changefeed.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class OutboundCallReminder {

    private CallConfiguration callConfiguration = null;
    private CallingServerClient callingServerClient = null;
    private CallConnection callConnection = null;
    private CancellationTokenSource reportCancellationTokenSource;
    private CancellationToken reportCancellationToken;
    private CompletableFuture<Boolean> callConnectedTask;
    private CompletableFuture<Boolean> playAudioCompletedTask;
    private CompletableFuture<Boolean> callTerminatedTask;
    private CompletableFuture<Boolean> toneReceivedCompleteTask;
    private CompletableFuture<Boolean> addParticipantCompleteTask;
    private Integer maxRetryAttemptCount = Integer
            .parseInt(ConfigurationManager.getInstance().getAppSettings("MaxRetryCount"));

    public OutboundCallReminder(CallConfiguration callConfiguration) {
        this.callConfiguration = callConfiguration;

        NettyAsyncHttpClientBuilder httpClientBuilder = new NettyAsyncHttpClientBuilder();
        CallingServerClientBuilder callClientBuilder = new CallingServerClientBuilder().httpClient(httpClientBuilder.build())
                .connectionString(this.callConfiguration.ConnectionString);

        this.callingServerClient = callClientBuilder.buildClient();
    }

    public void report(String targetPhoneNumber, String participant) {
        reportCancellationTokenSource = new CancellationTokenSource();
        reportCancellationToken = reportCancellationTokenSource.getToken();

        try {
            createCallAsync(targetPhoneNumber);
            registerToDtmfResultEvent(callConnection.getCallConnectionId());

            playAudioAsync(callConnection.getCallConnectionId());
            Boolean playAudioCompleted = playAudioCompletedTask.get();

            if (!playAudioCompleted) {
                hangupAsync(callConnection.getCallConnectionId());
            } else {
                Boolean toneReceivedComplete = toneReceivedCompleteTask.get();
                if (toneReceivedComplete) {
                    Logger.logMessage(Logger.MessageType.INFORMATION,"Initiating add participant from number --> " + targetPhoneNumber + " and participant identifier is -- > " + participant);

                    Boolean addParticipantCompleted = addParticipant(callConnection.getCallConnectionId(), participant);
                    if (!addParticipantCompleted) {
                        retryAddParticipantAsync(callConnection.getCallConnectionId(), participant);
                    }

                    hangupAsync(callConnection.getCallConnectionId());
                } else {
                    hangupAsync(callConnection.getCallConnectionId());
                }
            }

            // Wait for the call to terminate
            callTerminatedTask.get();
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR, "Call ended unexpectedly, reason -- > " + ex.getMessage());
        }
    }

    private void createCallAsync(String targetPhoneNumber) {
        try {
            // Preparting request data
            CommunicationUserIdentifier source = new CommunicationUserIdentifier(this.callConfiguration.SourceIdentity);
            PhoneNumberIdentifier target = new PhoneNumberIdentifier(targetPhoneNumber);

            List<MediaType> callModality = new ArrayList<MediaType>() { {add(MediaType.AUDIO);} };

            List<EventSubscriptionType> eventSubscriptionType = new ArrayList<EventSubscriptionType>() {
                    {add(EventSubscriptionType.PARTICIPANTS_UPDATED); add(EventSubscriptionType.DTMF_RECEIVED);}};

            CreateCallOptions createCallOption = new CreateCallOptions(this.callConfiguration.AppCallbackUrl,
                    callModality, eventSubscriptionType);

            createCallOption.setAlternateCallerId(new PhoneNumberIdentifier(this.callConfiguration.SourcePhoneNumber));

            Logger.logMessage(Logger.MessageType.INFORMATION,"Performing CreateCall operation");

            List<CommunicationIdentifier> targets = new ArrayList<CommunicationIdentifier>() { {add(target);} };

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
        callTerminatedTask = new CompletableFuture<Boolean>();
        callConnectedTask = new CompletableFuture<Boolean>();
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

    private void registerToDtmfResultEvent(String callLegId) {
        toneReceivedCompleteTask = new CompletableFuture<Boolean>();

        NotificationCallback dtmfReceivedEvent = ((callEvent) -> {
            ToneReceivedEvent toneReceivedEvent = (ToneReceivedEvent) callEvent;
            ToneInfo toneInfo = toneReceivedEvent.getToneInfo();

            Logger.logMessage(Logger.MessageType.INFORMATION, "Tone received -- > : " + toneInfo.getTone());

            if (toneInfo.getTone().equals(ToneValue.TONE1)) {
                toneReceivedCompleteTask.complete(true);
            } else {
                toneReceivedCompleteTask.complete(false);
            }
            EventDispatcher.getInstance().unsubscribe(CallingServerEventType.TONE_RECEIVED_EVENT.toString(), callLegId);
            // cancel playing audio
            cancelMediaProcessing(callLegId);
        });
        // Subscribe to event
        EventDispatcher.getInstance().subscribe(CallingServerEventType.TONE_RECEIVED_EVENT.toString(), callLegId,
                dtmfReceivedEvent);
    }

    private void cancelMediaProcessing(String callLegId) {
        if (reportCancellationToken.isCancellationRequested()) {
            Logger.logMessage(Logger.MessageType.INFORMATION,"Cancellation request, CancelMediaProcessing will not be performed");
            return;
        }

        Logger.logMessage(Logger.MessageType.INFORMATION, "Performing cancel media processing operation to stop playing audio");

        String operationContext = UUID.randomUUID().toString();
        Response<CancelAllMediaOperationsResult> cancelmediaresponse = this.callConnection.cancelAllMediaOperationsWithResponse(operationContext, null);
        CancelAllMediaOperationsResult response = cancelmediaresponse.getValue();

        Logger.logMessage(Logger.MessageType.INFORMATION, "cancelAllMediaOperationsWithResponse -- > " + getResponse(cancelmediaresponse) + 
        ", Id: " + response.getOperationId() + ", OperationContext: " + response.getOperationContext() + ", OperationStatus: " +
        response.getStatus().toString());
    }

    private void playAudioAsync(String callLegId) {
        if (reportCancellationToken.isCancellationRequested()) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Cancellation request, PlayAudio will not be performed");
            return;
        }

        try {
            // Preparing data for request
            String audioFileUri = callConfiguration.AudioFileUrl;
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

                // listen to play audio events
                registerToPlayAudioResultEvent(response.getOperationContext());

                CompletableFuture<Boolean> maxWait = CompletableFuture.supplyAsync(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(30);
                    } catch (InterruptedException ex) {
                        Logger.logMessage(Logger.MessageType.ERROR, " -- > " + ex.getMessage());
                    }
                    return false;
                });

                CompletableFuture<Object> completedTask = CompletableFuture.anyOf(playAudioCompletedTask, maxWait);
                if (completedTask.get() != playAudioCompletedTask.get()) {
                    Logger.logMessage(Logger.MessageType.INFORMATION, "No response from user in 30 sec, initiating hangup");
                    playAudioCompletedTask.complete(false);
                    toneReceivedCompleteTask.complete(false);
                }
            }
        } catch (Exception ex) {
            if (playAudioCompletedTask.isCancelled()) {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Play audio operation cancelled");
            } else {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Failure occured while playing audio on the call. Exception: " + ex.getMessage());
            }
        }
    }

    private void hangupAsync(String callLegId) {
        if (reportCancellationToken.isCancellationRequested()) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Cancellation request, Hangup will not be performed");
            return;
        }

        Logger.logMessage(Logger.MessageType.INFORMATION, "Performing Hangup operation");
        Response<Void> response = this.callConnection.hangupWithResponse(null);
        Logger.logMessage(Logger.MessageType.INFORMATION, "hangupWithResponse -- > " + getResponse(response));
    }

    private void registerToPlayAudioResultEvent(String operationContext) {
        playAudioCompletedTask = new CompletableFuture<Boolean>();
        NotificationCallback playPromptResponseNotification = ((callEvent) -> {
            PlayAudioResultEvent playAudioResultEvent = (PlayAudioResultEvent) callEvent;
            Logger.logMessage(Logger.MessageType.INFORMATION, "Play audio status -- > " + playAudioResultEvent.getStatus());

            if (playAudioResultEvent.getStatus().equals(OperationStatus.COMPLETED)) {
                EventDispatcher.getInstance().unsubscribe(CallingServerEventType.PLAY_AUDIO_RESULT_EVENT.toString(),
                        operationContext);
                playAudioCompletedTask.complete(true);
            } else if (playAudioResultEvent.getStatus().equals(OperationStatus.FAILED)) {
                playAudioCompletedTask.complete(false);
            }
        });

        // Subscribe to event
        EventDispatcher.getInstance().subscribe(CallingServerEventType.PLAY_AUDIO_RESULT_EVENT.toString(),
                operationContext, playPromptResponseNotification);
    }

    private void retryAddParticipantAsync(String callLegId, String addedParticipant) {
        int retryAttemptCount = 1;
        while (retryAttemptCount <= maxRetryAttemptCount) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Retrying add participant attempt -- > " + retryAttemptCount + " is in progress");
            Boolean addParticipantResult = addParticipant(callLegId, addedParticipant);

            if (addParticipantResult) {
                return;
            } else {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Retry add participant attempt -- > " + retryAttemptCount + " has failed");
                retryAttemptCount++;
            }
        }
    }

    private Boolean addParticipant(String callLegId, String addedParticipant) {
        CommunicationIdentifierKind identifierKind = getIdentifierKind(addedParticipant);

        if (identifierKind == CommunicationIdentifierKind.UnknownIdentity) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Unknown identity provided. Enter valid phone number or communication user id");
            return true;
        } else {
            CommunicationIdentifier participant = null;
            String operationContext = UUID.randomUUID().toString();

            registerToAddParticipantsResultEvent(operationContext);

            if (identifierKind == CommunicationIdentifierKind.UserIdentity) {
                participant = new CommunicationUserIdentifier(addedParticipant);

            } else if (identifierKind == CommunicationIdentifierKind.PhoneIdentity) {
                participant = new PhoneNumberIdentifier(addedParticipant);
            }

            String alternateCallerId = new PhoneNumberIdentifier(
                    ConfigurationManager.getInstance().getAppSettings("SourcePhone")).toString();
            Response<AddParticipantResult> response = callConnection.addParticipantWithResponse(participant, alternateCallerId, operationContext, null);
            Logger.logMessage(Logger.MessageType.INFORMATION, "addParticipantWithResponse -- > " + getResponse(response));
        }

        Boolean addParticipantCompleted = false;
        try {
            addParticipantCompleted = addParticipantCompleteTask.get();
        } catch (InterruptedException ex) {
            Logger.logMessage(Logger.MessageType.ERROR, "Failed to add participant InterruptedException -- > " + ex.getMessage());
        } catch (ExecutionException ex) {
            Logger.logMessage(Logger.MessageType.ERROR,"Failed to add participant ExecutionException -- > " + ex.getMessage());
        }

        return addParticipantCompleted;
    }

    private void registerToAddParticipantsResultEvent(String operationContext) {
        addParticipantCompleteTask = new CompletableFuture<Boolean>();

        NotificationCallback addParticipantReceivedEvent = ((callEvent) -> {
            AddParticipantResultEvent addParticipantsUpdatedEvent = (AddParticipantResultEvent) callEvent;
            OperationStatus operationStatus = addParticipantsUpdatedEvent.getStatus();
            if (operationStatus.equals(OperationStatus.COMPLETED)) {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Add participant status -- > " + operationStatus);
                addParticipantCompleteTask.complete(true);
            } else if (operationStatus.equals(OperationStatus.FAILED)) {
                addParticipantCompleteTask.complete(false);
            }
            EventDispatcher.getInstance().unsubscribe(CallingServerEventType.ADD_PARTICIPANT_RESULT_EVENT.toString(),
                    operationContext);
        });

        // Subscribe to event
        EventDispatcher.getInstance().subscribe(CallingServerEventType.ADD_PARTICIPANT_RESULT_EVENT.toString(),
                operationContext, addParticipantReceivedEvent);
    }

    public final String userIdentityRegex = "8:acs:[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}_[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}";
    public final String phoneIdentityRegex = "^\\+\\d{10,14}$";

    private CommunicationIdentifierKind getIdentifierKind(String participantnumber) {
        // checks the identity type returns as string
        return ((Pattern.matches(userIdentityRegex, participantnumber)) ? CommunicationIdentifierKind.UserIdentity
                : (Pattern.matches(phoneIdentityRegex, participantnumber)) ? CommunicationIdentifierKind.PhoneIdentity
                        : CommunicationIdentifierKind.UnknownIdentity);
    }

    public String getResponse(Response<?> response)
    {
        String responseString = null;
        responseString = "StatusCode: " + response.getStatusCode() + ", Headers: { ";
        Iterator<HttpHeader> headers = response.getHeaders().iterator();

        while(headers.hasNext())
        {
            HttpHeader header = headers.next();
            responseString += header.getName()+ ":" + header.getValue().toString() + ", ";
        }
        responseString += "} ";
        return responseString;
    }
}