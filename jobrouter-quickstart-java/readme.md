---
page_type: sample
languages:
- java
products:
- azure
- azure-communication-services
---

# Azure Communication Services - Job Router

For full instructions on how to build this code sample from scratch, look at [Quickstart: Create a worker and job](https://learn.microsoft.com/azure/communication-services/quickstarts/jobrouter/quickstart)

## Prerequisites

- An Azure account with an active subscription. [Create an account for free](https://azure.microsoft.com/free/?WT.mc_id=A261C142F).
- An active Communication Services resource and connection string.
- [Java Development Kit (JDK)](/java/azure/jdk/?view=azure-java-stable&preserve-view=true) version 8 or above.
- [Apache Maven](https://maven.apache.org/download.cgi)

## Code structure

- **src/main/java/com/communication/jobrouter/quickstart/App.java**: includes source code for quick start.
- **pom.xml:** Project's Project Object Model, or [POM](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html).

## Before running sample code

1. Open an instance of PowerShell, Windows Terminal, Command Prompt or equivalent and navigate to the directory that you'd like to clone the sample to.
1. `git clone https://github.com/Azure-Samples/communication-services-java-quickstarts.git`
1. With the Communication Services resource connection string procured in pre-requisites, replace the placeholder
   ```String connectionString = "<connection-string>";```.

## Run the code

1. Navigate to the directory containing the pom.xml file and compile the project by using command `mvn compile`.
2. Build the package using command `mvn package`.
3. Run the command (in command prompt or bash) to execute the app `mvn exec:java -Dexec.mainClass="com.communication.jobrouter.quickstart.App" -Dexec.cleanupDaemonThreads=false`.
