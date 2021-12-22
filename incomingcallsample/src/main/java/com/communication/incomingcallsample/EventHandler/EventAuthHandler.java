package com.communication.incomingcallsample.EventHandler;

public class EventAuthHandler {
    private final String secretValue;
    private final String secreteKey = "secret";

    public EventAuthHandler(String secretValue){
        this.secretValue = secretValue;
    }

    public String GetSecretQuerystring(){
        return this.secreteKey + "=" + secretValue;
    }

    public boolean authorize(String query){
        if(query==null || query.isEmpty()) {
            return false;
        }

        return query.equals(this.secretValue);
    }
}
