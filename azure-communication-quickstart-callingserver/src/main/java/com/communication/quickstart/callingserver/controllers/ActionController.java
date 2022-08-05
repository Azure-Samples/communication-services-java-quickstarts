package com.communication.quickstart.callingserver.controllers;

import com.azure.communication.callingserver.CallConnection;
import com.azure.communication.callingserver.models.AddParticipantsOptions;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.CommunicationUserIdentifier;
import com.communication.quickstart.callingserver.CallAutomationClient;
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

        CallConnection callConnection = CallAutomationClient
                .getCallAutomationClient()
                .getCallConnection(root.get("callConnectionId").asText());

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
        } else {
            System.out.println("Invalid url");
            response.status(HttpStatus.NOT_FOUND_404);
        }
        return "";
    }
}
