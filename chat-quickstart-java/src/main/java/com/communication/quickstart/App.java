// <Create a chat client>
package com.communication.quickstart;

import com.azure.communication.chat.*;
import com.azure.communication.chat.models.*;
import com.azure.communication.common.*;
import com.azure.core.http.rest.PagedIterable;

import java.io.*;
import java.util.*;

public class App
{
    public static void main( String[] args ) throws IOException
    {
        System.out.println("Azure Communication Services - Chat Quickstart");

        String endpoint = "https://<RESOURCE_NAME>.communication.azure.com";

        String userAccessToken = "<USER_ACCESS_TOKEN>";

        CommunicationTokenCredential userCredential = new CommunicationTokenCredential(userAccessToken);

        final ChatClientBuilder builder = new ChatClientBuilder();
        builder.endpoint(endpoint)
            .credential(userCredential);
        ChatClient chatClient = builder.buildClient();

        // <Start a chat thread>
        String userId1 = "<USER_Id1>";
        ChatParticipant firstThreadParticipant = new ChatParticipant()
        .setCommunicationIdentifier(new CommunicationUserIdentifier(userId1))
        .setDisplayName("Display Name 1");

        String userId2 = "<USER_Id2>";
        ChatParticipant secondThreadParticipant = new ChatParticipant()
        .setCommunicationIdentifier(new CommunicationUserIdentifier(userId2))
        .setDisplayName("Display Name 2");

        CreateChatThreadOptions createChatThreadOptions = new CreateChatThreadOptions("Topic")
        .addParticipant(firstThreadParticipant)
        .addParticipant(secondThreadParticipant);

        CreateChatThreadResult result = chatClient.createChatThread(createChatThreadOptions);
        String chatThreadId = result.getChatThread().getId();

        // <List chat threads>
        PagedIterable<ChatThreadItem> chatThreads = chatClient.listChatThreads();

        chatThreads.forEach(chatThread -> {
          System.out.printf("ChatThread id is %s.\n", chatThread.getId());
        });

        // <Get a chat thread client>
        ChatThreadClient chatThreadClient = chatClient.getChatThreadClient(chatThreadId);

        // <Send a message to a chat thread>
        SendChatMessageOptions sendChatMessageOptions = new SendChatMessageOptions()
        .setContent("Message content")
        .setType(ChatMessageType.TEXT)
        .setSenderDisplayName("Sender Display Name");

        SendChatMessageResult sendChatMessageResult = chatThreadClient.sendMessage(sendChatMessageOptions);
        String chatMessageId = sendChatMessageResult.getId();

        // <Receive chat messages from a chat thread>
        chatThreadClient.listMessages().forEach(message -> {
          System.out.printf("Message id is %s.\n", message.getId());
        });

        // <Send read receipt>
        chatThreadClient.sendReadReceipt(chatMessageId);

        // <List chat participants>
        PagedIterable<ChatParticipant> chatParticipantsResponse = chatThreadClient.listParticipants();
        chatParticipantsResponse.forEach(chatParticipant -> {
        System.out.printf("Participant id is %s.\n", ((CommunicationUserIdentifier) chatParticipant.getCommunicationIdentifier()).getId());
        });

        //<Add a user as participant to the chat thread>
        List<ChatParticipant> participants = new ArrayList<ChatParticipant>();

        String userId3 = "<USER_Id3>";
        ChatParticipant thirdThreadParticipant = new ChatParticipant()
        .setCommunicationIdentifier(new CommunicationUserIdentifier(userId3))
        .setDisplayName("Display Name 3");

        String userId4 = "<USER_Id4>";
        ChatParticipant fourthThreadParticipant = new ChatParticipant()
        .setCommunicationIdentifier(new CommunicationUserIdentifier(userId4))
        .setDisplayName("Display Name 4");

        participants.add(thirdThreadParticipant);
        participants.add(fourthThreadParticipant);

        chatThreadClient.addParticipants(participants);
    }
}
