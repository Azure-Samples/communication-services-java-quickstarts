package com.communication.rooms.quickstart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.azure.communication.rooms.models.AddOrUpdateParticipantsResult;
import com.azure.communication.rooms.models.CommunicationRoom;
import com.azure.communication.rooms.models.CreateRoomOptions;
import com.azure.communication.rooms.models.ParticipantRole;
import com.azure.communication.rooms.models.RemoveParticipantsResult;
import com.azure.communication.rooms.models.RoomParticipant;
import com.azure.communication.rooms.models.UpdateRoomOptions;
import com.azure.communication.rooms.implementation.models.CommunicationErrorResponseException;
import com.azure.communication.common.CommunicationIdentifier;
import com.azure.communication.common.implementation.CommunicationConnectionString;
import com.azure.communication.rooms.RoomsClient;
import com.azure.communication.rooms.RoomsClientBuilder;
import com.azure.communication.identity.CommunicationIdentityClient;
import com.azure.communication.identity.CommunicationIdentityClientBuilder;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.Context;
import com.azure.core.credential.AzureKeyCredential;


public class App {
    static String CONNECTION_STRING = "<connection-string>";
    static RoomParticipant participant_1;
    static RoomParticipant participant_2;
    static RoomParticipant participant_3;

    static RoomsClient roomsClient;
    static CommunicationIdentityClient communicationClient;

    public static RoomsClient createRoomsClientWithConnectionString() {
        RoomsClient roomsClient = new RoomsClientBuilder().connectionString(CONNECTION_STRING).buildClient();

        return roomsClient;
    }

    
    public static CommunicationIdentityClientBuilder getCommunicationIdentityClientBuilder() {
        CommunicationIdentityClientBuilder builder = new CommunicationIdentityClientBuilder();
        CommunicationConnectionString connectionStringObject = new CommunicationConnectionString(CONNECTION_STRING);
        String endpoint = connectionStringObject.getEndpoint();
        String accessKey = connectionStringObject.getAccessKey();
        builder.endpoint(endpoint)
                .credential(new AzureKeyCredential(accessKey));

        return builder;
    }


    public static CommunicationRoom createRoom() {
        OffsetDateTime validFrom = OffsetDateTime.now();
        OffsetDateTime validUntil = validFrom.plusDays(30);

        List<RoomParticipant> roomParticipants = new ArrayList<RoomParticipant>();

        roomParticipants.add(participant_1);
        roomParticipants.add(participant_2.setRole(ParticipantRole.CONSUMER));
        
        System.out.print("Creating room...\n");

        CreateRoomOptions roomOptions = new CreateRoomOptions()
                .setValidFrom(validFrom)
                .setValidUntil(validUntil)
                .setPstnDialOutEnabled(false)
                .setParticipants(roomParticipants);

        return roomsClient.createRoom(roomOptions);

    }

    public static boolean deleteRoom(String roomId) {
        try {
            System.out.println(roomId);
            System.out.print("Removing room...\n");
            roomsClient.deleteRoomWithResponse(roomId, Context.NONE);
        } catch (CommunicationErrorResponseException ex) {
            if (ex.getResponse().getStatusCode() == 404) {
                System.out.println("Room already deleted");
                return false;
            }
        }
        System.out.println("Deleted");
        return true;
    }

    public static void getRoom(String roomId) {
        try {
            System.out.print("Getting room...\n");

            CommunicationRoom roomResult = roomsClient.getRoom(roomId);
            System.out.println("RoomId: " + roomResult.getRoomId());
            System.out.println("Created at: " + roomResult.getCreatedAt());

            System.out.println("ValidFrom: " + roomResult.getValidFrom());
            System.out.println("ValidUntil: " + roomResult.getValidUntil());
            System.out.println("PstnDialOutEnabled: " + roomResult.isPstnDialOutEnabled());
        } catch (Exception ex) {
            System.out.println(ex);
        }

    }

    public static void updateRoom(String roomId) {

        try {
            OffsetDateTime validFrom = OffsetDateTime.now().plusDays(1);
            OffsetDateTime validUntil = validFrom.plusDays(1);

            UpdateRoomOptions roomUpdateOptions = new UpdateRoomOptions()
                    .setValidFrom(validFrom)
                    .setValidUntil(validUntil)
                    .setPstnDialOutEnabled(true);

                    
            System.out.print("Updating room...\n");

            CommunicationRoom roomResult = roomsClient.updateRoom(roomId, roomUpdateOptions);
            

            System.out.println("RoomId: " + roomResult.getRoomId());
            System.out.println("Created at: " + roomResult.getCreatedAt());
            System.out.println("ValidFrom: " + roomResult.getValidFrom());
            System.out.println("ValidUntil: " + roomResult.getValidUntil());
            System.out.println("PstnDialOutEnabled: " + roomResult.isPstnDialOutEnabled());
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public static void listRooms() {
        try {
            PagedIterable<CommunicationRoom> rooms = roomsClient.listRooms();

            int count = 0;
            
            System.out.print("Listing all rooms");

            for (CommunicationRoom room : rooms) {
                System.out.println("\nFirst two room ID's in the list of rooms: " + room.getRoomId());
                count++;
                if (count >= 2) {
                    break;
                }
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public static void addOrUpdateParticipants(String roomId) {
        try {
            List<RoomParticipant> participantsToAddAndUpdate = new ArrayList<>();

            // Adding new participant
            participantsToAddAndUpdate.add(participant_3.setRole(ParticipantRole.CONSUMER));

            // Updating current participant
            participantsToAddAndUpdate.add(participant_2.setRole(ParticipantRole.PRESENTER));

            System.out.print("Adding/Updating participants())...\n");

            AddOrUpdateParticipantsResult addOrUpdateParticipantsResult = roomsClient.addOrUpdateParticipants(roomId,
                    participantsToAddAndUpdate);

            System.out.println("Participant(s) added/updated");
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public static void removeParticipant(String roomId) {
        try {
            List<CommunicationIdentifier> participantsToRemove = new ArrayList<>();

            participantsToRemove.add(participant_3.getCommunicationIdentifier());
            
            System.out.print("Removing participant(s)...\n");

            RemoveParticipantsResult removeParticipantsResult = roomsClient.removeParticipants(roomId,
                    participantsToRemove);

            System.out.println("Participant(s) removed");
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public static void listParticipants(String roomId) {
        try {
            System.out.print("Listing participant(s)...\n");

            PagedIterable<RoomParticipant> participants = roomsClient.listParticipants(roomId);

            for (RoomParticipant participant : participants) {
                System.out.println(
                        participant.getCommunicationIdentifier().getRawId() + " (" + participant.getRole() + ")");
            }

        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public static void main(String[] args) {
        roomsClient = createRoomsClientWithConnectionString();
        communicationClient = getCommunicationIdentityClientBuilder().buildClient();
        participant_1 = new RoomParticipant(communicationClient.createUser());
        participant_2 = new RoomParticipant(communicationClient.createUser());
        participant_3 = new RoomParticipant(communicationClient.createUser());
    
        int selection;
        Set<String> roomIds = new HashSet<>();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
            while (true) {

                System.out.println("Make a selection");
                System.out.println("1. Add a room");
                System.out.println("2. Update a room");
                System.out.println("3. Delete a room");
                System.out.println("4. Get room details");
                System.out.println("5. List room ids created in this session");
                System.out.println("6. List rooms in resource");
                System.out.println("7. Add or Update participants");
                System.out.println("8. Remove a participant");
                System.out.println("9. List participants");
                System.out.println("10. Exit");
                selection = Integer.parseInt(br.readLine());
                switch (selection) {
                    case 1:
                        CommunicationRoom roomResult = createRoom();
                        getRoom(roomResult.getRoomId());
                        roomIds.add(roomResult.getRoomId());
                        break;
                    case 2: {
                        System.out.print("RoomId:");
                        String roomId = br.readLine();
                        
                        updateRoom(roomId);
                        break;
                    }
                    case 3: {
                        System.out.print("RoomId:");
                        String roomId = br.readLine();
                        
                        if (deleteRoom(roomId)) {
                            roomIds.remove(roomId);
                        }
                        break;
                    }
                    case 4: {
                        System.out.print("RoomId:");
                        String roomId = br.readLine();
                        
                        getRoom(roomId);

                        break;
                    }
                    case 5: {
                        System.out.print("Listing room ids...\n");
                        for (String room : roomIds) {
                            System.out.println(room);
                        }
                        break;
                    }
                    case 6: {
                        listRooms();

                        break;
                    }
                    case 7: {
                        System.out.print("RoomId:");
                        String roomId = br.readLine();
                        
                        addOrUpdateParticipants(roomId);

                        break;
                    }
                    case 8: {
                        System.out.print("RoomId:");
                        String roomId = br.readLine();
                        
                        removeParticipant(roomId);

                        break;
                    }
                    case 9: {
                        System.out.print("RoomId:");
                        String roomId = br.readLine();
                        
                        listParticipants(roomId);

                        break;
                    }
                    case 10:
                        System.out.println("Deleting all rooms");
                        for (String room : roomIds) {
                            System.out.println("Deleting:" + room);
                            deleteRoom(room);
                        }

                        return;
                    default:
                        break;
                }

            }
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
}