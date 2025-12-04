package com.communication;

import java.util.List;

public class ParticipantListResponse {

    private String callConnectionId;
    private String correlationId;
    private String message;
    private List<ParticipantInfo> participants;
    private int participantCount;

    public ParticipantListResponse() {
    }

    public ParticipantListResponse(String callConnectionId, String correlationId, String message, 
                                 List<ParticipantInfo> participants) {
        this.callConnectionId = callConnectionId;
        this.correlationId = correlationId;
        this.message = message;
        this.participants = participants;
        this.participantCount = participants != null ? participants.size() : 0;
    }

    public String getCallConnectionId() {
        return callConnectionId;
    }

    public void setCallConnectionId(String callConnectionId) {
        this.callConnectionId = callConnectionId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ParticipantInfo> getParticipants() {
        return participants;
    }

    public void setParticipants(List<ParticipantInfo> participants) {
        this.participants = participants;
        this.participantCount = participants != null ? participants.size() : 0;
    }

    public int getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }

    // Inner class for individual participant information
    public static class ParticipantInfo {
        private String participantId;
        private boolean isOnHold;
        private boolean isMuted;

        public ParticipantInfo() {
        }

        public ParticipantInfo(String participantId, boolean isOnHold, boolean isMuted) {
            this.participantId = participantId;
            this.isOnHold = isOnHold;
            this.isMuted = isMuted;
        }

        public String getParticipantId() {
            return participantId;
        }

        public void setParticipantId(String participantId) {
            this.participantId = participantId;
        }

        public boolean isOnHold() {
            return isOnHold;
        }

        public void setOnHold(boolean onHold) {
            isOnHold = onHold;
        }

        public boolean isMuted() {
            return isMuted;
        }

        public void setMuted(boolean muted) {
            isMuted = muted;
        }
    }
}