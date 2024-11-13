package com.communication.callautomation;
import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.communication.callautomation.CallAutomationAsyncClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.models.*;
import com.azure.communication.callautomation.models.events.*;
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
import java.util.*;

@RestController
@Slf4j
public class ProgramSample {
    private final AppConfig appConfig;
    private final CallAutomationAsyncClient asyncClient;
    private final OpenAIAsyncClient aiClient;
    private static final String INCOMING_CALL_CONTEXT = "incomingCallContext";

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
            MediaStreamingOptions mediaStreamingOptions = new MediaStreamingOptions(
        "",
        MediaStreamingTransport.WEBSOCKET,
        MediaStreamingContent.AUDIO,
        MediaStreamingAudioChannel.MIXED,
        false).setEnableBidirectional(true).setAudioFormat(AudioFormat.Pcm24KMono);
            callbackUri = String.format("%s/%s?callerId=%s",
                    appConfig.getCallBackUri(),
                    UUID.randomUUID(),
                    data.getJSONObject("from").getString("rawId"));
            cognitiveServicesUrl = new URI(appConfig.getCognitiveServicesUrl()).toString();
            CallIntelligenceOptions callIntelligenceOptions = new CallIntelligenceOptions()
                    .setCognitiveServicesEndpoint(appConfig.getCognitiveServicesUrl());
            options = new AnswerCallOptions(data.getString(INCOMING_CALL_CONTEXT),
                    callbackUri).setCallIntelligenceOptions(callIntelligenceOptions)
                    .setMediaStreamingOptions(mediaStreamingOptions);
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
