package com.communication.rooms.quickstart;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.io.*;

import com.azure.communication.rooms.models.CommunicationRoom;
import com.azure.communication.rooms.models.RoomParticipant;
import com.azure.communication.rooms.implementation.models.CommunicationErrorResponseException;
import com.azure.communication.rooms.RoomsClient;
import com.azure.communication.rooms.models.RoomJoinPolicy;
import com.azure.communication.rooms.models.RoleType;
import com.azure.communication.rooms.RoomsClientBuilder;

import com.azure.communication.common.CommunicationUserIdentifier;

//import com.azure.core.util.Context;
/**
 * Hello Rooms!
 *
*/
public class App 
{
    public static RoomsClient createRoomsClientWithConnectionString() {
        String connectionString = "endpoint=https://rooms-ppe-us.ppe.communication.azure.net/;accesskey=ko1ym0w1QfSlP/uuuRqR5ZVtl04xZppxWLB+ssMQm/4XMhcXtAZqwWI4E2vHoq/lYda4TUUmKMNRbfhptFnLkQ==";
        
        RoomsClient roomsClient = new RoomsClientBuilder().connectionString(connectionString).buildClient();

        return roomsClient;
    }

    public static CommunicationRoom createRoom( RoomsClient roomsClient)
    {
        OffsetDateTime validFrom = OffsetDateTime.now();
        OffsetDateTime validUntil = validFrom.plusDays(30);
        RoomJoinPolicy roomJoinPolicy = RoomJoinPolicy.INVITE_ONLY;
        
        List<RoomParticipant> roomParticipants = new ArrayList<RoomParticipant>();

        RoomParticipant firstChangeParticipant = new RoomParticipant().setCommunicationIdentifier(new CommunicationUserIdentifier("8:acs:db75ed0c-e801-41a3-99a4-66a0a119a06c_00000010-ce28-064a-83fe-084822000669")).setRole(RoleType.CONSUMER);
        RoomParticipant secondChangeParticipant = new RoomParticipant().setCommunicationIdentifier(new CommunicationUserIdentifier("8:acs:db75ed0c-e801-41a3-99a4-66a0a119a06c_00000010-ce28-064a-83fe-084822000667")).setRole(RoleType.ATTENDEE);
        
        roomParticipants.add(firstChangeParticipant);
        roomParticipants.add(secondChangeParticipant);        
        return roomsClient.createRoom(validFrom, validUntil, roomJoinPolicy, roomParticipants);
    }

    public static boolean deleteRoom( RoomsClient roomsClient, String roomId)
    {
        try {
            System.out.println(roomId);
   //         roomsClient.deleteRoomWithResponse(roomId, Context.NONE);
        } catch (CommunicationErrorResponseException ex) {
            if (ex.getResponse().getStatusCode() == 404) {
                System.out.println("Room already deleted");
                return false;
            }
        }
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
            System.out.println("Participants: "+roomResult.getParticipants());
        } catch (Exception ex) {
            System.out.println(ex);
        }
        
    }

    public static void updateRoom( RoomsClient roomsClient, String roomId)
    {
        try {
            OffsetDateTime validFrom = OffsetDateTime.of(2022, 2, 1, 5, 30, 20, 10, ZoneOffset.UTC);
            OffsetDateTime validUntil = OffsetDateTime.of(2022, 5, 2, 5, 30, 20, 10, ZoneOffset.UTC);
//            Map<String, Object> participants = new HashMap();
//            participants.put("<CommunicationUserIdentifier.Id>", new RoomParticipant());  
//            participants.put("<CommunicationUserIdentifier.Id>", null);  
//            RoomRequest request = new RoomRequest();
//            request.setValidFrom(validFrom);
//            request.setValidUntil(validUntil);
//            request.setParticipants(participants);
//            
//            CommunicationRoom roomResult = roomsClient.updateRoom(roomId, request);
//        
//            System.out.println("RoomId: "+ roomResult.getRoomId());
//            System.out.println("Create at: "+roomResult.getCreatedTime());
//            System.out.println("ValidFrom: "+roomResult.getValidFrom());
//            System.out.println("ValidUntil: "+roomResult.getValidUntil());
//            System.out.println("Participants: "+roomResult.getParticipants());
        } catch (Exception ex) {
            System.out.println(ex);
        }
        
    }
    public static void main( String[] args )
    {
        RoomsClient roomsClient = createRoomsClientWithConnectionString();
        int selection;
//        Set<String> roomIds = new HashSet();
        
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
                System.out.println("6. Exit");
                selection = Integer.parseInt(br.readLine());
                switch (selection) {
                    case 1:
                        CommunicationRoom roomResult = createRoom(roomsClient);
                        System.out.println(roomResult.getRoomId());
                        getRoom(roomsClient, roomResult.getRoomId());
//                        roomIds.add(roomResult.getRoomId());
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
//                            roomIds.remove(roomId); 
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
//                        for (String room : roomIds) {
//                            System.out.println(room);
//                         
//                        }
                        break;
                    case 6:
                    
//                        System.out.println("Deleting all rooms");
//                        for (String room : roomIds) {
//                            System.out.println("Deleting:" + room);
//                            deleteRoom(roomsClient, room);
//                        }
                    
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