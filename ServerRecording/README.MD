---
page_type: sample
languages:
- java
products:
- azure
- azure-communication-services
---
# Recording APIs Sample

This is a sample application to show how the Azure Communication Services server calling SDK can be used to build a call recording feature.

It's a Java web application powered by Spring Boot to connect this application with Azure Communication Services.

## Prerequisites

- [Visual Studio Code](https://code.visualstudio.com/download)
- [Java Development Kit (JDK) version 11 or above](https://docs.microsoft.com/azure/developer/java/fundamentals/java-jdk-install)
- [Apache Maven](https://maven.apache.org/download.cgi)
- [Spring Boot framework v- 2.5.0](https://spring.io/projects/spring-boot)
- An Azure account with an active subscription. For details, see here to [create an account for free](https://azure.microsoft.com/free/?WT.mc_id=A261C142F).
- [Create container registry](https://docs.microsoft.com/en-us/azure/developer/java/spring-framework/deploy-spring-boot-java-app-on-linux#create-an-azure-container-registry-to-use-as-a-private-docker-registry)
- [Create an Azure Communication Resource](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource). You'll need to record your resource\'s **connection string** for this quickstart.
- [Create a webhook](https://docs.microsoft.com/azure/communication-services/quickstarts/voice-video-calling/download-recording-file-sample) and subscribe to the recording events.

## Code structure
- ./src/main/java/com/acsrecording/api/Controller : Server app core logic to make Api calls that connect with the Azure Communication Services Web Calling SDK
- ./pom.xml : XML file which contains project and package configurations
- ./src/main/resources/config.properties : config file which contains user level configurations

## Before running the sample for the first time
1. Get the `Connection String` from the Azure portal. For more information on connection strings, see [Create an Azure Communication Resources](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource)
2. Once you get the config keys, add the keys to the **resources/config.properties** file found under the ./src/main/resources folder.
	- Input your ACS connection string in the variable: `ACSConnectionString`
	- Input your blob storage connection string in the variable: `BlobStorageConnectionString`
	- Input blob container name for recorded media in the variable `ContainerName`
	- Input recording callback url for start recording api in the variable `CallbackUri`

## Locally deploying the sample app

1. Build java code : `mvn clean install`

2. Run app locally : `mvn spring-boot:run`

3. Use [Postman](https://www.postman.com/) or any debugging tool and open url - http://localhost:8080 

## Troubleshooting

1. Solution doesn\'t build / It throws errors during MVN installation/build

	- Check if all the config keys are present, and rebuild with `mvn package`, then `mvn clean install`

	- After installing the JDK and building, if you see "invalid target release: 11", verify that your JAVA_HOME variable does in fact point to your Java 11 installation (as opposed to a previous installation). 

## Publish to Azure

1. Build java code :

	```
	mvn clean install
	```

1. Run app locally using :

	```
	mvn spring-boot:run
	```

1. Login to Azure :

	```azurecli
	az login
	```

1. Login to your Azure Container Registry :

	```azurecli
	az acr login --name <registryName>
	```

1. Build docker image using : 
	
	```
	mvn compile jib:dockerBuild
	```

1. Run image locally to validate using :
	```
	docker run -it --rm -p 8080:8080 {registryName}.azurecr.io/{projectNameAndVersion}
	```

1. Push docker image to ACR using : 
	```
	docker push  {registryName}.azurecr.io/{projectNameAndVersion}
	```

1. Create web app by following steps in link : https://docs.microsoft.com/en-us/azure/developer/java/spring-framework/deploy-spring-boot-java-app-on-linux#create-a-web-app-on-linux-on-azure-app-service-using-your-container-image

## Additional Reading

- [Azure Communication Calling SDK](https://docs.microsoft.com/azure/communication-services/concepts/voice-video-calling/calling-sdk-features) - To learn more about the calling web sdk.