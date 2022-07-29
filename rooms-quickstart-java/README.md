---
page_type: sample
languages:
- java
products:
- azure
- azure-communication-services
---

# Azure Communication Services - Rooms Private Preview
**Private Repository - Contents shared under NDA and TAP Agreement**

This is a private respository for the Azure Communication Services technology adoption program (TAP), colloquially called private preview. The documentation and artifacts listed below help you try out new features in development and provide feedback.

# Create and manage rooms

This code sample contains source code for a Java application that can create and manage Azure Communication Services rooms.

For full instructions on how to build this code sample, please refer to the accompanying [document](https://docs.microsoft.com/en-us/azure/communication-services/quickstarts/rooms/get-started-rooms?pivots=programming-language-java).

The private preview version of Azure Communiation Services Rooms Java SDK is also embedded.

## Prerequisites
- An Azure account with an active subscription. [Create an account for free](https://azure.microsoft.com/free/?WT.mc_id=A261C142F).
- An active Communication Services resource. [Create a Communication Services resource](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource).
- Two or more Communication User Identities. [Create and manage access tokens](https://docs.microsoft.com/en-us/azure/communication-services/quickstarts/access-tokens?pivots=programming-language-csharp) or [Quick-create identities for testing](https://review.docs.microsoft.com/en-us/azure/communication-services/quickstarts/identity/quick-create-identity).
- [Java Development Kit (JDK)](https://docs.microsoft.com/java/azure/jdk/?view=azure-java-stable) version 8 or above.
- [Apache Maven](https://maven.apache.org/download.cgi)

## Code Structure

- **./src/main/java/com/communication/rooms/quickstart/App.java:** Core application code with room operations implementation.
- **./azure-communication-rooms/azure-communication-rooms-1.0.0-alpha.1.jar**
- **pom.xml** Maven project and configuration

## Before running sample code
1. In App.java, replace the `<connection-string>` block with your Azure Communication Resource connection string.
1. In App.java, replace the `<communication-user-id-*>` blocks with your Communication User Identifiers as room participants.

## Run the sample code
1. Install `rooms-sdk` jar to local Maven repository:
    `mvn install:install-file -Dfile=.\azure-communication-rooms\azure-communication-rooms-1.0.0-alpha.1.jar -DgroupId=com.azure -DartifactId=azure-communication-rooms -Dversion=1.0.0-alpha.1`
2. Compile the package: `mvn compile`
3. Then, build the package using command: `mvn package`
4. Run the App
    `mvn exec:java -Dexec.mainClass=com.communication.rooms.quickstart.App -Dexec.cleanupDaemonThreads=false`
