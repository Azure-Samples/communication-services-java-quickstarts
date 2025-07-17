package com.communication.email;
import java.time.Duration;

import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.EmailClient;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.communication.email.models.EmailSendStatus;
import com.azure.communication.email.models.EmailMessage;
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

        EmailClient client = new EmailClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        EmailMessage message = new EmailMessage()
                .setSenderAddress(senderAddress)
                .setToRecipients(recipientAddress)
                .setSubject("Test email from Java Sample")
                .setBodyPlainText("This is plaintext body of test email.")
                .setBodyHtml("<html><h1>This is the html body of test email.</h1></html>");


        SyncPoller<EmailSendResult, EmailSendResult> poller = client.beginSend(message);
        PollResponse<EmailSendResult> response = poller.poll();
        String operationId = response.getValue().getId();
        System.out.printf("Sent email send request from first poller (operation id: %s)\n", operationId);

        System.out.print("Started polling from second poller\n");
        SyncPoller<EmailSendResult, EmailSendResult> poller2 = client.beginSend(operationId);
        PollResponse<EmailSendResult> response2 = poller2.waitForCompletion();

        System.out.printf("Successfully sent the email (operation id: %s)\n", poller2.getFinalResult().getId());
    }
}
