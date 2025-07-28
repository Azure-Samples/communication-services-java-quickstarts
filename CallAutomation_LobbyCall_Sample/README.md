|page_type| languages                             |products
|---|---------------------------------------|---|
|sample| <table><tr><td>Java</td></tr></table> |<table><tr><td>azure</td><td>azure-communication-services</td></tr></table>|

# Call Automation - Lobby Call Sample

In this sample, we cover how you can use Call Automation SDK Lobby Call Sample.

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
2. `cognitiveServiceEndpoint`: Cognitive service endpoint.
3. `callbackUriHost`: Base url of the app. (For local development replace the dev tunnel url)
4. `pmaEndpoint` : PMA service endpoint.
5. `acsGeneratedId`: Communication Identifier generated through the ACS.
6. `webSocketToken`: Web Socket Token Key.