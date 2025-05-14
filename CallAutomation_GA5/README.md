|page_type| languages                             |products
|---|---------------------------------------|---|
|sample| <table><tr><td>Java</tr></td></table> |<table><tr><td>azure</td><td>azure-communication-services</td></tr></table>|

# Call Automation - GA5 Sample

In this sample, we cover how you can use Call Automation SDK

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

### Deploy the application

`deploy-to-azure.sh` bash script automates the **build**, **configuration**, and **deployment** of your Spring Boot Swagger web app to Azure App Service.  
This script assumes you have the Azure CLI, Maven, and Java installed, and you are running it from your project root.

**How to use:**
1. Replace the variable values at the top with your actual configuration.
2. Make the script executable:  
   `chmod +x deploy-to-azure.sh`
3. Run the script:  
   `./deploy-to-azure.sh`

**This script will:**
- Build your app
- Create Azure resources if they don’t exist
- Deploy your JAR
- Output your Swagger UI URL

