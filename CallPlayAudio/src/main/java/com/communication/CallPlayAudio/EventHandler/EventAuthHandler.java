package com.communication.CallPlayAudio.EventHandler;

import com.communication.CallPlayAudio.ConfigurationManager;

public class EventAuthHandler {
    private String secretValue;
    public static EventAuthHandler eventAuthHandler = null;

    public EventAuthHandler() {
        ConfigurationManager configuration = ConfigurationManager.getInstance();
        secretValue = configuration.getAppSettings("SecretPlaceholder");

        if (secretValue == null) {
            System.out.println("SecretPlaceholder is null");
            secretValue = "h3llowW0rld";
        }
    }

    public static EventAuthHandler getInstance() {
        if (eventAuthHandler == null) {
            eventAuthHandler = new EventAuthHandler();
        }
        return eventAuthHandler;
    }

    public Boolean authorize(String requestSecretValue) {
        return requestSecretValue != null && requestSecretValue.equals(secretValue);
    }

    public String getSecretQuerystring() {
        String secretKey = "secret";
        return (secretKey + "=" + secretValue);
    }
}