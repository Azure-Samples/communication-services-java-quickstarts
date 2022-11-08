package com.communication.recognizedtmf;

import com.azure.communication.callautomation.CallConnection;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.models.CallMediaRecognizeDtmfOptions;
import com.azure.communication.callautomation.models.CallMediaRecognizeOptions;
import com.azure.communication.callautomation.models.CreateCallOptions;
import com.azure.communication.callautomation.models.CreateCallResult;
import com.azure.communication.callautomation.models.FileSource;
import com.azure.communication.callautomation.models.HangUpOptions;
import com.azure.communication.callautomation.models.PlayOptions;
import com.azure.communication.callautomation.models.PlaySource;
import com.azure.communication.callautomation.models.DtmfTone;
import com.azure.communication.callautomation.models.events.CallConnected;
import com.azure.communication.callautomation.models.events.CallDisconnected;
import com.azure.communication.callautomation.models.events.PlayCanceled;
import com.azure.communication.callautomation.models.events.PlayCompleted;
import com.azure.communication.callautomation.models.events.PlayFailed;
import com.azure.communication.callautomation.models.events.RecognizeCanceled;
import com.azure.communication.callautomation.models.events.RecognizeCompleted;
import com.azure.communication.callautomation.models.events.RecognizeFailed;

import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;

import com.azure.cosmos.implementation.changefeed.CancellationToken;
import com.azure.cosmos.implementation.changefeed.CancellationTokenSource;
import com.communication.recognizedtmf.EventHandler.EventDispatcher;
import com.communication.recognizedtmf.EventHandler.NotificationCallback;
import com.azure.core.http.HttpHeader;
import com.azure.core.http.rest.Response;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RecognizeDtmf {

    private final CallConfiguration callConfiguration;
    private final CallAutomationClient callAutomationClient;
    private CallConnection callConnection = null;
    private CancellationTokenSource reportCancellationTokenSource;
    private CancellationToken reportCancellationToken;
    private CompletableFuture<Boolean> callConnectedTask;
    private CompletableFuture<Boolean> playAudioCompletedTask;
    private CompletableFuture<Boolean> callTerminatedTask;
    private CompletableFuture<Boolean> recognizeDtmfCompletedTask;
    private DtmfTone recognizedDtmfTone = DtmfTone.ZERO;

    public RecognizeDtmf(CallConfiguration callConfiguration) {
        this.callConfiguration = callConfiguration;
        this.callAutomationClient = new CallAutomationClientBuilder()
                .connectionString(this.callConfiguration.connectionString)
                .buildClient();
    }

    public void report(String targetPhoneNumber) {
        reportCancellationTokenSource = new CancellationTokenSource();
        reportCancellationToken = reportCancellationTokenSource.getToken();

        try {
            createCallAsync(targetPhoneNumber);
            registerToDtmfResultEvent(callConnection.getCallProperties().getCallConnectionId());
            startRecognizingDtmf(targetPhoneNumber);

            Boolean recognizeDtmfCompleted = recognizeDtmfCompletedTask.get();

            if (recognizeDtmfCompleted) {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Play Audio for input --> " + recognizedDtmfTone);
                playAudioAsInput();
                playAudioCompletedTask.get();
            } 

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
        EventDispatcher.getInstance().subscribe(CallDisconnected.class.getName(), callLegId,
                callDisconnectedNotificaiton);
    }

    private void registerToDtmfResultEvent(String callLegId) {
        recognizeDtmfCompletedTask = new CompletableFuture<>();

        NotificationCallback dtmfReceived = ((callEvent) -> {
            RecognizeCompleted toneReceivedEvent = (RecognizeCompleted) callEvent;
            List<DtmfTone> toneInfo = toneReceivedEvent.getCollectTonesResult().getTones();
            Logger.logMessage(Logger.MessageType.INFORMATION, "Tone received -- > : " + toneInfo);

            if (!toneInfo.isEmpty() && toneInfo.size() != 0) {
                this.recognizedDtmfTone = toneInfo.get(0);
            }
            EventDispatcher.getInstance().unsubscribe(RecognizeCompleted.class.getName(), callLegId);
            recognizeDtmfCompletedTask.complete(true);
        });

        NotificationCallback dtmfFailed = ((callEvent) -> {
            EventDispatcher.getInstance().unsubscribe(RecognizeFailed.class.getName(), callLegId);
            recognizeDtmfCompletedTask.complete(false);
        });

        NotificationCallback dtmfCanceled = ((callEvent) -> {
            EventDispatcher.getInstance().unsubscribe(RecognizeCanceled.class.getName(), callLegId);
            recognizeDtmfCompletedTask.complete(false);
        });

        // Subscribe to event
        EventDispatcher.getInstance().subscribe(RecognizeCompleted.class.getName(), callLegId, dtmfReceived);
        EventDispatcher.getInstance().subscribe(RecognizeFailed.class.getName(), callLegId, dtmfFailed);
        EventDispatcher.getInstance().subscribe(RecognizeCanceled.class.getName(), callLegId, dtmfCanceled);
    }

    private void startRecognizingDtmf(String targetPhoneNumber) {
        if (reportCancellationToken.isCancellationRequested()) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Cancellation request, PlayAudio will not be performed");
            return;
        }

        try {
            // Preparing data for request
            String audioFileUri = this.callConfiguration.audioFileUrl + this.callConfiguration.audioFileName;
            PlaySource playSource = new FileSource().setUri(audioFileUri);

            // listen to play audio events
            registerToPlayAudioResultEvent(this.callConnection.getCallProperties().getCallConnectionId());

            int maxTonesToCollect = 1;

            // Start recognizing Dtmf Tone
            CallMediaRecognizeOptions callMediaRecognizeOptions = new CallMediaRecognizeDtmfOptions(
                    new PhoneNumberIdentifier(targetPhoneNumber), maxTonesToCollect)
                    .setInterToneTimeout(Duration.ofSeconds(5))
                    .setInterruptCallMediaOperation(true)
                    .setInitialSilenceTimeout(Duration.ofSeconds(15))
                    .setPlayPrompt(playSource)
                    .setInterruptPrompt(true);

            Response<Void> startRecognizeResponse = this.callConnection.getCallMedia()
                    .startRecognizingWithResponse(callMediaRecognizeOptions, null);
            Logger.logMessage(Logger.MessageType.INFORMATION,
                    "Start Recognizing response-- > " + getResponse(startRecognizeResponse));
        } catch (Exception ex) {
            if (playAudioCompletedTask.isCancelled()) {
                Logger.logMessage(Logger.MessageType.INFORMATION,
                        "Start Recognizing with play audio prompt got cancelled");
            } else {
                Logger.logMessage(Logger.MessageType.INFORMATION,
                        "Failure occurred while start recognizing with Play audio prompt on the call. Exception: "
                                + ex.getMessage());
            }
        }
    }

    private void playAudioAsInput() {
        if (reportCancellationToken.isCancellationRequested()) {
            Logger.logMessage(Logger.MessageType.INFORMATION, "Cancellation request, PlayAudio will not be performed");
            return;
        }

        try {
            // Preparing data for request
            var audioFileName = callConfiguration.InvalidAudioFileName;

            if (recognizedDtmfTone == DtmfTone.ONE) {
                audioFileName = callConfiguration.SalesAudioFileName;
            } else if (recognizedDtmfTone == DtmfTone.TWO) {
                audioFileName = callConfiguration.MarketingAudioFileName;
            } else if (recognizedDtmfTone == DtmfTone.THREE) {
                audioFileName = callConfiguration.CustomerCareAudioFileName;
            }

            String audioFileUri = this.callConfiguration.audioFileUrl + audioFileName;
            PlaySource playSource = new FileSource().setUri(audioFileUri);
            PlayOptions playAudioOptions = new PlayOptions();
            playAudioOptions.setLoop(false);
            playAudioOptions.setOperationContext(UUID.randomUUID().toString());

            Logger.logMessage(Logger.MessageType.INFORMATION, "Performing PlayAudio operation");
            Response<Void> playAudioResponse = this.callConnection.getCallMedia().playToAllWithResponse(playSource,
                    playAudioOptions, null);

            Logger.logMessage(Logger.MessageType.INFORMATION,
                    "playAudioWithResponse -- > " + getResponse(playAudioResponse));

            if (playAudioResponse.getStatusCode() == 202) {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Play Audio state is running ");

                // listen to play audio events
                registerToPlayAudioResultEvent(this.callConnection.getCallProperties().getCallConnectionId());
            }
        } catch (Exception ex) {
            if (playAudioCompletedTask.isCancelled()) {
                Logger.logMessage(Logger.MessageType.INFORMATION, "Play audio operation cancelled");
            } else {
                Logger.logMessage(Logger.MessageType.INFORMATION,
                        "Failure occured while playing audio on the call. Exception: " + ex.getMessage());
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

    public String getResponse(Response<?> response) {
        StringBuilder responseString;
        responseString = new StringBuilder("StatusCode: " + response.getStatusCode() + ", Headers: { ");

        for (HttpHeader header : response.getHeaders()) {
            responseString.append(header.getName()).append(":").append(header.getValue()).append(", ");
        }
        responseString.append("} ");
        return responseString.toString();
    }
}