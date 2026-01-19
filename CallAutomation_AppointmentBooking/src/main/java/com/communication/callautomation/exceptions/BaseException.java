package com.communication.callautomation.exceptions;

public class BaseException extends RuntimeException {
    public BaseException(final String message) { super(message); }

    public BaseException(final String message, Throwable cause) { super(message, cause); }
}
