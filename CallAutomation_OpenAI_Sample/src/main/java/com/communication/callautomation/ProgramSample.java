package com.communication.callautomation;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.azure.communication.callautomation.CallAutomationAsyncClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.SystemEventNames;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationEventData;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@Slf4j
public class ProgramSample {
    private final AppConfig appConfig;
    private final CallAutomationAsyncClient asyncClient;
    private final OpenAIAsyncClient aiClient;
    Set<String> recognizeFails = new HashSet<>() {
    };
    private static final String INCOMING_CALL_CONTEXT = "incomingCallContext";

    private static final String answerPromptSystemTemplate = """
            You are an assisant designed to answer the customer query and analyze the sentiment score from the customer tone.
            You also need to determine the intent of the customer query and classify it into categories such as sales, marketing, shopping, etc.
            Use a scale of 1-10 (10 being highest) to rate the sentiment score.
            Use the below format, replacing the text in brackets with the result. Do not include the brackets in the output:
            Content:[Answer the customer query briefly and clearly in two lines and ask if there is anything else you can help with]
            Score:[Sentiment score of the customer tone]
            Intent:[Determine the intent of the customer query]
            Category:[Classify the intent into one of the categories]
            """;

    private final String helloPrompt = "Hello, thank you for calling! How can I help you today?";
    private final String timeoutSilencePrompt = "I’m sorry, I didn’t hear anything. If you need assistance please let me know how I can help you.";
    private final String goodbyePrompt = "Thank you for calling! I hope I was able to assist you. Have a great day!";
    private final String connectAgentPrompt = "I'm sorry, I was not able to assist you with your request. Let me transfer you to an agent who can help you further. Please hold the line and I'll connect you shortly.";
    private final String callTransferFailurePrompt = "It looks like all I can’t connect you to an agent right now, but we will get the next available agent to call you back as soon as possible.";
    private final String agentPhoneNumberEmptyPrompt = "I’m sorry, we're currently experiencing high call volumes and all of our agents are currently busy. Our next available agent will call you back as soon as possible.";
    private final String EndCallPhraseToConnectAgent = "Sure, please stay on the line. I’m going to transfer you to an agent.";

    private final String transferFailedContext = "TransferFailed";
    private final String connectAgentContext = "ConnectAgent";
    private final String goodbyeContext = "GoodBye";
    private final String chatResponseExtractPattern = "\\s*Content:(.*)\\s*Score:(.*\\d+)\\s*Intent:(.*)\\s*Category:(.*)";

    // private final String agentPhonenumber =
    // builder.Configuration.GetValue<string>("AgentPhoneNumber");
    // private final String chatResponseExtractPattern =
    // "\s*Content:(.*)\s*Score:(.*\d+)\s*Intent:(.*)\s*Category:(.*)";

    public ProgramSample(final AppConfig appConfig) {
        this.appConfig = appConfig;
        asyncClient = initAsyncClient();
        aiClient = initOpenAIClient();
    }

    @GetMapping(path = "/")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok().body("Hello! ACS CallAutomation OpenAI Sample!");
    }

    @PostMapping(path = "/api/incomingCall")
    public ResponseEntity<SubscriptionValidationResponse> recordinApiEventGridEvents(
            @RequestBody final String reqBody) {
        List<EventGridEvent> events = EventGridEvent.fromString(reqBody);
        for (EventGridEvent eventGridEvent : events) {
            if (eventGridEvent.getEventType().equals(SystemEventNames.EVENT_GRID_SUBSCRIPTION_VALIDATION)) {
                return handleSubscriptionValidation(eventGridEvent.getData());
            } else if (eventGridEvent.getEventType().equals(SystemEventNames.COMMUNICATION_INCOMING_CALL)) {
                handleIncomingCall(eventGridEvent.getData());
            } else {
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
                handleRecognizeRequest(helloPrompt, callConnectionId, callerId);
            } else if (event instanceof RecognizeCompleted) {
                log.info("Recognize Completed event received for Call Connection ID: {}", callConnectionId);
                RecognizeCompleted recognizeEvent = (RecognizeCompleted) event;
                SpeechResult speechResult = (SpeechResult) recognizeEvent
                        .getRecognizeResult().get();
                if (!speechResult.getSpeech().isEmpty()) {
                    String question = speechResult.getSpeech();
                    log.info("Speech recognized: {}", question);

                    DetectEscalateToAgentIntent(question).subscribe(escalate -> {
                        if (escalate) {
                            handlePlayTo(EndCallPhraseToConnectAgent, connectAgentContext,
                                    callConnectionId, callerId);
                        } else {
                            getChatGptResponse(question).subscribe(chatResponse -> {
                                log.info("Chat GPT response: {}", chatResponse);
                                Pattern pattern = Pattern
                                        .compile(chatResponseExtractPattern);
                                Matcher match = pattern.matcher(chatResponse);
                                if (match.find()) {
                                    String answer = match.group(1);
                                    String sentimentScore = match.group(2).trim();
                                    String intent = match.group(3);
                                    String category = match.group(4);
                                    log.info("Chat GPT Answer={}, Sentiment Rating={}, Intent={}, Category={}",
                                            answer, sentimentScore, intent, category);
                                    int score = getSentimentScore(sentimentScore);
                                    log.info("Sentiment Score={}", score);
                                    if (score > -1 && score < 5) {
                                        handlePlayTo(connectAgentPrompt,
                                                connectAgentContext,
                                                callConnectionId,
                                                callerId);
                                    } else {
                                        handleRecognizeRequest(answer, callConnectionId, callerId);
                                    }
                                } else {
                                    log.info("No match found", chatResponse);
                                    handleChatGptResponse(chatResponse, callConnectionId, callerId);
                                }
                            }, error -> {
                                log.error("Error getting Chat GPT response: {}", error.getMessage(), error);
                            });
                        }
                    }, error -> {
                        log.error("Error while detecting escalation intent: {}", error.getMessage(), error);
                    });

                } else {
                    log.error("Speech Recognition was empty or null");
                }
            } else if (event instanceof RecognizeFailed) {
                log.error("Received failed event: {}", ((CallAutomationEventBaseWithReasonCode) event)
                        .getResultInformation().getMessage());
                if (recognizeFails.contains(callConnectionId)) {
                    log.error("No input was recognized, hanging up call: {}", callConnectionId);
                    handlePlayTo(goodbyePrompt, goodbyeContext, callConnectionId, callerId);
                } else {
                    if (((CallAutomationEventBaseWithReasonCode) event)
                            .getResultInformation().getSubCode().toString()
                            .equals(ReasonCode.Recognize.INITIAL_SILENCE_TIMEOUT.toString())) {
                        log.error("Silence timeout triggered for Call Connection ID: {} {}", callConnectionId,
                                ((CallAutomationEventBaseWithReasonCode) event)
                                        .getResultInformation().getMessage());
                        recognizeFails.add(callConnectionId);
                        handleRecognizeRequest(timeoutSilencePrompt, callConnectionId, callerId);
                    } else {
                        recognizeFails.add(callConnectionId);
                        handleRecognizeRequest("Something went wrong. Want to try it again? How can I help?",
                                callConnectionId, callerId);
                    }
                }
            } else if (event instanceof CallTransferAccepted) {
                log.info("Call transfer accepted event received for connection id:{}", callConnectionId);
            } else if (event instanceof CallTransferFailed) {
                log.info("Call transfer failed event received for connection id:{}", callConnectionId);
                ResultInformation resultInformation = ((CallTransferFailed) event).getResultInformation();
                log.info("Encountered error during call transfer, message={}, code={}, subCode={}",
                        resultInformation.getMessage(), resultInformation.getCode(), resultInformation.getSubCode());
                handlePlayTo(callTransferFailurePrompt, transferFailedContext, callConnectionId, callerId);
            } else if (event instanceof PlayCompleted) {
                log.info("Received Play Completed event. Terminating call");
                if (!event.getOperationContext().isEmpty()
                        && (event.getOperationContext().equals(connectAgentContext) ||
                                event.getOperationContext().equals(connectAgentContext))) {
                    var agentPhoneNumber = appConfig.getAgentPhoneNumber();
                    if (agentPhoneNumber.isEmpty()) {
                        log.info("Empty agent phone number");
                        handlePlayTo(agentPhoneNumberEmptyPrompt, transferFailedContext, callConnectionId, callerId);
                    } else {
                        asyncClient.getCallConnectionAsync(callConnectionId)
                                .transferCallToParticipant(new PhoneNumberIdentifier(agentPhoneNumber)).subscribe();
                    }
                } else if (!event.getOperationContext().isEmpty()
                        && (event.getOperationContext().equals(transferFailedContext)
                                || event.getOperationContext().equals(goodbyeContext))) {
                    recognizeFails.remove(callConnectionId);
                    hangUp(callConnectionId);
                }
            } else if (event instanceof PlayFailed) {
                log.error("Received Play Failed event: {}", ((CallAutomationEventBaseWithReasonCode) event)
                        .getResultInformation().getMessage());
                recognizeFails.remove(callConnectionId);
                hangUp(callConnectionId);
            } else if (event instanceof CallDisconnected) {
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
            CallIntelligenceOptions callIntelligenceOptions = new CallIntelligenceOptions()
                    .setCognitiveServicesEndpoint(appConfig.getCognitiveServicesUrl());
            options = new AnswerCallOptions(data.getString(INCOMING_CALL_CONTEXT),
                    callbackUri).setCallIntelligenceOptions(callIntelligenceOptions);
            Mono<Response<AnswerCallResult>> answerCallResponse = asyncClient.answerCallWithResponse(options);
            answerCallResponse.subscribe(response -> {
                log.info("Incoming call answered. Cognitive Services Url: {}\nCallbackUri: {}\nCallConnectionId: {}",
                        cognitiveServicesUrl,
                        callbackUri,
                        response.getValue().getCallConnectionProperties().getCallConnectionId());
            });
        } catch (Exception e) {
            log.error("Error getting recording location info {} {}",
                    e.getMessage(),
                    e.getCause());
        }
    }

    private ResponseEntity<SubscriptionValidationResponse> handleSubscriptionValidation(final BinaryData eventData) {
        try {
            log.info("Received Subscription Validation Event from Incoming Call API endpoint");
            SubscriptionValidationEventData subscriptioneventData = eventData
                    .toObject(SubscriptionValidationEventData.class);
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
            final String callerId) {
        String targetParticipant = callerId.replaceAll("\\s", "+");
        TextSource textSource = new TextSource()
                .setText(message)
                .setVoiceName("en-US-NancyNeural");
        CallMediaRecognizeSpeechOptions options = new CallMediaRecognizeSpeechOptions(
                CommunicationIdentifier.fromRawId(targetParticipant),
                Duration.ofSeconds(5))
                .setInterruptPrompt(false)
                .setPlayPrompt(textSource)
                .setOperationContext("OpenAISample")
                .setInitialSilenceTimeout(Duration.ofSeconds(15));
        try {
            asyncClient.getCallConnectionAsync(callConnectionId)
                    .getCallMediaAsync()
                    .startRecognizingWithResponse(options).subscribe();
        } catch (Exception e) {
            log.error("Error occurred when starting Recognize to participant {}: {} {}", targetParticipant,
                    e.getMessage(), e.getCause());
        }
    }

    private int getSentimentScore(String sentimentScore) {
        String pattern = "(\\d)+";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(sentimentScore);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            } catch (NumberFormatException e) {
                System.out.println("Parsing failed");
                return -1;
            }
        } else {
            System.out.println("No match found");
            return -1;
        }
    }

    private Mono<String> getChatCompletionsAsync(final String systemPrompt, final String userPrompt) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage(ChatRole.SYSTEM, systemPrompt));
        chatMessages.add(new ChatMessage(ChatRole.USER, userPrompt));

        ChatCompletionsOptions chatCompletionsOptions = new ChatCompletionsOptions(chatMessages).setMaxTokens(1000);
        String openAiModelName = appConfig.getOpenAiModelName();

        return aiClient.getChatCompletions(openAiModelName, chatCompletionsOptions)
                .map(chatCompletion -> chatCompletion.getChoices().get(0).getMessage().getContent())
                .doOnError(e -> log.error("Error when using Open AI API {} {}", e.getMessage(), e.getCause()));
    }

    private Mono<Boolean> DetectEscalateToAgentIntent(String speechText) {
        return HasIntentAsync(speechText, "talk to agent");
    }

    private Mono<Boolean> HasIntentAsync(String userQuery, String intentDescription) {
        String systemPrompt = "You are a helpful assistant";
        String baseUserPrompt = "In 1 word: does %s have similar meaning as %s?";
        String combinedPrompt = String.format(baseUserPrompt, userQuery, intentDescription);

        return getChatCompletionsAsync(systemPrompt, combinedPrompt)
                .map(response -> {
                    boolean isMatch = response.toLowerCase().contains("yes");
                    log.info("OpenAI results: isMatch={}, customerQuery='{}', intentDescription='{}'", isMatch,
                            userQuery, intentDescription);
                    return isMatch;
                });
    }

    private Mono<String> getChatGptResponse(final String speech) {
        return getChatCompletionsAsync(answerPromptSystemTemplate, speech);
    }

    private void handleChatGptResponse(final String chatResponse,
            final String callConnectionId,
            final String callerId) {
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
                .setOperationContext("OpenAISample")
                .setInitialSilenceTimeout(Duration.ofSeconds(15));
        try {
            asyncClient.getCallConnectionAsync(callConnectionId)
                    .getCallMediaAsync()
                    .startRecognizingWithResponse(options).subscribe();
        } catch (Exception e) {
            log.error("Error while handling Chat GPT response {} {}",
                    e.getMessage(),
                    e.getCause());
        }
    }

    private void handlePlayTo(final String textToPlay,
            final String context,
            final String callConnectionId,
            final String callerId) {

        String tParticipant = callerId.replaceAll("\\s", "+");
        CommunicationIdentifier targetParticipant = CommunicationIdentifier.fromRawId(tParticipant);
        PlaySource playSource = new TextSource()
                .setText(textToPlay)
                .setVoiceName("en-US-NancyNeural");
        PlayOptions playOptions = new PlayOptions(playSource, new ArrayList<>(List.of(targetParticipant)))
                .setOperationContext(context);

        try {
            asyncClient.getCallConnectionAsync(callConnectionId)
                    .getCallMediaAsync()
                    .playWithResponse(playOptions).subscribe();
        } catch (Exception e) {
            log.error("Error occurred when playing media to participant {} {}", e.getMessage(), e.getCause());
        }
    }

    private void hangUp(final String callConnectionId) {
        try {
            asyncClient.getCallConnectionAsync(callConnectionId).hangUpWithResponse(true).subscribe();
            log.info("Terminated call");
        } catch (Exception e) {
            log.error("Error when terminating the call for all participants {} {}", e.getMessage(), e.getCause());
        }
    }

    private OpenAIAsyncClient initOpenAIClient() {
        OpenAIAsyncClient aiClient;
        String key;
        String endpoint;
        try {
            key = appConfig.getAzureOpenAiServiceKey();
            endpoint = appConfig.getAzureOpenAiServiceEndpoint();

            aiClient = new OpenAIClientBuilder()
                    .credential(new AzureKeyCredential(key))
                    .endpoint(endpoint)
                    .buildAsyncClient();
            return aiClient;

        } catch (NullPointerException e) {
            log.error("Please verify if Application config is properly set up");
            return null;
        } catch (Exception e) {
            log.error("Error occurred when initializing open ai Async Client: {} {}",
                    e.getMessage(),
                    e.getCause());
            return null;
        }
    }

    private CallAutomationAsyncClient initAsyncClient() {
        CallAutomationAsyncClient client;
        try {
            client = new CallAutomationClientBuilder()
                    .connectionString(appConfig.getConnectionString())
                    .buildAsyncClient();
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
