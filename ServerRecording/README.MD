---
page_type: sample
languages:
- java
products:
- azure
- azure-communication-services
---

[![Deploy to Azure](https://aka.ms/deploytoazurebutton)](https://portal.azure.com/#create/Microsoft.Template/uri/https%3A%2F%2Fraw.githubusercontent.com%2FAzure-Samples%2Fcommunication-services-web-calling-hero%2Fmain%2Fdeploy%2Fazuredeploy.json)

# Recording APIs Sample

This is a sample application to show how the Azure Communication Services server calling SDK can be used to build a call recording feature.

Its a Java web application powered by Spring boot to connect this application with Azure Communication Services.

## Prerequisites

- Create an Azure account with an active subscription. For details, see [Create an account for free](https://azure.microsoft.com/free/?WT.mc_id=A261C142F)
- [Java Development Kit (JDK) version 11 or above](https://docs.microsoft.com/azure/developer/java/fundamentals/java-jdk-install)
- [Apache Maven](https://maven.apache.org/download.cgi)
- [Visual Studio code (2019 and above)](https://code.visualstudio.com/download)
- [Spring boot framework v- 2.5.0](https://spring.io/projects/spring-boot)
- Create an Azure Communication Services resource. For details, see [Create an Azure Communication Resource](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource). You'll need to record your resource **connection string** for this quickstart.
- Create a webhook and subscribe to the recording events [Create webhook](https://docs.microsoft.com/azure/communication-services/quickstarts/voice-video-calling/download-recording-file-sample)

## Code structure

- ./ServerRecording/src/main/java/com/acsrecording/api/controllers : Server app core logics to make Api calls that connects with the Azure Communication Services Web Calling SDK
- ./ServerRecording/pom.xml : XML file which contains project and package configurations
- ./ServerRecording/src/main/resources/config.properties : config file which contains user level configurations

## Before running the sample for the first time
1. Get the `Connection String` from the Azure portal. For more information on connection strings, see [Create an Azure Communication Resources](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource)
2. Once you get the config keys add the keys to the **resources/config.properties** file found under the ./ServerRecording/src/main/resources folder.
	- Input your connection string in the variable: `Connectionstring`
	- Input your blob storage connectionString string in the variable: `BlobStorageConnectionString`
	- Input blob container name for recorded media in the variable `ContainerName`
	- Input recording callback url for start recording api in the variable `CallbackUri`

## Locally deploying the sample app

1. Build java code : mvn clean install  

2. Run app locally : mvn spring-boot:run

3. Use postman or any debugging tool and open url - http://localhost:8080 

### Troubleshooting

1. Solution doesn\'t build, it throws errors during NPM installation/build

	- Check if all the config keys are present

	- Run mvn package, then mvn clean install

## Publish to Azure

1. Build java code using : mvn clean install  

2. Run app locally using : mvn spring-boot:run

3. Create container registry - follow steps in link - https://docs.microsoft.com/azure/developer/java/spring-framework/deploy-spring-boot-java-app-on-linux

4. Login to Azure :

	```azurecli
	using az login
	```

5. Login to Azure Container Registry :

	```azurecli
	az acr login --name <registryName>
	```

6. Build docker image using : mvn compile jib:dockerBuild  

7. Run image locally to validate using : docker run -it --rm -p 8080:8080   <registryName>.azurecr.io/<projectNameAndVersion>

8. Push docker image to ACR  using : docker push  <registryName>.azurecr.io/<projectNameAndVersion>

9.Create web app by following steps in link : https://docs.microsoft.com/azure/developer/java/spring-framework/deploy-spring-boot-java-app-on-linux

## Additional Reading

- [Azure Communication Calling SDK](https://docs.microsoft.com/azure/communication-services/concepts/voice-video-calling/calling-sdk-features) - To learn more about the calling web sdk