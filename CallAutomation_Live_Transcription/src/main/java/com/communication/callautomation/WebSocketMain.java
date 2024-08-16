package com.communication.callautomation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.glassfish.tyrus.server.Server;

public class WebSocketMain {

    public static void main(String[] args) {

        Server server = new Server("localhost", 5001, "/ws", null, WebSocket.class);

        try {
            server.start();
            System.out.println("Web socket running on port 5001...");
            System.out.println("wss://localhost:5001/ws/server");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            reader.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
        }
    }
}
