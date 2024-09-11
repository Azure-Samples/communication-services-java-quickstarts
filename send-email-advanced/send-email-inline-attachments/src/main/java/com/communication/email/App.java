package com.communication.email;
import java.time.Duration;
import java.io.File;

import com.azure.communication.email.models.*;
import com.azure.communication.email.*;
import com.azure.core.util.BinaryData;
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

        BinaryData jpgInlineAttachmentContent = BinaryData.fromFile(new File("./inline-attachment.jpg").toPath());
        EmailAttachment jpgInlineAttachment = new EmailAttachment(
            "inline-attachment.jpg",
            "image/jpeg",
            jpgInlineAttachmentContent
        ).setContentId("kittens-1");

        BinaryData pngInlineAttachmentContent = BinaryData.fromFile(new File("./inline-attachment.png").toPath());
        EmailAttachment pngInlineAttachment = new EmailAttachment(
            "inline-attachment.png",
            "image/png",
            pngInlineAttachmentContent
        ).setContentId("kittens-2");

        EmailClient client = new EmailClientBuilder()
            .connectionString(connectionString)
            .buildClient();

        EmailMessage message = new EmailMessage()
            .setSenderAddress(senderAddress)
            .setToRecipients(recipientAddress)
            .setSubject("Test email from Java Sample")
            .setBodyPlainText("This is plaintext body of test email.")
            .setBodyHtml("<html><h1>HTML body inline images:</h1><img src="cid:kittens-1" /><img src="cid:kittens-2" /></html>")
            .setAttachments(jpgInlineAttachmentContent, pngInlineAttachmentContent);

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
