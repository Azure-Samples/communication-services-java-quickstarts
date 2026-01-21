package com.communication;

public class ParticipantResponse {

    private String callConnectionId;
    private String correlationId;
    private String message;
    private String participantId;
    private boolean isOnHold;
    private boolean isMuted;

    public ParticipantResponse() {
    }

    public ParticipantResponse(String callConnectionId, String correlationId, String message, 
                             String participantId, boolean isOnHold, boolean isMuted) {
        this.callConnectionId = callConnectionId;
        this.correlationId = correlationId;
        this.message = message;
        this.participantId = participantId;
        this.isOnHold = isOnHold;
        this.isMuted = isMuted;
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