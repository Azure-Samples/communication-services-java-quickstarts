package com.communication.callautomation;

import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.azure.communication.callautomation.StreamingDataParser;
import com.azure.communication.callautomation.models.StreamingData;
import com.azure.communication.callautomation.models.TranscriptionData;
import com.azure.communication.callautomation.models.TranscriptionMetadata;
import com.azure.communication.callautomation.models.WordData;

@ServerEndpoint("/server")
public class WebSocket {
    @OnMessage
    public void onMessage(String message, Session session) {

        System.out.println("Received message: " + message);

        StreamingData data = StreamingDataParser.parse(message);

        if (data instanceof TranscriptionMetadata) {
            System.out.println("----------------------------------------------------------------");
            TranscriptionMetadata transcriptionMetadata = (TranscriptionMetadata) data;
            System.out.println(
                    "TRANSCRIPTION SUBSCRIPTION ID:-->" + transcriptionMetadata.getTranscriptionSubscriptionId());
            System.out.println("LOCALE:-->" + transcriptionMetadata.getLocale());
            System.out.println("CALL CONNECTION ID:-->" + transcriptionMetadata.getCallConnectionId());
            System.out.println("CORRELATION ID:-->" + transcriptionMetadata.getCorrelationId());
            System.out.println("----------------------------------------------------------------");
        }
        if (data instanceof TranscriptionData) {
            System.out.println("----------------------------------------------------------------");
            TranscriptionData transcriptionData = (TranscriptionData) data;
            System.out.println("TEXT:-->" + transcriptionData.getText());
            System.out.println("FORMAT:-->" + transcriptionData.getFormat());
            System.out.println("CONFIDENCE:-->" + transcriptionData.getConfidence());
            System.out.println("OFFSET:-->" + transcriptionData.getOffset());
            System.out.println("DURATION:-->" + transcriptionData.getDuration());
            var participant = transcriptionData.getParticipant().getRawId() != null
                    ? transcriptionData.getParticipant().getRawId()
                    : "";
            System.out.println("PARTICIPANT:-->" + participant);
            System.out.println("RESULT STATUS:-->" +
                    transcriptionData.getResultStatus());
            for (WordData word : transcriptionData.getTranscripeWords()) {
                System.out.println("TEXT:-->" + word.getText());
                System.out.println("OFFSET:-->" + word.getOffset());
                System.out.println("DURATION:-->" + word.getDuration());
            }
            System.out.println("----------------------------------------------------------------");
        }
    }
}
