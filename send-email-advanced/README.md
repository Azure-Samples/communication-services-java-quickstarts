---
page_type: sample
languages:
- Java
products:
- azure
- azure-communication-email
---


# Email Sample

This sample sends an email to the selected recipients of any domain using an [Email Communication Services resource](https://docs.microsoft.com/azure/communication-services/quickstarts/email/create-email-communication-resource).

Additional documentation for this sample can be found on [Microsoft Docs](https://docs.microsoft.com/azure/communication-services/concepts/email/email-overview).

## Prerequisites

- An Azure account with an active subscription. [Create an account for free](https://azure.microsoft.com/free/?WT.mc_id=A261C142F).
- [Java Development Kit (JDK)](https://docs.microsoft.com/azure/developer/java/fundamentals/java-jdk-install) version 8 or above.
- [Apache Maven](https://maven.apache.org/download.cgi).
- A deployed Communication Services resource and connection string. For details, see [Create a Communication Services resource](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource).
- Create an [Azure Email Communication Services resource](https://docs.microsoft.com/azure/communication-services/quickstarts/email/create-email-communication-resource) to start sending emails.
- A setup managed identity for a development environment, [see Authorize access with managed identity](https://docs.microsoft.com/azure/communication-services/quickstarts/managed-identity-from-cli).

> Note: We can also send an email from our own verified domain [Add custom verified domains to Email Communication Service](https://docs.microsoft.com/azure/communication-services/quickstarts/email/add-custom-verified-domains).

### Prerequisite check

- To view the domains verified with your Email Communication Services resource, sign in to the [Azure portal](https://portal.azure.com/). Locate your Email Communication Services resource and open the **Provision domains** tab from the left navigation pane.

## Code Structure

- **./sample-name/src/main/java/com/communication/email/App.java:** contains code for sending email.
- **pom.xml:** Project's Project Object Model, or [POM](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html).

## Before running sample code

1. Open an instance of PowerShell, Windows Terminal, Command Prompt or equivalent and navigate to the directory that you'd like to clone the sample to.
2. `git clone https://github.com/Azure-Samples/communication-services-java-quickstarts.git`
3. Open **App.java** file to configure the following settings:

   - `connectionString`: Replace `<ACS_CONNECTION_STRING>` with rhe connection string found within the 'Keys' blade of the Azure Communication Service resource.
   - `senderAddress`: Replace `<SENDER_EMAIL_ADDRESS>` with the sender email address obtained from the linked domain resource.
   - `recipientAddress`: Replace `<RECIPIENT_EMAIL_ADDRESS>` with the recipient email address.

## Run the code

Run the following commands to build and execute the code.

```powershell
mvn clean compile assembly:single
java -jar .\target\send-email-1.0-SNAPSHOT-jar-with-dependencies.jar
```
