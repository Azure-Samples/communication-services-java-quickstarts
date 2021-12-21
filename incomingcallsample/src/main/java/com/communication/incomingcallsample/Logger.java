package com.communication.incomingcallsample;

import com.azure.messaging.eventgrid.EventGridEvent;

public class Logger {
    //Caution: Logging should be removed/disabled if you want to use this sample in production to avoid exposing sensitive information
    public enum MessageType
    {
        INFORMATION,
        ERROR
    }

    /// <summary>
    /// Log message to console
    /// </summary>
    /// <param name="messageType">Type of the message: Information or Error</param>
    /// <param name="message">Message string</param>
    public static void logMessage(MessageType messageType, String message)
    {
        String logMessage;
        logMessage = messageType + " " + message;
        System.out.println(logMessage);
    }

    public static void logEventGridEvent(MessageType messageType, EventGridEvent eventGridEvent){
        String log = new StringBuilder()
                            .append(messageType + " ")
							.append("OnIncomingCall API POST request EventGridEvent---->")
							.append(" type: " + eventGridEvent.getEventType())
							.append(";")
							.append(" topic: " + eventGridEvent.getTopic())
							.append(";")
							.append(" subject: " + eventGridEvent.getSubject())
							.append(";")
							.append(" data: " + eventGridEvent.getData())
							.toString();
        System.out.println(log);
    }
}
