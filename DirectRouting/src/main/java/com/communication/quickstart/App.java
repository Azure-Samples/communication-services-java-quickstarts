package com.communication.quickstart;

import com.azure.communication.phonenumbers.siprouting.SipRoutingAsyncClient;
import com.azure.communication.phonenumbers.siprouting.SipRoutingClientBuilder;
import com.azure.communication.phonenumbers.siprouting.models.SipTrunk;
import com.azure.communication.phonenumbers.siprouting.models.SipTrunkRoute;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public class App {
    public static void main(String[] args) {
        System.out.println("Azure Communication Services - Direct Routing Quickstart");

        String connectionString = "https://<RESOURCE_NAME>.communication.azure.com/;accesskey=<ACCESS_KEY>";
        SipRoutingAsyncClient client = new SipRoutingClientBuilder()
                .connectionString(connectionString)
                .buildAsyncClient();

        listTrunksAndRoutes(client);
        setTrunksAndRoutes(client);
        listTrunksAndRoutes(client);
        updateTrunk(client);
        listTrunksAndRoutes(client);

        System.out.println("Finish");
    }

    private static void listTrunksAndRoutes(SipRoutingAsyncClient client) {
        System.out.println("---------------------");
        System.out.println("Listing trunks");
        try {
            List<SipTrunk> trunks = requireNonNull(client.listTrunks().byPage().blockFirst()).getValue();
            for (SipTrunk trunk : trunks) {
                System.out.printf(" %s:%d%n", trunk.getFqdn(), trunk.getSipSignalingPort());
            }
            if (trunks.isEmpty()) {
                System.out.println(" There are no SIP trunks.");
            }

            System.out.println("Listing routes");
            List<SipTrunkRoute> routes = requireNonNull(client.listRoutes().byPage().blockFirst()).getValue();
            for (SipTrunkRoute route : routes) {
                System.out.printf(" %s: \"%s\" -> %s%n", route.getName(), route.getNumberPattern(), String.join(", ", route.getTrunks()));
            }
            if (routes.isEmpty()) {
                System.out.println(" There are no voice routes.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Listing failed");
        }
    }

    private static void setTrunksAndRoutes(SipRoutingAsyncClient client) {
        System.out.println("---------------------");
        System.out.println("Setting trunks");
        try {
            client.setTrunksWithResponse(asList(
                    new SipTrunk("sbc.us.contoso.com", 1234),
                    new SipTrunk("sbc.eu.contoso.com", 1234)
            )).block();

            System.out.println("Setting routes");
            client.setRoutes(asList(
                    new SipTrunkRoute("UsRoute", "^\\+1(\\d{10})$")
                            .setTrunks(asList("sbc.us.contoso.com")),
                    new SipTrunkRoute("DefaultRoute", "^\\+\\d+$")
                            .setTrunks(asList("sbc.us.contoso.com", "sbc.eu.contoso.com"))
            )).block();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Setting failed");
        }
    }

    private static void updateTrunk(SipRoutingAsyncClient client) {
        System.out.println("---------------------");
        System.out.println("Updating trunk");
        try {
            client.setTrunk(new SipTrunk("sbc.us.contoso.com", 9876)).block();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Updating failed");
        }
    }
}