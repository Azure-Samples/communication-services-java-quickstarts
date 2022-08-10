---
page_type: sample
languages:
- java
products:
- azure
- azure-communication-services
---

# Azure Communication Services - Rooms

This quickstart will help you get started with Azure Communication Services Rooms. A `room` is a server-managed communications space for a known, fixed set of participants to collaborate for a pre-determined duration. The [rooms conceptual documentation](https://docs.microsoft.com/azure/communication-services/concepts/rooms/room-concept) covers more details and use cases for `rooms`.

## Prerequisites

- An Azure account with an active subscription. [Create an account for free](https://azure.microsoft.com/free/?WT.mc_id=A261C142F).
- An active Communication Services resource and connection string.
- Two or more Communication User Identities.
- [Java Development Kit (JDK)](/java/azure/jdk/?view=azure-java-stable&preserve-view=true) version 8 or above.
- [Apache Maven](https://maven.apache.org/download.cgi)

## Code structure

- **src/main/java/com/communication/rooms/quickstart/App.java**: includes source code for manging rooms and room participants.
- **pom.xml:** Project's Project Object Model, or [POM](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html).

## Before running sample code

1. Open an instance of PowerShell, Windows Terminal, Command Prompt or equivalent and navigate to the directory that you'd like to clone the sample to.
1. `git clone https://github.com/Azure-Samples/communication-services-java-quickstarts.git`
1. With the Communication Services resource connection string procured in pre-requisites, replace the placeholder at line no 21
    ```String connectionString = "<connection-string>";```.

## Run the code

1. Navigate to the directory containing the pom.xml file and compile the project by using command `mvn compile`.
2. Build the package using command `mvn package`.
3. Run the command to execute the app `mvn exec:java -Dexec.mainClass="com.communication.quickstart.App" -Dexec.cleanupDaemonThreads=false`.
