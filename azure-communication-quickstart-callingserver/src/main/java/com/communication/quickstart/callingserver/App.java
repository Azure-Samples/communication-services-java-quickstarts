package com.communication.quickstart.callingserver;

import com.communication.quickstart.callingserver.controllers.ActionController;
import com.communication.quickstart.callingserver.controllers.CallController;

import static spark.Spark.*;

public class App 
{
    public static void main( String[] args )
    {
        CallAutomationClient.initializeCallAutomationClient();
        CallController callController = new CallController();
        ActionController actionController = new ActionController();
        post("/incomingCall", callController::onIncomingCall);
        post("/events", callController::onCloudEvents);
        post("/actions/:action", actionController::onAction);
    }
}
