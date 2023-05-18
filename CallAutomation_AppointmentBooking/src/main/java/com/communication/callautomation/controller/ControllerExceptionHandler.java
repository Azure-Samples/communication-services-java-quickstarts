package com.communication.callautomation.controller;

import com.communication.callautomation.exceptions.AzureCallAutomationException;
import com.communication.callautomation.exceptions.InvalidEventPayloadException;
import com.communication.callautomation.exceptions.MediaLoadingException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * Top level exception handler for rest controllers. This class makes sure that we don't leak any exception to the client and is also responsible for
 * mapping various exceptions to HTTP error codes.
 */
@RestControllerAdvice
@Slf4j
public class ControllerExceptionHandler {
    @ExceptionHandler(value = InvalidEventPayloadException.class)
    public ResponseEntity<String> handleInvalidEventPayloadException(final Exception exception) {
        log.warn("Invalid payload : {}", exception.getMessage());
        return buildExceptionResponse("Missing information in payload !", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(value = AzureCallAutomationException.class)
    public ResponseEntity<String> handleAzureCallAutomationException(final Exception exception) {
        log.warn("Remote server error : {}", exception.getMessage());
        return buildExceptionResponse("An error occurred while talking to a remote server !", HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(value = MediaLoadingException.class)
    public ResponseEntity<String> handleMediaLoadingException(final Exception exception) {
        log.warn("Server error : {}", exception.getMessage());
        return buildExceptionResponse("An error occurred in server IO operation !", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<String> handleGenericException(final Exception exception) {
        log.error("Unhandled exception occurred : {}", exception);
        return buildExceptionResponse("An unexpected error occurred !", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<String> buildExceptionResponse(final String message, final HttpStatus status) {
        return ResponseEntity.status(status).body(message);
    }
}
