package com.communication.rooms.quickstart;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.*;

import com.azure.communication.rooms.models.CommunicationRoom;
import com.azure.communication.rooms.models.RoleType;
import com.azure.communication.rooms.models.RoomJoinPolicy;
import com.azure.communication.rooms.models.RoomParticipant;
import com.azure.communication.rooms.implementation.models.CommunicationErrorResponseException;
import com.azure.communication.rooms.models.ParticipantsCollection;
import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.rooms.RoomsClient;
import com.azure.communication.rooms.RoomsClientBuilder;
import com.azure.core.util.Context;
/**
 * Hello world!
 *
*/
public class App
{
    public static RoomsClient createRoomsClientWithConnectionString() {
        String connectionString = "endpoint=<connection string>";

        RoomsClient roomsClient = new RoomsClientBuilder().connectionString(connectionString).buildClient();

        return roomsClient;
    }

    public static CommunicationRoom createRoom( RoomsClient roomsClient)
    {
        OffsetDateTime validFrom = OffsetDateTime.now();
        OffsetDateTime validUntil = validFrom.plusDays(30);
        RoomJoinPolicy roomJoinPolicy = RoomJoinPolicy.INVITE_ONLY;

        List<RoomParticipant> roomParticipants = new ArrayList<RoomParticipant>();

        RoomParticipant firstChangeParticipant = new RoomParticipant()
                .setCommunicationIdentifier(
                    new CommunicationUserIdentifier("<communication user id 1>"))
                .setRole(RoleType.CONSUMER);
        RoomParticipant secondChangeParticipant = new RoomParticipant()
                .setCommunicationIdentifier(
                    new CommunicationUserIdentifier("communication user id "))
                .setRole(RoleType.ATTENDEE);
        roomParticipants.add(firstChangeParticipant);
        roomParticipants.add(secondChangeParticipant);

        return roomsClient.createRoom(
            validFrom,
            validUntil,
            roomJoinPolicy,
            roomParticipants
        );
    }

    public static boolean deleteRoom( RoomsClient roomsClient, String roomId)
    {
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

    public static void getRoom( RoomsClient roomsClient, String roomId)
    {
        try {
            CommunicationRoom roomResult = roomsClient.getRoom(roomId);
            System.out.println("RoomId: "+ roomResult.getRoomId());
            System.out.println("Create at: "+roomResult.getCreatedTime());
            System.out.println("ValidFrom: "+roomResult.getValidFrom());
            System.out.println("ValidUntil: "+roomResult.getValidUntil());
            System.out.println("Participants: "+participantsToString(roomResult.getParticipants()));
        } catch (Exception ex) {
            System.out.println(ex);
        }

    }

    public static void updateRoom(RoomsClient roomsClient, String roomId)
    {
        try {
            OffsetDateTime validFrom = OffsetDateTime.of(2022, 2, 1, 5, 30, 20, 10, ZoneOffset.UTC);
            OffsetDateTime validUntil = OffsetDateTime.of(2022, 5, 2, 5, 30, 20, 10, ZoneOffset.UTC);
            List<RoomParticipant> participants = new ArrayList<>();

            CommunicationRoom roomResult = roomsClient.updateRoom(
                roomId,
                validFrom,
                validUntil,
                null,
                participants
                );

            System.out.println("RoomId: "+ roomResult.getRoomId());
            System.out.println("Create at: "+roomResult.getCreatedTime());
            System.out.println("ValidFrom: "+roomResult.getValidFrom());
            System.out.println("ValidUntil: "+roomResult.getValidUntil());
            System.out.println("Participants: "+participantsToString(roomResult.getParticipants()));
        } catch (Exception ex) {
            System.out.println(ex);
        }

    }

    public static void addParticipant(RoomsClient roomsClient, String roomId)
    {
        try {
            RoomParticipant firstChangeParticipant = new RoomParticipant()
                .setCommunicationIdentifier(
                    new CommunicationUserIdentifier("communication user id 3"))
                .setRole(RoleType.CONSUMER);
            ParticipantsCollection updatedParticipants = roomsClient.addParticipants(roomId, List.of(firstChangeParticipant));
            System.out.println("Participants: " + participantsToString(updatedParticipants.getParticipants()));
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public static void updateParticipant(RoomsClient roomsClient, String roomId)
    {

    }

    public static void removeParticipant(RoomsClient roomsClient, String roomId)
    {

    }

    private static String participantsToString(List<RoomParticipant> participants)
    {
        return participants.stream().map
        (p -> "ID: " + p.getCommunicationIdentifier().getRawId()
                    + "\n Role: " + p.getRole()
        ).collect(Collectors.joining("\n"));
    }


    public static void main(String[] args)
    {
        RoomsClient roomsClient = createRoomsClientWithConnectionString();
        int selection;
        Set<String> roomIds = new HashSet<>();

        try
        {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        while(true)
        {

                System.out.println("Make a selection");
                System.out.println("1. Add a room");
                System.out.println("2. Update a room");
                System.out.println("3. Delete a room");
                System.out.println("4. Get room details");
                System.out.println("5. List room ids");
                System.out.println("6. Add a participant");
                System.out.println("7. Update a participant");
                System.out.println("8. Remove a participant");
                System.out.println("9. Exit");
                selection = Integer.parseInt(br.readLine());
                switch (selection) {
                    case 1:
                        CommunicationRoom roomResult = createRoom(roomsClient);
                        getRoom(roomsClient, roomResult.getRoomId());
                        roomIds.add(roomResult.getRoomId());
                        break;
                    case 2:
                    {
                        System.out.print("RoomId:");
                        String roomId = br.readLine();
                        updateRoom(roomsClient, roomId);
                        break;
                    }
                    case 3:
                    {
                        System.out.print("RoomId:");
                        String roomId = br.readLine();
                        if(deleteRoom(roomsClient, roomId))
                        {
                            roomIds.remove(roomId);
                        }
                        break;
                    }
                    case 4:
                    {
                        System.out.print("RoomId:");
                        String roomId = br.readLine();
                        getRoom(roomsClient, roomId);

                        break;
                    }
                    case 5:
                    {
                        for (String room : roomIds) {
                            System.out.println(room);

                        }
                        break;
                    }
                    case 6:
                    {
                        System.out.print("RoomId:");
                        String roomId = br.readLine();
                        addParticipant(roomsClient, roomId);

                        break;
                    }
                    case 7:
                    {
                        System.out.print("RoomId:");
                        String roomId = br.readLine();
                        updateParticipant(roomsClient, roomId);

                        break;
                    }
                    case 8:
                    {
                        System.out.print("RoomId:");
                        String roomId = br.readLine();
                        removeParticipant(roomsClient, roomId);

                        break;
                    }
                    case 9:

                        System.out.println("Deleting all rooms");
                        for (String room : roomIds) {
                            System.out.println("Deleting:" + room);
                            deleteRoom(roomsClient, room);
                        }

                        return;
                    default:
                        break;
                }

            }
        }
        catch (IOException ioe) {
            System.out.println(ioe);
         }

    }
}