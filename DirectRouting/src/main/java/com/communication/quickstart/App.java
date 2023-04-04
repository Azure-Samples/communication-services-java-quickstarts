package com.communication.quickstart;

import com.azure.communication.phonenumbers.siprouting.SipRoutingAsyncClient;
import com.azure.communication.phonenumbers.siprouting.SipRoutingClientBuilder;
import com.azure.communication.phonenumbers.siprouting.models.SipTrunk;
import com.azure.communication.phonenumbers.siprouting.models.SipTrunkRoute;
import static java.util.Arrays.asList;

public class App {
  public static void main(String[] args) {
	System.out.println("Azure Communication Services - Direct Routing Quickstart");

    String connectionString = "https://<RESOURCE_NAME>.communication.azure.com/;accesskey=<ACCESS_KEY>";
	SipRoutingAsyncClient sipRoutingAsyncClient = new SipRoutingClientBuilder()
		.connectionString(connectionString)
		.buildAsyncClient();

	System.out.println("Setting trunks");
	sipRoutingAsyncClient.setTrunksWithResponse(asList(
		new SipTrunk("sbc.us.contoso.com", 1234),
		new SipTrunk("sbc.eu.contoso.com", 1234)
	)).block();

	System.out.println("Setting routes");
	sipRoutingAsyncClient.setRoutes(asList(
		new SipTrunkRoute("UsRoute", "^\\+1(\\d{10})$").setTrunks(asList("sbc.us.contoso.com")),
		new SipTrunkRoute("DefaultRoute", "^\\+\\d+$").setTrunks(asList("sbc.us.contoso.com", "sbc.eu.contoso.com"))
	)).block();

	System.out.println("Finish");
  }
}