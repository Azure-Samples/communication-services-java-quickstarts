package com.communication.email;
import java.time.Duration;
import java.util.Arrays;

import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.EmailClient;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.communication.email.models.EmailSendStatus;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailAddress;
import com.azure.core.util.polling.SyncPoller;
import com.azure.core.util.polling.LongRunningOperationStatus;
import com.azure.core.util.polling.PollResponse;


public class App 
{
    public static final Duration POLLER_WAIT_TIME = Duration.ofSeconds(10);

    public static void main( String[] args )
    {
        String connectionString = "<ACS_CONNECTION_STRING>";
        String senderAddress = "<SENDER_EMAIL_ADDRESS>";
        String recipientAddress = "<RECIPIENT_EMAIL_ADDRESS>";
        String replyToAddress = "<REPLYTO_EMAIL_ADDRESS>";

        EmailClient client = new EmailClientBuilder()
            .connectionString(connectionString)
            .buildClient();

        EmailMessage message = new EmailMessage()
            .setSenderAddress(senderAddress)
            .setToRecipients(recipientAddress, recipientAddress)
            .setCcRecipients(recipientAddress)
            .setBccRecipients(recipientAddress)
            .setReplyTo(Arrays.asList(new EmailAddress(replyToAddress)))
            .setSubject("Test email from Java Sample")
            .setBodyPlainText("This is plaintext body of test email.")
            .setBodyHtml("<html><h1>This is the html body of test email.</h1></html>");

        try
        {
            SyncPoller<EmailSendResult, EmailSendResult> poller = client.beginSend(message, null);

            PollResponse<EmailSendResult> pollResponse = null;

            Duration timeElapsed = Duration.ofSeconds(0);

             while (pollResponse == null
                     || pollResponse.getStatus() == LongRunningOperationStatus.NOT_STARTED
                     || pollResponse.getStatus() == LongRunningOperationStatus.IN_PROGRESS)
             {
                 pollResponse = poller.poll();
                 System.out.println("Email send poller status: " + pollResponse.getStatus());

                 Thread.sleep(POLLER_WAIT_TIME.toMillis());
                 timeElapsed = timeElapsed.plus(POLLER_WAIT_TIME);

                 if (timeElapsed.compareTo(POLLER_WAIT_TIME.multipliedBy(18)) >= 0)
                 {
                     throw new RuntimeException("Polling timed out.");
                 }
             }

             if (poller.getFinalResult().getStatus() == EmailSendStatus.SUCCEEDED)
             {
                 System.out.printf("Successfully sent the email (operation id: %s)", poller.getFinalResult().getId());
             }
             else
             {
                 throw new RuntimeException(poller.getFinalResult().getError().getMessage());
             }
        }
        catch (Exception exception)
        {
            System.out.println(exception.getMessage());
        }
    }
}
