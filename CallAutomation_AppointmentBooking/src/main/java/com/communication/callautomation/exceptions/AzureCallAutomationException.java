package com.communication.callautomation.exceptions;

/**
 * Thrown when an error happens while interacting with ACS through the CallAutomation SDK.
 */
public class AzureCallAutomationException extends BaseException {
    /**
     * @param message
     */
    public AzureCallAutomationException(final String message) { super(message); }

    /**
     * @param message
     * @param cause
     */
    public AzureCallAutomationException(final String message, final Throwable cause) { super(message, cause); }
}
