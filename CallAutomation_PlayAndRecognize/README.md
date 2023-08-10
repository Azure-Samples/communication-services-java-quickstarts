|page_type| languages                             |products
|---|---------------------------------------|---|
|sample| <table><tr><td>Java</tr></td></table> |<table><tr><td>azure</td><td>azure-communication-services</td></tr></table>|

# Call Automation - Play and Recognize Sample

This sample application shows how the Azure Communication Services  - Call Automation SDK can be used to build IVR related solutions. 
It makes an outbound call to a phone number and plays audio and recognize speech. The application is a web-based application built on Java's Spring framework.

## Prerequisites

- Create an Azure account with an active subscription. For details, see [Create an account for free](https://azure.microsoft.com/free/)
- Create an Azure Communication Services resource. For details, see [Create an Azure Communication Resource](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource). You will need to record your resource **connection string** for this sample.
- Connect your Azure Cognitive Service to your Azure Communication Service. For details see [Connect Azure Communication Services with Azure AI services](https://learn.microsoft.com/en-us/azure/communication-services/concepts/call-automation/azure-communication-services-azure-cognitive-services-integration).
- A [phone number](https://learn.microsoft.com/en-us/azure/communication-services/quickstarts/telephony/get-phone-number) in your Azure Communication Services resource that can make outbound calls. NB: phone numbers are not available in free subscriptions.
- [Java Development Kit (JDK) Microsoft.OpenJDK.17](https://learn.microsoft.com/en-us/java/openjdk/download)
- [Apache Maven](https://maven.apache.org/download.cgi)
- Create and host a Azure Dev Tunnel. Instructions [here](https://learn.microsoft.com/en-us/azure/developer/dev-tunnels/get-started)

## Before running the sample for the first time

- Open the application.yml file in the resources folder to configure the following settings

    - `connectionstring`: Azure Communication Service resource's connection string.
    - `callerphonenumber`: Phone number associated with the Azure Communication Service resource.
    - `targetphonenumber`: Target Phone number.

      Format: "OutboundTarget(Phone Number)".

          For e.g. "+1425XXXAAAA"
    - `basecallbackuri`: Base url of the app. For local development use dev tunnel url.
    - `cognitiveServiceEndpoint`: Cognitive Service endpoint URI


### Setup and host your Azure DevTunnel

[Azure DevTunnels](https://learn.microsoft.com/en-us/azure/developer/dev-tunnels/overview) is an Azure service that enables you to share local web services hosted on the internet. Use the commands below to connect your local development environment to the public internet. This creates a tunnel with a persistent endpoint URL and which allows anonymous access. We will then use this endpoint to notify your application of calling events from the ACS Call Automation service.

```bash
devtunnel create --allow-anonymous
devtunnel port create -p 8080
devtunnel host
```

### Run the application

1. Run the application.
2. Open `http://localhost:8080/index.html` in a Web browser.
3. To initiate the call, click on the `Place a call!` button.
