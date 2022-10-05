package com.communication.CallPlayAudio;

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
}
