package com.communication.callautomation.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class CallState {
    public static enum CallStateEnum {
        STARTED,
        ONRETRY,
        FINISHED;
    }
    private static CallStateEnum currentState;
    private static int retryCount;

    public static void incrementRetryCount() {
        retryCount++;
    }
    public static void resetCount() {
        retryCount = 0;
    }
    public static int getIncrementRetryCount() {
        return retryCount;
    }
    public static void setCallState(final CallStateEnum state){
        currentState = state;
    }
    public static CallStateEnum getCurrentState() {
        return currentState;
    }
}
