package com.communication.callautomation.exceptions;

/**
 * Thrown when an error happens at the controller from incoming payload.
 */
public class InvalidEventPayloadException extends BaseException {

    public InvalidEventPayloadException(String message) { super(message); }

    public InvalidEventPayloadException(String message, Throwable cause) { super(message, cause); }
}
