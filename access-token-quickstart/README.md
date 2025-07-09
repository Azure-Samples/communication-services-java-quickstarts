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
- [Java Development Kit (JDK)](https://docs.microsoft.com/en-us/azure/developer/java/fundamentals/java-jdk-install) version 8 or above
- [Apache Maven](https://maven.apache.org/download.cgi)
- A deployed Communication Services resource and connection string. For details, see [Create a Communication Services resource](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource).

## Code Structure

- **./communication-quickstart/src/main/java/com/communication/quickstart:** contains code for creating and managing access tokens.
- **pom.xml:** Project's Project Object Model, or [POM](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html).

## Before running sample code

1. Open an instance of PowerShell, Windows Terminal, Command Prompt or equivalent and navigate to the directory that you'd like to clone the sample to.
2. `git clone https://github.com/Azure-Samples/communication-services-java-quickstarts.git`
3.  With the Communication Services procured in pre-requisites, add connection string with the access key in the code.

## Run the code

1. Navigate to the directory containing the pom.xml file and compile the project by using command `mvn compile`
2. Then, build the package using command `mvn package`
3. Run the command to execute the app `mvn exec:java -D"exec.mainClass=com.communication.quickstart.App" -D"exec.cleanupDaemonThreads=false"`
