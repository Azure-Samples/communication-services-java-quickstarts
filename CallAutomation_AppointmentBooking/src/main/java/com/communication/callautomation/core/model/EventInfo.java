package com.communication.callautomation.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder(toBuilder = true)
public class EventInfo {
    private String incomingCallContext;
    private String validationCode;
    private String topic;
    private String fromId;
    private String callConnectionId;
}
