package com.communication.outboundcallreminder;

import com.communication.outboundcallreminder.EventHandler.EventDispatcher;
import com.communication.outboundcallreminder.EventHandler.NotificationCallback;
import com.azure.communication.callingserver.*;
import com.azure.communication.callingserver.models.*;
import com.azure.communication.callingserver.models.events.*;
import com.azure.communication.common.*;
import com.azure.core.http.netty.*;
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
    private CompletableFuture<Boolean> callEstablishedTask;
    private CompletableFuture<Boolean> playAudioCompletedTask;
    private CompletableFuture<Boolean> callTerminatedTask;
    private CompletableFuture<Boolean> toneReceivedCompleteTask;
    private CompletableFuture<Boolean> inviteParticipantCompleteTask;
    private Integer maxRetryAttemptCount = Integer
            .parseInt(ConfigurationManager.GetInstance().GetAppSettings("MaxRetryCount"));

    public OutboundCallReminder(CallConfiguration callConfiguration) {
        this.callConfiguration = callConfiguration;

        NettyAsyncHttpClientBuilder httpClientBuilder = new NettyAsyncHttpClientBuilder();
        CallingServerClientBuilder callClientBuilder = new CallingServerClientBuilder().httpClient(httpClientBuilder.build())
                .connectionString(this.callConfiguration.ConnectionString);

        this.callingServerClient = callClientBuilder.buildClient();
    }

    public void Report(String targetPhoneNumber, String participant) {
        reportCancellationTokenSource = new CancellationTokenSource();
        reportCancellationToken = reportCancellationTokenSource.getToken();

        try {
            CreateCallAsync(targetPhoneNumber);
            RegisterToDtmfResultEvent(callConnection.getCallConnectionId());

            PlayAudioAsync(callConnection.getCallConnectionId());
            Boolean playAudioCompleted = playAudioCompletedTask.get();

            if (!playAudioCompleted) {
                HangupAsync(callConnection.getCallConnectionId());
            } else {
                Boolean toneReceivedComplete = toneReceivedCompleteTask.get();
                if (toneReceivedComplete) {
                    System.out.println("Initiating invite participant from number " + targetPhoneNumber
                            + " and participant identifier is" + participant);

                    Boolean inviteParticipantCompleted = InviteParticipant(callConnection.getCallConnectionId(), participant);
                    if (!inviteParticipantCompleted) {
                        RetryInviteParticipantAsync(callConnection.getCallConnectionId(), participant);
                    }

                    HangupAsync(callConnection.getCallConnectionId());
                } else {
                    HangupAsync(callConnection.getCallConnectionId());
                }
            }

            // Wait for the call to terminate
            callTerminatedTask.get();
        } catch (Exception ex) {
            System.out.println("Call ended unexpectedly, reason: " + ex.getMessage());
        }
    }

    private void CreateCallAsync(String targetPhoneNumber) {
        try {
            // Preparting request data
            CommunicationUserIdentifier source = new CommunicationUserIdentifier(this.callConfiguration.SourceIdentity);
            PhoneNumberIdentifier target = new PhoneNumberIdentifier(targetPhoneNumber);

            CallModality[] callModality = new CallModality[] { CallModality.AUDIO };

            EventSubscriptionType[] eventSubscriptionType = new EventSubscriptionType[] {
                    EventSubscriptionType.PARTICIPANTS_UPDATED, EventSubscriptionType.DTMF_RECEIVED };

            CreateCallOptions createCallOption = new CreateCallOptions(this.callConfiguration.AppCallbackUrl,
                    callModality, eventSubscriptionType);

            createCallOption.setAlternateCallerId(new PhoneNumberIdentifier(this.callConfiguration.SourcePhoneNumber));

            System.out.println("Performing CreateCall operation");

            CommunicationIdentifier[] targets = new CommunicationIdentifier[] { target };

            callConnection = this.callingServerClient.createCallConnection(source, targets, createCallOption);

            System.out.println("Call initiated with Call Leg id:" + callConnection.getCallConnectionId());

            RegisterToCallStateChangeEvent(callConnection.getCallConnectionId());
            callEstablishedTask.get();

        } catch (Exception ex) {
            System.out.println("Failure occured while creating/establishing the call. Exception: " + ex.getMessage());
        }
    }

    private void RegisterToCallStateChangeEvent(String callLegId) {
        callTerminatedTask = new CompletableFuture<Boolean>();
        callEstablishedTask = new CompletableFuture<Boolean>();
        // Set the callback method
        NotificationCallback callStateChangeNotificaiton = ((callEvent) -> {
            CallConnectionStateChangedEvent callStateChanged = (CallConnectionStateChangedEvent) callEvent;

            System.out.println("Call State changed to: " + callStateChanged.getCallConnectionState());

            if (callStateChanged.getCallConnectionState().toString().equalsIgnoreCase(CallConnectionState.ESTABLISHED.toString())) {
                System.out.println("Call State successfully ESTABLISHED");
                callEstablishedTask.complete(true);
            } else if (callStateChanged.getCallConnectionState().toString().equalsIgnoreCase(CallConnectionState.TERMINATED.toString())) {
                EventDispatcher.GetInstance()
                        .Unsubscribe(CallingServerEventType.CALL_CONNECTION_STATE_CHANGED_EVENT.toString(), callLegId);
                reportCancellationTokenSource.cancel();
                callTerminatedTask.complete(true);
            }
        });

        // Subscribe to the event
        EventDispatcher.GetInstance().Subscribe(CallingServerEventType.CALL_CONNECTION_STATE_CHANGED_EVENT.toString(),
                callLegId, callStateChangeNotificaiton);
    }

    private void RegisterToDtmfResultEvent(String callLegId) {
        toneReceivedCompleteTask = new CompletableFuture<Boolean>();

        NotificationCallback dtmfReceivedEvent = ((callEvent) -> {
            ToneReceivedEvent toneReceivedEvent = (ToneReceivedEvent) callEvent;
            ToneInfo toneInfo = toneReceivedEvent.getToneInfo();

            System.out.println("Tone received --------- : " + toneInfo.getTone());

            if (toneInfo.getTone().toString().equalsIgnoreCase(ToneValue.TONE1.toString())) {
                toneReceivedCompleteTask.complete(true);
            } else {
                toneReceivedCompleteTask.complete(false);
            }
            EventDispatcher.GetInstance().Unsubscribe(CallingServerEventType.TONE_RECEIVED_EVENT.toString(), callLegId);
            // cancel playing audio
            CancelMediaProcessing(callLegId);
        });
        // Subscribe to event
        EventDispatcher.GetInstance().Subscribe(CallingServerEventType.TONE_RECEIVED_EVENT.toString(), callLegId,
                dtmfReceivedEvent);
    }

    private void CancelMediaProcessing(String callLegId) {
        if (reportCancellationToken.isCancellationRequested()) {
            System.out.println("Cancellation request, CancelMediaProcessing will not be performed");
            return;
        }

        System.out.println("Performing cancel media processing operation to stop playing audio");

        String operationContext = UUID.randomUUID().toString();
        this.callConnection.cancelAllMediaOperations(operationContext);
    }

    private void PlayAudioAsync(String callLegId) {
        if (reportCancellationToken.isCancellationRequested()) {
            System.out.println("Cancellation request, PlayAudio will not be performed");
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

            System.out.println("Performing PlayAudio operation");
            PlayAudioResponse response = this.callConnection.playAudio(audioFileUri, playAudioOptions);

            if (response.getStatus().toString().equalsIgnoreCase(OperationStatus.RUNNING.toString())) {
                System.out.println("Play Audio state: " + OperationStatus.RUNNING);

                // listen to play audio events
                RegisterToPlayAudioResultEvent(response.getOperationContext());

                CompletableFuture<Boolean> maxWait = CompletableFuture.supplyAsync(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(30);
                    } catch (InterruptedException ex) {
                        System.out.println(ex.getMessage());
                    }
                    return false;
                });

                CompletableFuture<Object> completedTask = CompletableFuture.anyOf(playAudioCompletedTask, maxWait);
                if (completedTask.get() != playAudioCompletedTask.get()) {
                    System.out.println("No response from user in 30 sec, initiating hangup");
                    playAudioCompletedTask.complete(false);
                    toneReceivedCompleteTask.complete(false);
                }
            }
        } catch (Exception ex) {
            if (playAudioCompletedTask.isCancelled()) {
                System.out.println("Play audio operation cancelled");
            } else {
                System.out.println("Failure occured while playing audio on the call. Exception: " + ex.getMessage());
            }
        }
    }

    private void HangupAsync(String callLegId) {
        if (reportCancellationToken.isCancellationRequested()) {
            System.out.println("Cancellation request, Hangup will not be performed");
            return;
        }

        System.out.println("Performing Hangup operation");
        this.callConnection.hangup();
    }

    private void RegisterToPlayAudioResultEvent(String operationContext) {
        playAudioCompletedTask = new CompletableFuture<Boolean>();
        NotificationCallback playPromptResponseNotification = ((callEvent) -> {
            PlayAudioResultEvent playAudioResultEvent = (PlayAudioResultEvent) callEvent;
            System.out.println("Play audio status: " + playAudioResultEvent.getStatus());

            if (playAudioResultEvent.getStatus().toString().equalsIgnoreCase(OperationStatus.COMPLETED.toString())) {
                EventDispatcher.GetInstance().Unsubscribe(CallingServerEventType.PLAY_AUDIO_RESULT_EVENT.toString(),
                        operationContext);
                playAudioCompletedTask.complete(true);
            } else if (playAudioResultEvent.getStatus().toString()
                    .equalsIgnoreCase(OperationStatus.FAILED.toString())) {
                playAudioCompletedTask.complete(false);
            }
        });

        // Subscribe to event
        EventDispatcher.GetInstance().Subscribe(CallingServerEventType.PLAY_AUDIO_RESULT_EVENT.toString(),
                operationContext, playPromptResponseNotification);
    }

    private void RetryInviteParticipantAsync(String callLegId, String invitedParticipant) {
        int retryAttemptCount = 1;
        while (retryAttemptCount <= maxRetryAttemptCount) {
            System.out.println("Retrying invite participant attempt " + retryAttemptCount + " is in progress");
            Boolean inviteParticipantResult = InviteParticipant(callLegId, invitedParticipant);

            if (inviteParticipantResult) {
                return;
            } else {
                System.out.println("Retry invite participant attempt " + retryAttemptCount + " has failed");
                retryAttemptCount++;
            }
        }
    }

    private Boolean InviteParticipant(String callLegId, String invitedParticipant) {
        CommunicationIdentifierKind identifierKind = GetIdentifierKind(invitedParticipant);

        if (identifierKind == CommunicationIdentifierKind.UnknownIdentity) {
            System.out.println("Unknown identity provided. Enter valid phone number or communication user id");
            return true;
        } else {
            CommunicationIdentifier participant = null;
            String operationContext = UUID.randomUUID().toString();

            RegisterToInviteParticipantsResultEvent(operationContext);

            if (identifierKind == CommunicationIdentifierKind.UserIdentity) {
                participant = new CommunicationUserIdentifier(invitedParticipant);

            } else if (identifierKind == CommunicationIdentifierKind.PhoneIdentity) {
                participant = new PhoneNumberIdentifier(invitedParticipant);
            }

            String alternateCallerId = new PhoneNumberIdentifier(
                    ConfigurationManager.GetInstance().GetAppSettings("SourcePhone")).toString();
            callConnection.addParticipant(participant, alternateCallerId, operationContext);
        }

        Boolean inviteParticipantCompleted = false;
        try {
            inviteParticipantCompleted = inviteParticipantCompleteTask.get();
        } catch (InterruptedException ex) {
            System.out.println("Failed to invite participant InterruptedException: " + ex.getMessage());
        } catch (ExecutionException ex) {
            System.out.println("Failed to invite participant ExecutionException: " + ex.getMessage());
        }

        return inviteParticipantCompleted;
    }

    private void RegisterToInviteParticipantsResultEvent(String operationContext) {
        inviteParticipantCompleteTask = new CompletableFuture<Boolean>();

        NotificationCallback inviteParticipantReceivedEvent = ((callEvent) -> {
            InviteParticipantResultEvent inviteParticipantsUpdatedEvent = (InviteParticipantResultEvent) callEvent;
            String status = inviteParticipantsUpdatedEvent.getStatus().toString();
            if (status.equalsIgnoreCase(OperationStatus.COMPLETED.toString())) {
                System.out.println("Invite participant status - " + status);
                inviteParticipantCompleteTask.complete(true);
            } else if (status.equalsIgnoreCase(OperationStatus.FAILED.toString())) {
                inviteParticipantCompleteTask.complete(false);
            }
            EventDispatcher.GetInstance().Unsubscribe(CallingServerEventType.INVITE_PARTICIPANT_RESULT_EVENT.toString(),
                    operationContext);
        });

        // Subscribe to event
        EventDispatcher.GetInstance().Subscribe(CallingServerEventType.INVITE_PARTICIPANT_RESULT_EVENT.toString(),
                operationContext, inviteParticipantReceivedEvent);
    }

    public final String userIdentityRegex = "8:acs:[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}_[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}";
    public final String phoneIdentityRegex = "^\\+\\d{10,14}$";

    private CommunicationIdentifierKind GetIdentifierKind(String participantnumber) {
        // checks the identity type returns as string
        return ((Pattern.matches(userIdentityRegex, participantnumber)) ? CommunicationIdentifierKind.UserIdentity
                : (Pattern.matches(phoneIdentityRegex, participantnumber)) ? CommunicationIdentifierKind.PhoneIdentity
                        : CommunicationIdentifierKind.UnknownIdentity);
    }
}