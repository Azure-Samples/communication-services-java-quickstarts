package com.communication.quickstart;

import com.azure.communication.sms.models.*;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.communication.sms.*;
import com.azure.core.util.Context;
import java.util.Arrays;

public class App
{
    public static void main( String[] args )
    {
        // Quickstart code goes here
		// You can find your endpoint and access key from your resource in the Azure portal
		//String endpoint = "https://<resource-name>.communication.azure.com/";
		//AzureKeyCredential azureKeyCredential = new AzureKeyCredential("<access-key-credential>");

		//SmsClient smsClient = new SmsClientBuilder()
						//.endpoint(endpoint)
						//.credential(azureKeyCredential)
						//.buildClient();

		// You can find your connection string from your resource in the Azure portal
		String connectionString = "https://<resource-name>.communication.azure.com/;<access-key>";

		SmsClient smsClient = new SmsClientBuilder()
					.connectionString(connectionString)
					.buildClient();		

		//Send a 1:1 SMS message
		//SmsSendResult sendResult = smsClient.send(
		//				"<from-phone-number>",
		//				"<to-phone-number>",
		//				"Weekly Promotion");

		//System.out.println("Message Id: " + sendResult.getMessageId());
		//System.out.println("Recipient Number: " + sendResult.getTo());
		//System.out.println("Send Result Successful:" + sendResult.isSuccessful());

		//Send a 1:N SMS message with options
		SmsSendOptions options = new SmsSendOptions();
		options.setDeliveryReportEnabled(true);
		options.setTag("Marketing");

		Iterable<SmsSendResult> sendResults = smsClient.sendWithResponse(
			"<from-phone-number>",
			Arrays.asList("<to-phone-number1>", "<to-phone-number2>"),
			"Weekly Promotion",
			options /* Optional */,
			Context.NONE).getValue();

		for (SmsSendResult result : sendResults) {
			System.out.println("Message Id: " + result.getMessageId());
			System.out.println("Recipient Number: " + result.getTo());
			System.out.println("Send Result Successful:" + result.isSuccessful());
		}		
    }
}