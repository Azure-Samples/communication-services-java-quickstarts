package com.communication.quickstart;
import java.util.ArrayList;
import java.util.List;

import com.azure.communication.email.*;
import com.azure.communication.email.models.EmailAddress;
import com.azure.communication.email.models.EmailContent;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailRecipients;
import com.azure.communication.email.models.SendEmailResult;
import com.azure.communication.email.models.SendStatus;
import com.azure.communication.email.models.SendStatusResult;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;

public class App 
{
    public static void main( String[] args )
    {
        String connectionString = "<ACS_CONNECTION_STRING>";

        NettyAsyncHttpClientBuilder httpClientBuilder = new NettyAsyncHttpClientBuilder();
        EmailClientBuilder emailClientBuilder = new EmailClientBuilder().httpClient(httpClientBuilder.build())
        .connectionString(connectionString);

        EmailClient emailClient = emailClientBuilder.buildClient();
    
        String subject = "Send email quick start - java";

        EmailContent emailContent = new EmailContent(subject)
        .setPlainText("This is plain mail send test body \n Best Wishes!!")
        .setHtml("<html><body><h1>Quick send email test</h1><br/><h4>Communication email as a service mail send app working properly</h4><p>Happy Learning!!</p></body></html>");
        
        String sender = "<SENDER_EMAIL>";
        List<EmailAddress> emailAddress = new ArrayList<EmailAddress>() {
            {
                add(new EmailAddress("<RECIPIENT_EMAIL>").setDisplayName("<RECIPIENT_DISPLAY_NAME>"));
            }
        };

        EmailRecipients emailRecipients = new EmailRecipients(emailAddress);

        EmailMessage emailMessage = new EmailMessage(sender, emailContent)
        .setRecipients(emailRecipients);

        try
        {
            SendEmailResult sendEmailResult = emailClient.send(emailMessage);

            String messageId = sendEmailResult.getMessageId();
            if (!messageId.isEmpty() && messageId != null)
            {
                System.out.printf("Email sent, MessageId = {%s} %n", messageId);
            }
            else
            {
                System.out.println("Failed to send email.");
                return;
            }

            long waitTime = 120*1000;
            boolean timeout = true;
            while (waitTime > 0)
            {
                SendStatusResult sendStatus = emailClient.getSendStatus(messageId);
                System.out.printf("Send mail status for MessageId : <{%s}>, Status: [{%s}]", messageId, sendStatus.getStatus());

                if (!sendStatus.getStatus().toString().toLowerCase().equals(SendStatus.QUEUED.toString()))
                {
                    timeout = false;
                    break;
                }
                Thread.sleep(10000);
                waitTime = waitTime-10000;
            }

            if(timeout)
            {
                System.out.println("Looks like we timed out for email");
            }
        }
        catch (Exception ex)
        {
            System.out.printf("Error in sending email, {%s}", ex);
        }
    }
}
