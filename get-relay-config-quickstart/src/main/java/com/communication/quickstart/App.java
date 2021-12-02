package com.communication.quickstart;

import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.identity.CommunicationIdentityClient;
import com.azure.communication.identity.CommunicationIdentityClientBuilder;
import com.azure.communication.networktraversal.*;
import com.azure.communication.networktraversal.models.*;
import java.util.List;
import java.lang.reflect.*;

public class App 
{
    // You can find your connection string from your resource in the Azure portal
    private static String connectionString = "https://<RESOURCE_NAME>.communication.azure.com/;accesskey=<ACCESS_KEY>";
    private static CommunicationIdentityClient communicationIdentityClient = new CommunicationIdentityClientBuilder()
        .connectionString(connectionString)
        .buildClient();

    private static CommunicationRelayClient communicationRelayClient = new CommunicationRelayClientBuilder()
        .connectionString(connectionString)
        .buildClient();

    public static void main(String[] args)
    {
        System.out.println("Azure Communication Services - NetworkTraversal Quickstart");

        System.out.println("Getting a relay configuration");
        getRelayConfiguration();

        System.out.println("Getting a relay configuration using Identity");
        getRelayConfigurationUsingIdentity();

        System.out.println("Getting a relay configuration passing a Route Type");
        getRelayConfigurationUsingRouteType();
    }

    public static void getRelayConfiguration()
    {
        CommunicationRelayConfiguration config = communicationRelayClient.getRelayConfiguration();

        System.out.println("Expires on:" + config.getExpiresOn());
        List<CommunicationIceServer> iceServers = config.getIceServers();

        for (CommunicationIceServer iceS : iceServers) {
        System.out.println("URLS: " + iceS.getUrls());
        System.out.println("Username: " + iceS.getUsername());
        System.out.println("credential: " + iceS.getCredential());
        System.out.println("RouteType: " + iceS.getRouteType());
    }
}

    public static void getRelayConfigurationUsingIdentity()
    {
        CommunicationUserIdentifier user = communicationIdentityClient.createUser();
        System.out.println("User id: " + user.getId());

        CommunicationRelayConfiguration config = communicationRelayClient.getRelayConfiguration(user);

        System.out.println("Expires on:" + config.getExpiresOn());
        List<CommunicationIceServer> iceServers = config.getIceServers();

        for (CommunicationIceServer iceS : iceServers) {
            System.out.println("URLS: " + iceS.getUrls());
            System.out.println("Username: " + iceS.getUsername());
            System.out.println("credential: " + iceS.getCredential());
            System.out.println("RouteType: " + iceS.getRouteType());
        }
    }

    public static void getRelayConfigurationUsingRouteType()
    {
        CommunicationRelayConfiguration config = communicationRelayClient.getRelayConfiguration(RouteType.NEAREST);

        System.out.println("Expires on:" + config.getExpiresOn());
        List<CommunicationIceServer> iceServers = config.getIceServers();

        for (CommunicationIceServer iceS : iceServers) {
            System.out.println("URLS: " + iceS.getUrls());
            System.out.println("Username: " + iceS.getUsername());
            System.out.println("credential: " + iceS.getCredential());
            System.out.println("RouteType: " + iceS.getRouteType());
        }
    }
}
