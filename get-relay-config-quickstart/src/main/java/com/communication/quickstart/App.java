package com.communication.quickstart;

import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.identity.CommunicationIdentityClient;
import com.azure.communication.identity.CommunicationIdentityClientBuilder;
import com.azure.communication.networktraversal.*;
import com.azure.communication.networktraversal.models.*;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.MediaPipeline;
import org.kurento.client.KurentoClient;
import java.util.List;
import java.lang.reflect.*;

public class App 
{
    private static KurentoClient kurento;

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

        // Now you can configure your WebRtcEndpoint to use TURN credentials
        
        // MediaPipeline pipeline = kurento.createMediaPipeline();
        // WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
        
        // CommunicationIceServer iceServerToUse = iceServers.get(0);
        // String urlToUse = iceServerToUse.getUrls().get(0);

        // Format for URL must be user:password@ipaddress:port
        // String ipAndPort = urlToUse.substring(5, urlToUse.length());
        // webRtcEndpoint.setTurnUrl(iceServerToUse.getUsername()+ ":" + iceServerToUse.getCredential() + "@" + ipAndPort);        
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
