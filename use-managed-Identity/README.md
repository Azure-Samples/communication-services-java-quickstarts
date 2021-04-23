---
page_type: sample
languages:
- Java
products:
- azure
- azure-communication-services
---


# Create and manage access tokens

For full instructions on how to build this code sample from scratch, look at [Quickstart: Create and manage access tokens](https://docs.microsoft.com/en-us/azure/communication-services/quickstarts/access-tokens?pivots=programming-language-java)

## Prerequisites

- An Azure account with an active subscription. [Create an account for free](https://azure.microsoft.com/free/?WT.mc_id=A261C142F). 
- [Java Development Kit (JDK)](https://docs.microsoft.com/en-us/azure/developer/java/fundamentals/java-jdk-install) version 8 or above.
- [Apache Maven](https://maven.apache.org/download.cgi).
- A deployed Communication Services resource and connection string. For details, see [Create a Communication Services resource](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource).

## Code Structure

- **./communication-quickstart/src/main/java/com/communication/quickstart/app.java:** contains code for creating and managing access tokens.
- **pom.xml:** Project's Project Object Model, or [POM](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html).

## Before running sample code

1. Open an instance of PowerShell, Windows Terminal, Command Prompt or equivalent and navigate to the directory that you'd like to clone the sample to.
2. `git clone https://github.com/Azure-Samples/communication-services-java-quickstarts.git`
3. With the `endpoint ` procured in pre-requisites, add it to the **app.java** file. Assign your endpoint in line 41:
   ```String endpoint = "https://<RESOURCE_NAME>.communication.azure.com/";```
4. With the SMS enabled telephone number procured in pre-requisites, add it to the **app.java** file. Assign your ACS telephone number and sender number in line 49:
   ```SmsSendResult result = instance.sendSms(endpoint, "<FROM NUMBER>", "<TO NUMBER>", "Hello from Managed Identities");```

## Run the code

1. Navigate to the directory containing the pom.xml file and compile the project by using command `mvn compile`
2. Then, build the package using command `mvn package`
3. Run the command to execute the app `mvn exec:java -Dexec.mainClass="com.communication.quickstart.App" -Dexec.cleanupDaemonThreads=false`