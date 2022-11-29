package com.communication.MediaStreaming.WebSocketListener;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class App {
    public static void main(String[] args) throws IOException,
            NoSuchAlgorithmException {
        ServerSocket server = new ServerSocket(8080);
        Map<String, FileOutputStream> audioDataFiles = null;
        try {
            System.out.println("Server has started on 127.0.0.1:80.\r\nWaiting for a connection…");
            while (true) {
                Socket client = server.accept();
                System.out.println("A client connected.");
                if (audioDataFiles == null) {
                    audioDataFiles = new HashMap<String, FileOutputStream>();
                }
                InputStream ins = client.getInputStream();
                OutputStream out = client.getOutputStream();
                byte[] receiveInput = new byte[2048];
                ins.read(receiveInput, 0, receiveInput.length);
                String data = new String(receiveInput, StandardCharsets.UTF_8);
                System.out.println(data);
                Matcher get = Pattern.compile("^GET").matcher(data);

                if (get.find()) {
                    Matcher match = Pattern.compile("Sec-Websocket-Key: (.*)").matcher(data);
                    match.find();
                    String socket_key = match.group(1).split(" ")[0];
                    byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                            + "Connection: Upgrade\r\n"
                            + "Upgrade: websocket\r\n"
                            + "Sec-WebSocket-Accept: "
                            + Base64.getEncoder()
                                    .encodeToString(MessageDigest.getInstance("SHA-1").digest(
                                            (socket_key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")))
                            + "\r\n\r\n").getBytes("UTF-8");

                    out.write(response, 0, response.length);

                    // Checking for the data streaming
                    while (client.isConnected() && !client.isClosed()) {
                        InputStream in = client.getInputStream();
                        byte[] recvInput = new byte[client.getReceiveBufferSize()];
                        in.read(recvInput);
                        if ((recvInput[0] & 127) == 1) {
                            String decodedData = DecodeData(recvInput);
                            try {
                                if (decodedData != null) {
                                    Gson gson = new Gson();
                                    JsonReader reader = new JsonReader(new StringReader(decodedData));
                                    reader.setLenient(true);
                                    AudioDataPackets jsonData = gson.fromJson(reader, AudioDataPackets.class);

                                    if (jsonData != null && jsonData.kind.equals("AudioData")) {
                                        String dataAsString = jsonData.audioData.data;
                                        byte[] byteArray = dataAsString.getBytes();

                                        // generate file name and write data into file in dictionary
                                        String fileName = String.format("%s.txt", jsonData.audioData.participantRawID)
                                                .replace(":", "");
                                        FileOutputStream audioDataFileStream = null;

                                        if (audioDataFiles.containsKey(fileName)) {
                                            audioDataFileStream = audioDataFiles.getOrDefault(fileName, null);
                                        } else {
                                            audioDataFileStream = new FileOutputStream(fileName);
                                            audioDataFiles.put(fileName, audioDataFileStream);
                                        }
                                        audioDataFileStream.write(byteArray, 0, byteArray.length);
                                    }
                                }
                            } catch (Exception ex) {
                                System.out.println("Exception ->" + ex);
                            }
                        }

                    }
                }
                client.close();
            }
        } catch (Exception ex) {
            System.out.println(ex);
        } finally {
            for (Map.Entry<String, FileOutputStream> entry : audioDataFiles.entrySet()) {
                FileOutputStream value = entry.getValue();
                value.close();
            }
            audioDataFiles.clear();
        }
        server.close();
    }

    static String DecodeData(byte[] encodedData) {
        byte secondByte = encodedData[1];
        int length = secondByte & (127);
        int dataLength = 0;
        int indexFirstMask = 2;
        int extraBytes = 2;

        if (length == 126) {
            extraBytes = 8;
            indexFirstMask = 4; // if a special case, change indexFirstMask
        } else if (length == 127) {
            extraBytes = 14;
            indexFirstMask = 10;
        } else {
            dataLength = length;
        }

        for (int i = 2; i < indexFirstMask; i++) {
            dataLength = (dataLength << 8) + (encodedData[i] & 0xFF);
        }

        dataLength += extraBytes;
        byte[] masks = new byte[4];

        for (int i = 0; i < 4; i++) {
            masks[i] = encodedData[indexFirstMask + i];
        }

        int indexFirstDataByte = indexFirstMask + 4;
        byte[] decoded = new byte[dataLength];

        for (int i = indexFirstDataByte, j = 0; i < dataLength; i++, j++) {
            decoded[j] = (byte) (encodedData[i] ^ masks[j % 4]);
        }

        String dataStream = new String(decoded, StandardCharsets.UTF_8);
        return dataStream;
    }

    public class AudioDataPackets {
        public String kind;
        public AudioData audioData;
    }

    class AudioData {
        public String data; // Base64 Encoded audio buffer data
        public String timestamp; // In ISO 8601 format (yyyy-mm-ddThh:mm:ssZ)
        public String participantRawID;
        public boolean silent; // Indicates if the received audio buffer contains only silence.
    }
}