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
import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.rooms.RoomsClient;
import com.azure.communication.rooms.RoomsClientBuilder;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.Context;

public class App {
    static String USER_ID_1 = "<communication-user-id-1>";
    static String USER_ID_2 = "<communication-user-id-2>";
    static String USER_ID_3 = "<communication-user-id-3>";
    static RoomsClient roomsClient;

    public static RoomsClient createRoomsClientWithConnectionString() {
        String connectionString = "<connection-string>";

        RoomsClient roomsClient = new RoomsClientBuilder().connectionString(connectionString).buildClient();
        return roomsClient;
    }

    public static CommunicationRoom createRoom() {
        OffsetDateTime validFrom = OffsetDateTime.now();
        OffsetDateTime validUntil = validFrom.plusDays(30);

        List<RoomParticipant> roomParticipants = new ArrayList<RoomParticipant>();

        roomParticipants.add(new RoomParticipant(new CommunicationUserIdentifier(USER_ID_1)));
        roomParticipants
                .add(new RoomParticipant(new CommunicationUserIdentifier(USER_ID_2)).setRole(ParticipantRole.CONSUMER));

        CreateRoomOptions roomOptions = new CreateRoomOptions()
                .setValidFrom(validFrom)
                .setValidUntil(validUntil)
                .setParticipants(roomParticipants);

        return roomsClient.createRoom(roomOptions);

    }

    public static boolean deleteRoom(String roomId) {
        try {
            System.out.println(roomId);
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
            CommunicationRoom roomResult = roomsClient.getRoom(roomId);
            System.out.println("RoomId: " + roomResult.getRoomId());
            System.out.println("Create at: " + roomResult.getCreatedAt());

            System.out.println("ValidFrom: " + roomResult.getValidFrom());
            System.out.println("ValidUntil: " + roomResult.getValidUntil());
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
                    .setValidUntil(validUntil);

            CommunicationRoom roomResult = roomsClient.updateRoom(roomId, roomUpdateOptions);

            System.out.println("RoomId: " + roomResult.getRoomId());
            System.out.println("Create at: " + roomResult.getCreatedAt());
            System.out.println("ValidFrom: " + roomResult.getValidFrom());
            System.out.println("ValidUntil: " + roomResult.getValidUntil());
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public static void listRooms() {
        try {
            PagedIterable<CommunicationRoom> rooms = roomsClient.listRooms();
            for (CommunicationRoom room : rooms) {
                System.out.println("\nRoom ID: " + room.getRoomId());

            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public static void addOrUpdateParticipants(String roomId) {
        try {
            List<RoomParticipant> participantsToAddAndUpdate = new ArrayList<>();

            participantsToAddAndUpdate.add(
                    new RoomParticipant(new CommunicationUserIdentifier(USER_ID_3)).setRole(ParticipantRole.CONSUMER));

            // Updating current participant
            participantsToAddAndUpdate.add(
                    new RoomParticipant(new CommunicationUserIdentifier(USER_ID_2)).setRole(ParticipantRole.PRESENTER));

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

            participantsToRemove.add(new CommunicationUserIdentifier(USER_ID_2));

            RemoveParticipantsResult removeParticipantsResult = roomsClient.removeParticipants(roomId,
                    participantsToRemove);

            System.out.println("Participant(s) removed");
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public static void listParticipants(String roomId) {
        try {
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
                System.out.println("6. Add or Update participants");
                System.out.println("7. List rooms in resource");
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
                        for (String room : roomIds) {
                            System.out.println(room);
                        }
                        break;
                    }
                    case 6: {
                        System.out.print("RoomId:");
                        String roomId = br.readLine();
                        addOrUpdateParticipants(roomId);

                        break;
                    }
                    case 7: {
                        System.out.print("Listing all rooms");
                        listRooms();

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