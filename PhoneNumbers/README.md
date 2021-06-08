---
page_type: sample
languages:
- Java
products:
- azure
- azure-communication-phonenumbers
- azure-identity
- azure-communication-common
---


# Manage Phone Numbers

For full instructions on how to build this code sample from scratch, look at [Quickstart: Manage Phone Numbers](https://docs.microsoft.com/azure/communication-services/quickstarts/telephony-sms/get-phone-number?pivots=programming-language-java)

## Prerequisites

- An Azure account with an active subscription. [Create an account for free](https://azure.microsoft.com/free/?WT.mc_id=A261C142F). 
- [Java Development Kit (JDK)](https://docs.microsoft.com/azure/developer/java/fundamentals/java-jdk-install) version 8 or above
- [Apache Maven](https://maven.apache.org/download.cgi)
- An deployed Communication Services resource and connection string. For details, see [Create a Communication Services resource](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource).

## Code Structure

- **./PhoneNumbers/src/main/java/com/communication/quickstart/App.java:** contains code for managing phone numbers.
- **pom.xml:** Project's Project Object Model, or [POM](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html).

## Before running sample code

1. Open an instance of PowerShell, Windows Terminal, Command Prompt or equivalent and navigate to the directory that you'd like to clone the sample to.
2. `git clone https://github.com/Azure-Samples/communication-services-java-quickstarts.git`
3.  With the Communication Services procured in pre-requisites, add connection string in the code at line no 26
    ```String connectionString = "https://<RESOURCE_NAME>.communication.azure.com/;accesskey=<ACCESS_KEY>";```.
4.  With a purchased number, you can update the capabilities, pass the purchased number in the code at line no 68.
5.  You can release a purchased phone number, pass the purchased number in the code at line no 76.    


## Run the code

1. Navigate to the directory containing the pom.xml file and compile the project by using command `mvn compile`.
2. Then, build the package using command `mvn package`.
3. Run the command to execute the app `mvn exec:java -Dexec.mainClass="com.communication.quickstart.App" -Dexec.cleanupDaemonThreads=false`.
