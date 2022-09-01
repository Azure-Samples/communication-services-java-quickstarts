package com.communication.quickstart.callautomation;

import com.communication.quickstart.callautomation.controllers.ActionController;
import com.communication.quickstart.callautomation.controllers.CallController;

import static spark.Spark.*;

public class App 
{
    public static void main( String[] args )
    {
        QueryCallAutomationClient.initializeCallAutomationClient();
        CallController callController = new CallController();
        ActionController actionController = new ActionController();
        post("/incomingCall", callController::onIncomingCall);
        post("/events", callController::onCloudEvents);
        post("/actions/:action", actionController::onAction);
    }
}
