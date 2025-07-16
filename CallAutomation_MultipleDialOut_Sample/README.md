|page_type| languages                             |products
|---|---------------------------------------|---|
|sample| <table><tr><td>Java</tr></td></table> |<table><tr><td>azure</td><td>azure-communication-services</td></tr></table>|

# Call Automation - Multiple Dial Out Sample

In this sample, we cover how you can use Call Automation SDK Multiple Dial Out.

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
    - Try the GET and POST methods to run the Sample Application

### Configuring settings

In the swagger app, provide these values for the setConfiguration endpoint to configure settings

1. `acsConnectionString`: Azure Communication Service resource's connection string.
2. `acsInboundPhoneNumber`: Inbound Phone number associated with the Azure Communication Service resource. For e.g. "+1425XXXAAAA"
3. `acsOutboundPhoneNumber`: Outbound Phone number associated with the Azure Communication Service resource. For e.g. "+1425XXXAAAA"
4. `userPhoneNumber`: User phone number to add in the call. For e.g. "+1425XXXAAAA"
5. `acsTestIdentity2`: An ACS Communication Identifier to add in the call.
6. `acsTestIdentity3`: Another ACS Communication Identifier to add in the call.
7. `callbackUriHost`: Base url of the app. (For local development replace the dev tunnel url)
8. `pmaEndpoint` : PMA service endpoint.
