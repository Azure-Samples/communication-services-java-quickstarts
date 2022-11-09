---
page_type: sample
languages:
- java
products:
- azure
- azure-communication-services
---

# Recording APIs Sample

This is a sample application to show how the Azure Communication Services Call Automation SDK can be used to build a call recording feature.

It's a Java web application powered by Spring Boot to connect this application with Azure Communication Services.

## Prerequisites

- [Create an Azure Communication Resource](https://learn.microsoft.com/en-us/azure/communication-services/quickstarts/create-communication-resource). You'll need to record your resource\'s **connection string** for this quickstart.
- [Visual Studio Code](https://code.visualstudio.com/download)
- [Java Development Kit (JDK) version 11 or above](https://learn.microsoft.com/en-us/azure/developer/java/fundamentals/java-jdk-install)
- [Apache Maven](https://maven.apache.org/download.cgi)
- [Spring Boot framework v- 2.5.0](https://spring.io/projects/spring-boot)
- [Docker desktop](https://www.docker.com/products/docker-desktop)
- An Azure account with an active subscription. For details, see here to [create an account for free](https://azure.microsoft.com/free/?WT.mc_id=A261C142F).
- [Create container registry](https://learn.microsoft.com/en-us/azure/developer/java/spring-framework/deploy-spring-boot-java-app-on-linux#create-an-azure-container-registry-to-use-as-a-private-docker-registry)
- [Create a webhook](https://learn.microsoft.com/en-us/azure/communication-services/quickstarts/voice-video-calling/download-recording-file-sample) and subscribe to the recording events.


## Code structure

- ./src/main/java/com/acsrecording/api/Controller : Server app core logic to make Api calls that connect with the Azure Communication Services Web Calling SDK
- ./pom.xml : XML file which contains project and package configurations
- ./src/main/resources/config.properties : config file which contains user level configurations

## Before running the sample for the first time
1. Get the `Connection String` from the Azure portal. For more information on connection strings, see [Create an Azure Communication Resources](https://learn.microsoft.com/en-us/azure/communication-services/quickstarts/create-communication-resource).
2. Once you get the config keys, add the keys to the **resources/config.properties** file found under the ./src/main/resources folder.
	- Input your ACS connection string in the variable: `ACSConnectionString`
	- Input your blob storage connection string in the variable: `BlobStorageConnectionString`
	- Input blob container name for recorded media in the variable `ContainerName`

## Locally deploying the sample app

1. Build java code.

	```bash
	mvn clean install  
	```

1. Run app locally.

	```bash
	mvn spring-boot:run
	```

1. Use [Postman](https://www.postman.com/) or any debugging tool to test the recording APIs

	- Start a recording :

	http://localhost:8080/startRecording?serverCallId=XXXX

	- Stop a recording :

	http://localhost:8080/stopRecording?serverCallId=XXXX

	To get the `serverCallId`, you first need to build a client application. Using the client application initiate a call. Once the call is connected use the getServerCallId method to get the server call id.

	```JavaScript
	callAgent.on('callsUpdated', (e: { added: Call[]; removed: Call[] }): void => {
	    e.added.forEach((addedCall) => {
	        addedCall.on('stateChanged', (): void => {
	            if (addedCall.state === 'Connected') {
	                addedCall.info.getServerCallId().then(result => {
	                    dispatch(setServerCallId(result));
	                }).catch(err => {
	                    console.log(err);
	                });
	            }
	        });
	    });
	});
	```

 For complete details on how to build a client application using ACS calling SDK, refer to https://learn.microsoft.com/en-us/azure/communication-services/samples/calling-hero-sample?pivots=platform-web

### Troubleshooting

1. Solution doesn\'t build / It throws errors during MVN installation/build

	-  Check if all the config keys are present, and rebuild with `mvn package`, then `mvn clean install`

	- After installing the JDK and building, if you see "invalid target release: 11", verify that your JAVA_HOME variable does in fact point to your Java 11 installation (as opposed to a previous installation).

## Publish to Azure

1. Build java code :

	```bash
	mvn clean install  
	```

1. Run app locally using :

	```bash
	mvn spring-boot:run
 	```

1. Configure Maven to build image to your Azure Container Registry.

 	- Navigate to the completed project directory for your Spring Boot application and open the pom.xml file with a text editor.
 	- Update the <properties> collection in the pom.xml file with the latest version of [jib-maven-plugin](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin), login server value, and access settings for your Azure Container Registry created using [Create container registry](https://learn.microsoft.com/en-us/azure/developer/java/spring-framework/deploy-spring-boot-java-app-on-linux#create-an-azure-container-registry-to-use-as-a-private-docker-registry). For example:

		```xml
		<properties>
			<jib-maven-plugin.version>2.5.2</jib-maven-plugin.version>
			<docker.image.prefix>{docker.image.prefix}</docker.image.prefix>
			<java.version>1.8</java.version>
		</properties>
		```
  	- Add [jib-maven-plugin](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin) to the <plugins> collection in the pom.xml file. This example uses version 2.5.2.

		Specify the base image at ```<from>/<image>```, here ```mcr.microsoft.com/java/jre:11-zulu-alpine```. Specify the name of the final image to be built from the base in ```<to>/<image>```.

		The {docker.image.prefix} is the Login server of Azure Container Registry. The {project.artifactId} is the name and version number of the JAR file from the first Maven build of the project.

		```xml
		<plugin>
		<artifactId>jib-maven-plugin</artifactId>
		<groupId>com.google.cloud.tools</groupId>
		<version>${jib-maven-plugin.version}</version>
		<configuration>
			<from>
				<image>mcr.microsoft.com/java/jre:11-zulu-alpine</image>
			</from>
			<to>
				<image>${docker.image.prefix}/${project.artifactId}</image>
			</to>
		</configuration>
		</plugin>

		```


1. Login to Azure.

	```azurecli
	az login
	```

1. Login to Azure Container Registry.

	```azurecli
	az acr login --name <registryName>
	```

1. Build docker image.

	```bash
	mvn compile jib:dockerBuild  
	```

1. Run image locally to validate.

	```bash
	docker run -it --rm -p 8080:8080   <registryName>.azurecr.io/<dockerImageName>
	```

1. Push docker image to Azure Container Registry.

	```bash
	docker push <registryName>.azurecr.io/<dockerImageName>
	```

1. Create web app by following steps in link : [Create Web App](https://learn.microsoft.com/en-us/azure/developer/java/spring-framework/deploy-spring-boot-java-app-on-linux#create-a-web-app-on-linux-on-azure-app-service-using-your-container-image)

## Additional Reading

- [Azure Communication Calling SDK](https://learn.microsoft.com/en-us/azure/communication-services/concepts/voice-video-calling/calling-sdk-features) - To learn more about the calling web sdk.
