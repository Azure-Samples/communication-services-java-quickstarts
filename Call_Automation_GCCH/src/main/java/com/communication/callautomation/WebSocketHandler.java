package com.communication.callautomation;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.TextMessage;
import com.azure.communication.callautomation.StreamingData;
import com.azure.communication.callautomation.models.StreamingData;
import com.azure.communication.callautomation.models.TranscriptionData;
import com.azure.communication.callautomation.models.TranscriptionMetadata;
import com.azure.communication.callautomation.models.WordData;

public class WebSocketHandler extends TextWebSocketHandler {

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("Received message: " + payload);

        // Parse the message into StreamingData (custom data parsing logic)
        StreamingData data = StreamingData.parse(payload);
        

        // Handle TranscriptionMetadata
        if (data instanceof TranscriptionMetadata) {
            TranscriptionMetadata transcriptionMetadata = (TranscriptionMetadata) data;
            System.out.println("----------------------------------------------------------------");
            System.out.println("TRANSCRIPTION SUBSCRIPTION ID:-->" + transcriptionMetadata.getTranscriptionSubscriptionId());
            System.out.println("LOCALE:-->" + transcriptionMetadata.getLocale());
            System.out.println("CALL CONNECTION ID:-->" + transcriptionMetadata.getCallConnectionId());
            System.out.println("CORRELATION ID:-->" + transcriptionMetadata.getCorrelationId());
            System.out.println("----------------------------------------------------------------");
        }

        // Handle TranscriptionData
        if (data instanceof TranscriptionData) {
            TranscriptionData transcriptionData = (TranscriptionData) data;
            System.out.println("----------------------------------------------------------------");
            System.out.println("TEXT:-->" + transcriptionData.getText());
            System.out.println("FORMAT:-->" + transcriptionData.getFormat());
            System.out.println("CONFIDENCE:-->" + transcriptionData.getConfidence());
            System.out.println("OFFSET:-->" + transcriptionData.getOffset());
            System.out.println("DURATION:-->" + transcriptionData.getDuration());

            String participant = transcriptionData.getParticipant().getRawId() != null
                    ? transcriptionData.getParticipant().getRawId()
                    : "";
            System.out.println("PARTICIPANT:-->" + participant);
            System.out.println("RESULT STATUS:-->" + transcriptionData.getResultState());

            // Print word data (example of transcribed words)
            for (WordData word : transcriptionData.getTranscribedWords()) {
                System.out.println("TEXT:-->" + word.getText());
                System.out.println("OFFSET:-->" + word.getOffset());
                System.out.println("DURATION:-->" + word.getDuration());
            }
            System.out.println("----------------------------------------------------------------");
        }

        // Send an echo response back to the client
        session.sendMessage(new TextMessage("Echo: " + payload));
    }
}
