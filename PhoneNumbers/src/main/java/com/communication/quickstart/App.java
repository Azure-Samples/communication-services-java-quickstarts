package com.communication.quickstart;
import com.azure.communication.phonenumbers.*;
import com.azure.communication.phonenumbers.models.*;
import com.azure.core.http.rest.*;
import com.azure.core.util.Context;
import com.azure.core.util.polling.PollResponse;
import com.azure.identity.*;
import java.io.*;


public class App 
{
    public static void main( String[] args )
    {
		
		System.out.println("Azure Communication Services - Phone Numbers Quickstart");
		// You can find your endpoint and access key from your resource in the Azure portal
		//String endpoint = "https://<RESOURCE_NAME>.communication.azure.com";

		//PhoneNumbersClient phoneNumberClient = new PhoneNumbersClientBuilder()
			//.endpoint(endpoint)
			//.credential(new DefaultAzureCredentialBuilder().build())
			//.buildClient();		  
		
		// You can find your connection string from your resource in the Azure portal
		String connectionString = "https://<RESOURCE_NAME>.communication.azure.com/;accesskey=<ACCESS_KEY>";

		PhoneNumbersClient phoneNumberClient = new PhoneNumbersClientBuilder()
			.connectionString(connectionString)
			.buildClient();		
		
		
		// Search for Available Phone Numbers
		PhoneNumberCapabilities capabilities = new PhoneNumberCapabilities()
			.setCalling(PhoneNumberCapabilityType.INBOUND)
			.setSms(PhoneNumberCapabilityType.INBOUND_OUTBOUND);
		PhoneNumberSearchOptions searchOptions = new PhoneNumberSearchOptions().setAreaCode("833").setQuantity(1);

		PhoneNumberSearchResult searchResult = phoneNumberClient
			.beginSearchAvailablePhoneNumbers("US", PhoneNumberType.TOLL_FREE, PhoneNumberAssignmentType.APPLICATION, capabilities,  searchOptions, Context.NONE)
			.getFinalResult();

		System.out.println("Searched phone numbers: " + searchResult.getPhoneNumbers());
		System.out.println("Search expires by: " + searchResult.getSearchExpiresBy());
		System.out.println("Phone number costs:" + searchResult.getCost().getAmount());	

		// Purchase Phone Numbers
		PollResponse<PhoneNumberOperation> purchaseResponse = phoneNumberClient.beginPurchasePhoneNumbers(searchResult.getSearchId(), Context.NONE).waitForCompletion();
		System.out.println("Purchase phone numbers operation is: " + purchaseResponse.getStatus());		
		
		
		// Get Phone Number(s)
		//PurchasedPhoneNumber phoneNumber = phoneNumberClient.getPurchasedPhoneNumber("<Phone Number>");
		//System.out.println("Phone Number Country Code: " + phoneNumber.getCountryCode());
		
		// You can also retrieve all the purchased phone numbers.
		PagedIterable<PurchasedPhoneNumber> phoneNumbers = phoneNumberClient.listPurchasedPhoneNumbers(Context.NONE);
		PurchasedPhoneNumber phoneNumber = phoneNumbers.iterator().next();
		System.out.println("Phone Number Country Code: " + phoneNumber.getCountryCode());


		// Update Phone Number Capabilities
		
		PhoneNumberCapabilities updatecapabilities = new PhoneNumberCapabilities();
		capabilities
			.setCalling(PhoneNumberCapabilityType.INBOUND)
			.setSms(PhoneNumberCapabilityType.INBOUND_OUTBOUND);
		PurchasedPhoneNumber updatephoneNumber = phoneNumberClient.beginUpdatePhoneNumberCapabilities("<Phone Number>", updatecapabilities, Context.NONE).getFinalResult();

		System.out.println("Phone Number Calling capabilities: " + updatephoneNumber.getCapabilities().getCalling()); //Phone Number Calling capabilities: inbound
		System.out.println("Phone Number SMS capabilities: " + updatephoneNumber.getCapabilities().getSms()); //Phone Number SMS capabilities: inbound+outbound		
		
		
		// Release Phone Number
		PollResponse<PhoneNumberOperation> releaseResponse =
			phoneNumberClient.beginReleasePhoneNumber("<Phone Number>", Context.NONE).waitForCompletion();
		System.out.println("Release phone number operation is: " + releaseResponse.getStatus());		
		
		
    }
}
