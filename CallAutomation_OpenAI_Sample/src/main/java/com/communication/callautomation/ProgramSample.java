package com.communication.callautomation;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.SystemEventNames;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationEventData;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.util.*;

@RestController
@Slf4j
public class ProgramSample {
    private final AppConfig appConfig;
    private final CallAutomationClient client;
    Set<String> recognizeFails = new HashSet<>(){};
    private static final String INCOMING_CALL_CONTEXT = "incomingCallContext";

    public ProgramSample(final AppConfig appConfig) {
        this.appConfig = appConfig;
        client = initClient();
    }

    @GetMapping(path = "/")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok().body("Hello! ACS CallAutomation OpenAI Sample!");
    }

    @PostMapping(path = "/api/incomingCall")
    public ResponseEntity<SubscriptionValidationResponse> recordinApiEventGridEvents(@RequestBody final String reqBody) {
        List<EventGridEvent> events = EventGridEvent.fromString(reqBody);
        for (EventGridEvent eventGridEvent : events) {
            if (eventGridEvent.getEventType().equals(SystemEventNames.EVENT_GRID_SUBSCRIPTION_VALIDATION)) {
               return handleSubscriptionValidation(eventGridEvent.getData());
            }
            else if (eventGridEvent.getEventType().equals(SystemEventNames.COMMUNICATION_INCOMING_CALL)) {
                handleIncomingCall(eventGridEvent.getData());
            }
            else {
                log.debug("Unhandled event.");
            }
        }
        return ResponseEntity.ok().body(null);
    }

    @PostMapping(path = "/api/callback/{contextId}")
    public ResponseEntity<String> callbackEvents(@RequestBody final String reqBody,
                                                 @PathVariable final String contextId,
                                                 @RequestParam final String callerId) {
        List<CallAutomationEventBase> events = CallAutomationEventParser.parseEvents(reqBody);
        for (CallAutomationEventBase event : events) {
            String callConnectionId = event.getCallConnectionId();
            if (event instanceof CallConnected) {
                log.info("Call connected performing recognize for Call Connection ID: {}", callConnectionId);
                handleRecognizeRequest("Hello. How can I help?", callConnectionId, callerId);
            }
            else if (event instanceof RecognizeCompleted) {
                log.info("Recognize Completed event received for Call Connection ID: {}", callConnectionId);
                RecognizeCompleted recognizeEvent = (RecognizeCompleted) event;
                SpeechResult speechResult = (SpeechResult) recognizeEvent
                        .getRecognizeResult().get();

                if (!speechResult.getSpeech().isEmpty())
                {
                    String question = speechResult.getSpeech();
                    log.info("Speech recognized: {}", question);
                    String chatResponse = getChatGptResponse(question);
                    handleChatGptResponse(
                            chatResponse,
                            callConnectionId,
                            callerId);
                }
                else
                {
                    log.error("Speech Recognition was empty or null");
                }
            }
            else if(event instanceof RecognizeFailed) {
                log.error("Received failed event: {}", ((CallAutomationEventBaseWithReasonCode) event)
                        .getResultInformation().getMessage());
                if (recognizeFails.contains(callConnectionId))
                {
                    log.error("No input was recognized, hanging up call: {}", callConnectionId);
                    handlePlayTo(callConnectionId, callerId);
                }
                else
                {
                    if (((CallAutomationEventBaseWithReasonCode) event)
                            .getResultInformation().getMessage()
                            .contains("Action failed, initial silence timeout reached"))
                    {
                        log.error("Silence timeout triggered for Call Connection ID: {} {}", callConnectionId, ((CallAutomationEventBaseWithReasonCode) event)
                                .getResultInformation().getMessage());
                        recognizeFails.add(callConnectionId);
                        handleRecognizeRequest("Is there anybody there?", callConnectionId, callerId);
                    }
                    else
                    {
                        recognizeFails.add(callConnectionId);
                        handleRecognizeRequest("Something went wrong. Want to try it again? How can I help?", callConnectionId, callerId);
                    }
                }
            }
            else if(event instanceof PlayCompleted) {
                log.info("Received Play Completed event. Terminating call");
                recognizeFails.remove(callConnectionId);
                hangUp(callConnectionId);
            }
            else if(event instanceof PlayFailed) {
                log.error("Received Play Failed event: {}", ((CallAutomationEventBaseWithReasonCode) event)
                        .getResultInformation().getMessage());
            }
            else if(event instanceof CallDisconnected) {
                log.info("Received Call Disconnected event for Call Connection ID: {}", callConnectionId);
            }
        }
        return ResponseEntity.ok().body("");
    }

    private void handleIncomingCall(final BinaryData eventData) {
        JSONObject data = new JSONObject(eventData.toString());
        String callbackUri;
        AnswerCallOptions options;
        String cognitiveServicesUrl;

        try {
            callbackUri = String.format("%s/%s?callerId=%s",
                    appConfig.getCallBackUri(),
                    UUID.randomUUID(),
                    data.getJSONObject("from").getString("rawId"));
            cognitiveServicesUrl = new URI(appConfig.getCognitiveServicesUrl()).toString();
            options = new AnswerCallOptions(data.getString(INCOMING_CALL_CONTEXT),
                    callbackUri).setCognitiveServicesEndpoint(cognitiveServicesUrl);
            Response<AnswerCallResult> answerCallResponse = client.answerCallWithResponse(options, Context.NONE);

            log.info("Incoming call answered. Cognitive Services Url: {}\nCallbackUri: {}\nCallConnectionId: {}",
                    cognitiveServicesUrl,
                    callbackUri,
                    answerCallResponse.getValue().getCallConnectionProperties().getCallConnectionId());
        } catch (Exception e) {
            log.error("Error getting recording location info {} {}",
                    e.getMessage(),
                    e.getCause());
        }
    }

    private ResponseEntity<SubscriptionValidationResponse> handleSubscriptionValidation(final BinaryData eventData) {
        try {
            log.info("Received Subscription Validation Event from Incoming Call API endpoint");
            SubscriptionValidationEventData subscriptioneventData = eventData.toObject(SubscriptionValidationEventData.class);
            SubscriptionValidationResponse responseData = new SubscriptionValidationResponse();
            responseData.setValidationResponse(subscriptioneventData.getValidationCode());
            return ResponseEntity.ok().body(responseData);
        } catch (Exception e) {
            log.error("Error at subscription validation event {} {}",
                    e.getMessage(),
                    e.getCause());
            return ResponseEntity.internalServerError().body(null);
        }
    }

    private void handleRecognizeRequest(
            final String message,
            final String callConnectionId,
            final String callerId)
    {
        String targetParticipant = callerId.replaceAll("\\s", "+");
        TextSource textSource = new TextSource()
                .setText(message)
                .setVoiceName("en-US-NancyNeural");
        CallMediaRecognizeSpeechOptions options = new CallMediaRecognizeSpeechOptions(
                CommunicationIdentifier.fromRawId(targetParticipant),
                Duration.ofSeconds(5))
                .setInterruptPrompt(false)
                .setPlayPrompt(textSource)
                .setOperationContext("GetFreeFormText")
                .setInitialSilenceTimeout(Duration.ofSeconds(15));
        try
        {
            client.getCallConnection(callConnectionId)
                    .getCallMedia()
                    .startRecognizing(options);
        } catch (Exception e)
        {
            log.error("Error occurred when starting Recognize to participant {}: {} {}",
                    targetParticipant,
                    e.getMessage(),
                    e.getCause());
        }
    }

    private String getChatGptResponse(final String speech)
    {
        String key;
        String openAiModelName;
        String endpoint;
        OpenAIClient aiClient;
        ChatCompletionsOptions chatCompletionsOptions;
        List<ChatMessage> chatMessages = new ArrayList<>(){};
        try
        {
            key = appConfig.getAzureOpenAiServiceKey();
            endpoint = appConfig.getAzureOpenAiServiceEndpoint();
            openAiModelName = appConfig.getOpenAiModelName();
            aiClient = new OpenAIClientBuilder()
                    .endpoint(new URI(endpoint).toString())
                    .credential(new AzureKeyCredential(key))
                    .buildClient();
            chatMessages.add(new ChatMessage(ChatRole.SYSTEM, "You are a helpful assistant."));
            chatMessages.add(new ChatMessage(ChatRole.USER, String.format("In less than 200 characters: respond to this question: %s", speech)));
            chatCompletionsOptions = new ChatCompletionsOptions(chatMessages).setMaxTokens(1000);

            ChatCompletions chatCompletion = aiClient
                    .getChatCompletions(openAiModelName,
                            chatCompletionsOptions);

            return chatCompletion.getChoices().get(0).getMessage().getContent();
        } catch (Exception e)
        {
            log.error("Error when using Open AI API {} {}",
                    e.getMessage(),
                    e.getCause());
            return null;
        }
    }

    private void handleChatGptResponse(final String chatResponse,
                                                 final String callConnectionId,
                                                 final String callerId)
    {
        String targetParticipant = callerId.replaceAll("\\s", "+");
        String prompt = String.format("%s %s", chatResponse, "Is there anything else I can help you with?");
        TextSource textSource = new TextSource()
                .setText(prompt)
                .setVoiceName("en-US-NancyNeural");
        CallMediaRecognizeSpeechOptions options = new CallMediaRecognizeSpeechOptions(
                CommunicationIdentifier.fromRawId(targetParticipant),
                Duration.ofSeconds(5))
                .setInterruptPrompt(false)
                .setPlayPrompt(textSource)
                .setOperationContext("GetFreeFormText")
                .setInitialSilenceTimeout(Duration.ofSeconds(15));
        try
        {
            client.getCallConnection(callConnectionId)
                    .getCallMedia()
                    .startRecognizing(options);
        } catch (Exception e)
        {
            log.error("Error while handling Chat GPT response {} {}",
                    e.getMessage(),
                    e.getCause());
        }
    }

    private void handlePlayTo(
            final String callConnectionId,
            final String callerId) {

        String tParticipant = callerId.replaceAll("\\s", "+");
        PlaySource playSource = new TextSource()
                .setText("Goodbye!")
                .setVoiceName("en-US-NancyNeural");
        CommunicationIdentifier targetParticipant = CommunicationIdentifier.fromRawId(tParticipant);

        try {
            client.getCallConnection(callConnectionId)
                    .getCallMedia()
                    .play(playSource, new ArrayList<>(List.of(targetParticipant)));
        } catch (Exception e) {
            log.error("Error occurred when playing media to participant {} {}",
                    e.getMessage(),
                    e.getCause());
        }
    }

    private void hangUp(final String callConnectionId) {
        try {
            client.getCallConnection(callConnectionId).hangUp(true);
            log.info("Terminated call");
        } catch (Exception e) {
            log.error("Error when terminating the call for all participants {} {}",
                    e.getMessage(),
                    e.getCause());
        }
    }

    private CallAutomationClient initClient() {
        CallAutomationClient client;
        try {
            client = new CallAutomationClientBuilder()
                    .connectionString(appConfig.getConnectionString())
                    .buildClient();
            return client;
        } catch (NullPointerException e) {
            log.error("Please verify if Application config is properly set up");
            return null;
        } catch (Exception e) {
            log.error("Error occurred when initializing Call Automation Async Client: {} {}",
                    e.getMessage(),
                    e.getCause());
            return null;
        }
    }
}
