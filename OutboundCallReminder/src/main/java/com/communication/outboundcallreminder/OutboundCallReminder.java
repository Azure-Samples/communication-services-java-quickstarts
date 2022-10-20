package com.communication.outboundcallreminder;

import com.azure.communication.callautomation.CallConnection;
import com.azure.communication.callautomation.models.DtmfTone;
import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.models.AddParticipantsOptions;
import com.azure.communication.callautomation.models.AddParticipantsResult;
import com.azure.communication.callautomation.models.CallMediaRecognizeDtmfOptions;
import com.azure.communication.callautomation.models.CallMediaRecognizeOptions;
import com.azure.communication.callautomation.models.CreateCallOptions;
import com.azure.communication.callautomation.models.CreateCallResult;
import com.azure.communication.callautomation.models.FileSource;
import com.azure.communication.callautomation.models.HangUpOptions;
import com.azure.communication.callautomation.models.PlaySource;
import com.azure.communication.callautomation.models.events.AddParticipantsFailedEvent;
import com.azure.communication.callautomation.models.events.AddParticipantsSucceededEvent;
import com.azure.communication.callautomation.models.events.CallConnectedEvent;
import com.azure.communication.callautomation.models.events.CallDisconnectedEvent;
import com.azure.communication.callautomation.models.events.PlayCanceledEvent;
import com.azure.communication.callautomation.models.events.PlayCompletedEvent;
import com.azure.communication.callautomation.models.events.PlayFailedEvent;
import com.azure.communication.callautomation.models.events.RecognizeCompletedEvent;
import com.azure.communication.callautomation.models.events.RecognizeFailedEvent;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;

import com.azure.cosmos.implementation.changefeed.CancellationToken;
import com.azure.cosmos.implementation.changefeed.CancellationTokenSource;
import com.communication.outboundcallreminder.EventHandler.EventDispatcher;
import com.communication.outboundcallreminder.EventHandler.NotificationCallback;
import com.azure.core.http.HttpHeader;
import com.azure.core.http.rest.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.time.Duration;


public class OutboundCallReminder {

    private final CallConfiguration callConfiguration;
    private final CallAutomationClient callingAutomationClient;
    private CallConnection callConnection = null;
    private CancellationTokenSource reportCancellationTokenSource;
    private CancellationToken reportCancellationToken;
    private CompletableFuture<Boolean> callConnectedTask;
    private CompletableFuture<Boolean> playAudioCompletedTask;
    private CompletableFuture<Boolean> callTerminatedTask;
    private CompletableFuture<Boolean> toneReceivedCompleteTask;
    private CompletableFuture<Boolean> addParticipantCompleteTask;

    public OutboundCallReminder(CallConfiguration callConfiguration) {
        this.callConfiguration = callConfiguration;
        this.callingAutomationClient = new CallAutomationClientBuilder().connectionString(this.callConfiguration.connectionString)
        .buildClient();
    }

    public void report(String targetPhoneNumber, String participant) {
        reportCancellationTokenSource = new CancellationTokenSource();
        reportCancellationToken = reportCancellationTokenSource.getToken();

        try {
            createCallAsync(targetPhoneNumber);
            registerToDtmfResultEvent(callConnection.getCallProperties().getCallConnectionId());

            startRecognizingDtmf(targetPhoneNumber);
            Boolean playAudioCompleted = playAudioCompletedTask.get();

            if (!playAudioCompleted) {
                hangupAsync();
            } else {
                Boolean toneReceivedComplete = toneReceivedCompleteTask.get();
                if (toneReceivedComplete) {
                    Logger.logMessage(Logger.MessageType.INFORMATION,"Initiating add participant from number --> " + targetPhoneNumber + " and participant identifier is -- > " + participant);

                    Boolean addParticipantCompleted = addParticipant(participant);
                    if (!addParticipantCompleted) {
                        retryAddParticipantAsync(participant);
                    }
                }
                hangupAsync();
            }

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
                {add(new PhoneNumberIdentifier(targetPhoneNumber));}
            };

            CreateCallOptions createCallOption = new CreateCallOptions(source, targets, this.callConfiguration.appCallbackUrl);
            createCallOption.setSourceCallerId(this.callConfiguration.sourcePhoneNumber);
            Logger.logMessage(Logger.MessageType.INFORMATION,"Performing CreateCall operation");

            Response<CreateCallResult> response = this.callingAutomationClient.
            createCallWithResponse(createCallOption, null);
            CreateCallResult callResult = response.getValue();
            callConnection = callResult.getCallConnection();

            Logger.logMessage(Logger.MessageType.INFORMATION, "createCallConnectionWithResponse -- > " + getResponse(response) + ", Call connection ID: " + callResult.getCallConnectionProperties().getCallConnectionId());
            Logger.logMessage(Logger.MessageType.INFORMATION, "Call initiated with Call Leg id -- >" + callResult.getCallConnectionProperties().getCallConnectionId());

            registerToCallStateChangeEvent(callResult.getCallConnectionProperties().getCallConnectionId());
            callConnectedTask.get();

        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR, "Failure occured while creating/establishing the call. Exception -- >" + ex.getMessage());
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

    private void registerToDtmfResultEvent(String callLegId) {
        toneReceivedCompleteTask = new CompletableFuture<>();

        NotificationCallback dtmfReceivedEvent = ((callEvent) -> {
            RecognizeCompletedEvent toneReceivedEvent = (RecognizeCompletedEvent) callEvent;
            List<DtmfTone> toneInfo = toneReceivedEvent.getCollectTonesResult().getTones();
            Logger.logMessage(Logger.MessageType.INFORMATION, "Tone received -- > : " + toneInfo);

            if (!toneInfo.isEmpty() && toneInfo.get(0).equals(DtmfTone.ONE)) {
                toneReceivedCompleteTask.complete(true);
            } else {
                toneReceivedCompleteTask.complete(false);
            }
            EventDispatcher.getInstance().unsubscribe(RecognizeCompletedEvent.class.getName(), callLegId);   
            playAudioCompletedTask.complete(true);
            // cancel playing audio
            cancelMediaProcessing();
        });

        NotificationCallback dtmfFailedEvent = ((callEvent) -> {
            EventDispatcher.getInstance().unsubscribe(RecognizeFailedEvent.class.getName(), callLegId);
            toneReceivedCompleteTask.complete(false);
            playAudioCompletedTask.complete(false);
        });

        // Subscribe to event
        EventDispatcher.getInstance().subscribe(RecognizeCompletedEvent.class.getName(), callLegId, dtmfReceivedEvent);
        EventDispatcher.getInstance().subscribe(RecognizeFailedEvent.class.getName(), callLegId, dtmfFailedEvent);
    }

    private void cancelMediaProcessing() {
        if (reportCancellationToken.isCancellationRequested()) {
            Logger.logMessage(Logger.MessageType.INFORMATION,"Cancellation request, CancelMediaProcessing will not be performed");
            return;
        }

        Logger.logMessage(Logger.MessageType.INFORMATION, "Performing cancel media processing operation to stop playing audio");
        this.callConnection.getCallMedia().cancelAllMediaOperations();
    }

    private void startRecognizingDtmf(String targetPhoneNumber) {
        if (reportCancellationToken.isCancellationRequested()) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Cancellation request, PlayAudio will not be performed");
            return;
        }

        try {
            // Preparing data for request
            String audioFileUri = this.callConfiguration.audioFileUrl;   
            PlaySource playSource = new FileSource().setUri(audioFileUri);

            // listen to play audio events
            registerToPlayAudioResultEvent(this.callConnection.getCallProperties().getCallConnectionId());

            //Start recognizing Dtmf Tone
            CallMediaRecognizeOptions callMediaRecognizeOptions = new CallMediaRecognizeDtmfOptions(new PhoneNumberIdentifier(targetPhoneNumber), 1)
            .setInterToneTimeout(Duration.ofSeconds(5))
            .setInterruptCallMediaOperation(true)
            .setInitialSilenceTimeout(Duration.ofSeconds(30))
            .setPlayPrompt(playSource)
            .setInterruptPrompt(true);
            
            Response<Void> startRecognizeResponse = this.callConnection.getCallMedia().startRecognizingWithResponse(callMediaRecognizeOptions, null);
            Logger.logMessage(Logger.MessageType.INFORMATION, "Start Recognizing response-- > " + getResponse(startRecognizeResponse));

            //Wait for 30 secs for input
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
        } catch (Exception ex) {
            if (playAudioCompletedTask.isCancelled()) {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Start Recognizing with play audio prompt got cancelled");
            } else {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Failure occurred while start recognizing with Play audio prompt on the call. Exception: " + ex.getMessage());
            }
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

    private void registerToPlayAudioResultEvent(String callConnectionId) {
        playAudioCompletedTask = new CompletableFuture<>();
        NotificationCallback playCompletedNotification = ((callEvent) -> {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Play audio status completed" );

            EventDispatcher.getInstance().unsubscribe(PlayCompletedEvent.class.getName(), callConnectionId);
            playAudioCompletedTask.complete(true);   
        });

        NotificationCallback playFailedNotification = ((callEvent) -> {
            EventDispatcher.getInstance().unsubscribe(PlayFailedEvent.class.getName(), callConnectionId);
            reportCancellationTokenSource.cancel();
            playAudioCompletedTask.complete(false);
        });

        NotificationCallback playCanceledNotification = ((callEvent) -> {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Play audio status Canceled" );
            EventDispatcher.getInstance().unsubscribe(PlayCanceledEvent.class.getName(), callConnectionId);
            reportCancellationTokenSource.cancel();
            playAudioCompletedTask.complete(false);
        });

        // Subscribe to event
        EventDispatcher.getInstance().subscribe(PlayCompletedEvent.class.getName(), callConnectionId, playCompletedNotification);
        EventDispatcher.getInstance().subscribe(PlayFailedEvent.class.getName(), callConnectionId, playFailedNotification);
        EventDispatcher.getInstance().subscribe(PlayCanceledEvent.class.getName(), callConnectionId, playCanceledNotification);
    }

    private void retryAddParticipantAsync(String addedParticipant) {
        int retryAttemptCount = 1;
        while (retryAttemptCount <= this.callConfiguration.maxRetryAttemptCount) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Retrying add participant attempt -- > " + retryAttemptCount + " is in progress");
            Boolean addParticipantResult = addParticipant(addedParticipant);

            if (addParticipantResult) {
                return;
            } else {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Retry add participant attempt -- > " + retryAttemptCount + " has failed");
                retryAttemptCount++;
            }
        }
    }

    private Boolean addParticipant(String addedParticipant) {
        CommunicationIdentifierKind identifierKind = getIdentifierKind(addedParticipant);

        if (identifierKind == CommunicationIdentifierKind.UnknownIdentity) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Unknown identity provided. Enter valid phone number or communication user id");
            return true;
        } else {
            CommunicationIdentifier participant = null;
            registerToAddParticipantsResultEvent(this.callConnection.getCallProperties().getCallConnectionId());

            if (identifierKind == CommunicationIdentifierKind.UserIdentity) {
                participant = new CommunicationUserIdentifier(addedParticipant);

            } else if (identifierKind == CommunicationIdentifierKind.PhoneIdentity) {
                participant = new PhoneNumberIdentifier(addedParticipant);
            }

            List<CommunicationIdentifier> targets = new ArrayList<CommunicationIdentifier>();
            targets.add(participant);

            AddParticipantsOptions addParticipantsOptions = new AddParticipantsOptions(targets);
            addParticipantsOptions.setSourceCallerId(new PhoneNumberIdentifier(this.callConfiguration.sourcePhoneNumber));
            addParticipantsOptions.setOperationContext(UUID.randomUUID().toString());
            addParticipantsOptions.setInvitationTimeout(Duration.ofSeconds(30));
            addParticipantsOptions.setRepeatabilityHeaders(null);

            Response<AddParticipantsResult> response = this.callConnection.addParticipantsWithResponse(addParticipantsOptions, null);
            Logger.logMessage(Logger.MessageType.INFORMATION, "addParticipantWithResponse -- > " + getResponse(response));
            if(targets.contains(participant)){
                targets.clear();
            }
        }

        Boolean addParticipantCompleted = false;
        try {
            addParticipantCompleted = addParticipantCompleteTask.get();
        } catch (InterruptedException ex) {
            Logger.logMessage(Logger.MessageType.ERROR, "Failed to add participant InterruptedException -- > " + ex.getMessage());
        } catch (ExecutionException ex) {
            Logger.logMessage(Logger.MessageType.ERROR, "Failed to add participant ExecutionException -- > " + ex.getMessage());
        }

        return addParticipantCompleted;
    }

    private void registerToAddParticipantsResultEvent(String callConnectionId) {
        addParticipantCompleteTask = new CompletableFuture<>();

        NotificationCallback addParticipantReceivedEvent = ((callEvent) -> {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Add participant status completed" );
            Logger.logMessage(Logger.MessageType.INFORMATION, "Sleeping for 60 seconds before proceeding further");
            try {
                Thread.sleep(60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            EventDispatcher.getInstance().unsubscribe(AddParticipantsSucceededEvent.class.getName(), callConnectionId);
            addParticipantCompleteTask.complete(true);   
        });

        NotificationCallback addParticipantFailedEvent = ((callEvent) -> {
            EventDispatcher.getInstance().unsubscribe(AddParticipantsFailedEvent.class.getName(), callConnectionId);
            reportCancellationTokenSource.cancel();
            addParticipantCompleteTask.complete(false);
        });

        // Subscribe to event
        EventDispatcher.getInstance().subscribe(AddParticipantsSucceededEvent.class.getName(), callConnectionId, addParticipantReceivedEvent);
        EventDispatcher.getInstance().subscribe(AddParticipantsFailedEvent.class.getName(), callConnectionId, addParticipantFailedEvent);
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
        StringBuilder responseString;
        responseString = new StringBuilder("StatusCode: " + response.getStatusCode() + ", Headers: { ");

        for (HttpHeader header : response.getHeaders()) {
            responseString.append(header.getName()).append(":").append(header.getValue()).append(", ");
        }
        responseString.append("} ");
        return responseString.toString();
    }
}