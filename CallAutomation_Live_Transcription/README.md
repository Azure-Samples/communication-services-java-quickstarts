|page_type| languages                             |products
|---|---------------------------------------|---|
|sample| <table><tr><td>Java</tr></td></table> |<table><tr><td>azure</td><td>azure-communication-services</td></tr></table>|

# Call Live Transcription - Quick Start Sample

This sample application shows how the Azure Communication Services  - Call Automation SDK can be used generate the live transcription between PSTN calls. 
It accepts an incoming call from a phone number, performs DTMF recognition, and transfer the call to agent. You can see the live transcription in websocket during the conversation between agent and user
This sample application is also capable of making multiple concurrent inbound calls. The application is a web-based application built on Java's Spring framework.


## Prerequisites

- An Azure account with an active subscription. [Create an account for free](https://azure.microsoft.com/free/?WT.mc_id=A261C142F). 
- A deployed Communication Services resource. [Create a Communication Services resource](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource).
- A [phone number](https://learn.microsoft.com/en-us/azure/communication-services/quickstarts/telephony/get-phone-number) in your Azure Communication Services resource that can make outbound calls. NB: phone numbers are not available in free subscriptions.
- [Java Development Kit (JDK) Microsoft.OpenJDK.17](https://learn.microsoft.com/en-us/java/openjdk/download)
- [Apache Maven](https://maven.apache.org/download.cgi)
- Install ngrok. Instructions [here](https://ngrok.com/)
- Create Azure AI Multi Service resource. For details, see [Create an Azure AI Multi service](https://learn.microsoft.com/en-us/azure/cognitive-services/cognitive-services-apis-create-account).

## Before running the sample for the first time

### Setup and host ngrok

You can run multiple tunnels on ngrok by changing ngrok.yml file as follows:

1. Open the ngrok.yml file from a powershell using the command ngrok config edit
2. Update the ngrok.yml file as follows:
    authtoken: xxxxxxxxxxxxxxxxxxxxxxxxxx
    version: "2"
    region: us
    tunnels:
    first:
        addr: 8080
        proto: http 
        host_header: localhost:8080
    second:
        proto: http
        addr: 5001
        host_header: localhost:5001
NOTE: Make sure the "addr:" field has only the port number, not the localhost url.
3. Start all ngrok tunnels configured using the following command on a powershell - ngrok start --all
4. Once you have setup the websocket server, note down the the ngrok url on your server's port as the websocket url in this application for incoming call scenario. Just replace the https:// with wss:// and update in the application.yml file.

### Configuring application

- Open the application.yml file in the resources folder to configure the following settings

    - `connectionstring`: Azure Communication Service resource's connection string.
    - `basecallbackuri`: Base url of the app. For local development use dev tunnel url.
    - `cognitiveServicesUrl`: The Cognitive Services endpoint
    - `transportUrl`: Ngrok url for the server port (in this example port 5001) make sure to replace https:// with wss://
    - `acsPhoneNumber`: Acs Phone Number
    - `agentPhoneNumber`: Agent Phone Number
    - `locale`: Transcription locale

### Run the application

- Navigate to the directory containing the pom.xml file and use the following mvn commands:
    - Compile the application: mvn compile
    - Build the package: mvn package
    - Execute the app: mvn exec:java
- Access the Swagger UI at http://localhost:8080/swagger-ui.html
    - Test this application by giving a call to ACS phone number