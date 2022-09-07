package com.communication.quickstart.callautomation.controllers;

import com.azure.communication.callautomation.CallConnection;
import com.azure.communication.callautomation.CallMedia;
import com.azure.communication.callautomation.models.AddParticipantsOptions;
import com.azure.communication.callautomation.models.CreateCallOptions;
import com.azure.communication.callautomation.models.CreateCallResult;
import com.azure.communication.callautomation.models.FileSource;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.core.util.Context;
import com.communication.quickstart.callautomation.QueryCallAutomationClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ActionController {
    public Object onAction(Request request, Response response) throws JsonProcessingException {
        String action = request.params(":action");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(request.body());

        CallConnection callConnection = QueryCallAutomationClient
                .getCallAutomationClient()
                .getCallConnection(root.get("callConnectionId").asText());
        String callbackUri = QueryCallAutomationClient.getCallbackUrl();

        if (Objects.equals(action, "addParticipant")) {
            String targetMri = root.get("participant").asText();
            CommunicationUserIdentifier target = new CommunicationUserIdentifier(targetMri);
            List<CommunicationIdentifier> targets = new ArrayList<>(List.of(target));
            AddParticipantsOptions addParticipantsOptions = new AddParticipantsOptions(targets);

            callConnection.addParticipants(addParticipantsOptions);
            System.out.println("Added participant: " + targetMri);

        } else if (Objects.equals(action, "getParticipants")) {
            callConnection.listParticipants()
                    .getValues()
                    .forEach(callParticipant -> System.out.println(callParticipant.getIdentifier()));

        } else if (Objects.equals(action, "createCall")) {
            String sourceMri = root.get("source").asText();
            String targetMri = root.get("participant").asText();
            CommunicationUserIdentifier target = new CommunicationUserIdentifier(targetMri);
//            PhoneNumberIdentifier target = new PhoneNumberIdentifier(targetMri);
            List<CommunicationIdentifier> targets = new ArrayList<>(List.of(target));

            CreateCallOptions callOptions = new CreateCallOptions(new CommunicationUserIdentifier(sourceMri), targets, callbackUri);
//                    .setSourceCallerId("+18337597849");
            com.azure.core.http.rest.Response<CreateCallResult> result = QueryCallAutomationClient.getCallAutomationClient().createCallWithResponse(callOptions, Context.NONE);

        } else if (Objects.equals(action, "answerCall")) {
            // Answering the incoming call
            String incomingCallContext = root.get("incomingCallContext").asText();
            String callConnectionId = QueryCallAutomationClient
                    .getCallAutomationClient()
                    .answerCall(incomingCallContext, callbackUri)
                    .getCallConnectionProperties()
                    .getCallConnectionId();
            System.out.println("Call answered, callConnectionId: " + callConnectionId);

        } else if (Objects.equals(action, "playAudio")) {
            CallMedia callMedia = callConnection.getCallMedia();
            FileSource fileSource = new FileSource().setUri("https://mwlstoragetest.blob.core.windows.net/blobs1/languagesPrompt.wav");
            callMedia.playToAll(fileSource);
            System.out.println("Audio played");

        } else {
            System.out.println("Invalid url");
            response.status(HttpStatus.NOT_FOUND_404);
        }
        return "";
    }
}
