---
page_type: sample
languages:
- Java
products:
- azure
- azure-communication-services
---


# Add Chat to your Application

For full instructions on how to build this code sample from scratch, look at [Quickstart: Add Chat to your App](https://docs.microsoft.com/en-us/azure/communication-services/quickstarts/chat/get-started?pivots=programming-language-java)

## Prerequisites

- An Azure account with an active subscription. [Create an account for free](https://azure.microsoft.com/free/?WT.mc_id=A261C142F). 
- [Java Development Kit (JDK)](https://docs.microsoft.com/en-us/azure/developer/java/fundamentals/java-jdk-install)
- [Apache Maven](https://maven.apache.org/download.cgi)
- A deployed Communication Services resource and connection string. For details, see [Create a Communication Services resource](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource).
- A [User Access Token](https://docs.microsoft.com/en-us/azure/communication-services/quickstarts/access-tokens?pivots=programming-language-java). Be sure to set the scope to "chat", and note the token string as well as the userId string.

## Code Structure

- **src/main/java directory:** contains the project source code.
- **pom.xml:** Project's Project Object Model, or [POM](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html).

## Before running sample code

1. Open an instance of PowerShell, Windows Terminal, Command Prompt or equivalent and navigate to the directory that you'd like to clone the sample to.
2. `git clone https://github.com/Azure-Samples/communication-services-java-quickstarts.git`
3.  With the Communication Services procured in pre-requisites, add connection string with the access key in the code.

## Run the code
1. Navigate to the directory containing the pom.xml file and compile the project by using command `mvn compile`
2. Then, build the package using command `mvn package`
3. Run the command to execute the app `mvn exec:java -Dexec.mainClass="com.communication.quickstart.App" -Dexec.cleanupDaemonThreads=false`
