package com.communication;

public class CallResponse {

    private String callConnectionId;
    private String correlationId;
    private String message;
    private String recordingId;

    public CallResponse() {
    }

    public CallResponse(String callConnectionId, String correlationId, String message) {
        this.callConnectionId = callConnectionId;
        this.correlationId = correlationId;
        this.message = message;
    }

    public CallResponse(String callConnectionId, String correlationId, String message, String recordingId) {
        this.callConnectionId = callConnectionId;
        this.correlationId = correlationId;
        this.message = message;
        this.recordingId = recordingId;
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

    public String getRecordingId() {
        return recordingId;
    }

    public void setRecordingId(String recordingId) {
        this.recordingId = recordingId;
    }
}
