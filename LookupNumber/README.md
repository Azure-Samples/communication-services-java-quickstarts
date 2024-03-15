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


# Look Up Phone Numbers

For full instructions on how to build this code sample from scratch, look at [Quickstart: Manage Phone Numbers](https://learn.microsoft.com/en-us/azure/communication-services/quickstarts/telephony/number-lookup?pivots=programming-language-java)

## Prerequisites

- An Azure account with an active subscription. [Create an account for free](https://azure.microsoft.com/free/?WT.mc_id=A261C142F). 
- [Java Development Kit (JDK)](https://docs.microsoft.com/azure/developer/java/fundamentals/java-jdk-install) version 8 or above
- [Apache Maven](https://maven.apache.org/download.cgi)
- An deployed Communication Services resource and connection string. For details, see [Create a Communication Services resource](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource).

## Code Structure

- **./LookupNumber/src/main/java/com/communication/lookup/quickstart/App.java:** contains code for looking up phone numbers.
- **pom.xml:** Project's Project Object Model, or [POM](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html).

## Before running sample code

1. Open an instance of PowerShell, Windows Terminal, Command Prompt or equivalent and navigate to the directory that you'd like to clone the sample to.
2. `git clone https://github.com/Azure-Samples/communication-services-java-quickstarts.git`
3.  With the Communication Services procured in pre-requisites, add connection string as an environment variable named `COMMUNICATION_SERVICES_CONNECTION_STRING`
4.  Update line 25 with the phone number you want to look up.
5.  Decide which lookup you would like to perform, and keep in mind that looking up all the operator details incurs a cost, while looking up only number formatting is free.

> [!WARNING]
> If you want to avoid incurring a charge, comment out lines 36-48

## Run the code

1. Navigate to the directory containing the pom.xml file and compile the project by using command `mvn compile`.
2. Then, build the package using command `mvn package`.
3. Run the command to execute the app `mvn exec:java -D"exec.mainClass"="com.communication.lookup.quickstart.App" -D"exec.cleanupDaemonThreads"="false"`.
