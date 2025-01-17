|page_type| languages                             |products
|---|---------------------------------------|---|
|sample| <table><tr><td>Java</tr></td></table> |<table><tr><td>azure</td><td>azure-communication-services</td></tr></table>|

# Call Automation - Quick Start Sample

This sample application shows how the Azure Communication Services  - Call Automation SDK can be used with Azure OpenAI Service to enable intelligent conversational agents. 
It answers an inbound call, does a speech recognition with the recognize API and Cognitive Services, uses OpenAi Services with the input speech and responds to the caller through Cognitive Services' Text to Speech. 
This sample application configured for accepting input speech until the caller terminates the call or a long silence is detected.
This sample application is also capable of making multiple concurrent inbound calls. The application is a web-based application built on Java's Spring framework.


## Prerequisites

- An Azure account with an active subscription. [Create an account for free](https://azure.microsoft.com/free/?WT.mc_id=A261C142F). 
- A deployed Communication Services resource. [Create a Communication Services resource](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource).
- A [phone number](https://learn.microsoft.com/en-us/azure/communication-services/quickstarts/telephony/get-phone-number) in your Azure Communication Services resource that can make outbound calls. NB: phone numbers are not available in free subscriptions.
- [Java Development Kit (JDK) Microsoft.OpenJDK.17](https://learn.microsoft.com/en-us/java/openjdk/download)
- [Apache Maven](https://maven.apache.org/download.cgi)
- Create and host a Azure Dev Tunnel. Instructions [here](https://learn.microsoft.com/en-us/azure/developer/dev-tunnels/get-started)
- Create an Azure Cognitive Services resource. For details, see Create an Azure Cognitive Services Resource.
- An Azure OpenAI Resource and Deployed Model. See https://learn.microsoft.com/en-us/azure/ai-services/openai/how-to/create-resource?pivots=web-portal.

## Before running the sample for the first time

- Open the application.yml file in the resources folder to configure the following settings

    - `connectionstring`: Azure Communication Service resource's connection string.
    - `basecallbackuri`: Base url of the app. For local development use dev tunnel url.
    - `cognitiveServicesUrl`: The Cognitive Services endpoint
    - `azureOpenAiServiceKey`: Open AI's Service Key
    - `azureOpenAiServiceEndpoint`: Open AI's Service Endpoint
    - `openAiModelName`: Open AI's Model name
    - `agentPhoneNumber`: Agent Phone Number


### Setup and host your Azure DevTunnel

[Azure DevTunnels](https://learn.microsoft.com/en-us/azure/developer/dev-tunnels/overview) is an Azure service that enables you to share local web services hosted on the internet. Use the commands below to connect your local development environment to the public internet. This creates a tunnel with a persistent endpoint URL and which allows anonymous access. We will then use this endpoint to notify your application of calling events from the ACS Call Automation service.

```bash
devtunnel create --allow-anonymous
devtunnel port create -p 8080
devtunnel host
```

### Run the application

- Navigate to the directory containing the pom.xml file and use the following mvn commands:
    - Compile the application: mvn compile
    - Build the package: mvn package
    - Execute the app: mvn exec:java
- Access the Swagger UI at http://localhost:8080/swagger-ui.html
    - Try the GET /outboundCall to run the Sample Application