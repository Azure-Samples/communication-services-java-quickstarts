---
page_type: sample
languages:
- Java
products:
- azure
- azure-communication-networktraversal
- azure-communication-common
---


# Get a relay Configuration

## Prerequisites

- An Azure account with an active subscription. [Create an account for free](https://azure.microsoft.com/free/?WT.mc_id=A261C142F). 
- [Java Development Kit (JDK)](https://docs.microsoft.com/azure/developer/java/fundamentals/java-jdk-install) version 8 or above
- [Apache Maven](https://maven.apache.org/download.cgi)
- An deployed Communication Services resource and connection string. For details, see [Create a Communication Services resource](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource).

## Code Structure

- **./get-relay-config-quickstart/src/main/java/com/communication/quickstart/App.java:** contains code for getting a relay configuration.
- **pom.xml:** Project's Project Object Model, or [POM](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html).

## Before running sample code

1. Open an instance of PowerShell, Windows Terminal, Command Prompt or equivalent and navigate to the directory that you'd like to clone the sample to.
2. `git clone https://github.com/Azure-Samples/communication-services-java-quickstarts.git`
3.  With the Communication Services procured in pre-requisites, add connection string in the code at line no 14
    ```String connectionString = "https://<RESOURCE_NAME>.communication.azure.com/;accesskey=<ACCESS_KEY>";```.

## Run the code

1. Navigate to the directory containing the pom.xml file and compile the project by using command `mvn compile`.
2. Then, build the package using command `mvn package`.
3. Run the command to execute the app `mvn exec:java -Dexec.mainClass="com.communication.quickstart.App" -Dexec.cleanupDaemonThreads=false`. If you are on Windows, run the following command: `mvn exec:java -D"exec.mainClass"="com.communication.quickstart.App" -D"exec.cleanupDaemonThreads"="false"`

## Next Steps

Take a look at our [API Documentation][apiref] for more information about the APIs that are available in the clients:

[getrelayconfiguration]: https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/communication/azure-communication-networktraversal/src/samples/java/com/azure/communication/networktraversal
[freesub]: https://azure.microsoft.com/free/
[createinstance_azurecommunicationservicesaccount]: https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource
[package]: https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/communication/azure-communication-networktraversal/README.md
