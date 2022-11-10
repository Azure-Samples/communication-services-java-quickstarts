---
page_type: sample
languages:
- java
products:
- azure
- azure-communication-services
- azure-communication-callAutomation
---

# Call Automation - Simple IVR Solution

This sample application shows how the Azure Communication Services - Call Automation SDK can be used to build IVR related solutions. This sample makes an outbound call to a phone number performs dtmf recognition and the application plays next audio based on the key pressed by the callee. 
This sample application configured for accepting tone-1  through tone-5 , If the callee pressed any other key than expected, an invalid audio tone will be played and then call will be disconnected. This sample application is also capable of making multiple concurrent outbound calls.
The application is an app service application built on java.

## Prerequisites

- Create an Azure account with an active subscription. For details, see [Create an account for free](https://azure.microsoft.com/free/)
- [Visual Studio Code](https://code.visualstudio.com/download)
- [Java Development Kit (JDK) version 11 or above](https://learn.microsoft.com/en-us/azure/developer/java/fundamentals/java-jdk-install)
- [Apache Maven](https://maven.apache.org/download.cgi)
- [Spring Boot framework v- 2.5.0](https://spring.io/projects/spring-boot)
- [Docker desktop](https://www.docker.com/products/docker-desktop)
- [Create an Azure Communication Resource](https://learn.microsoft.com/en-us/azure/communication-services/quickstarts/create-communication-resource). You'll need to record your resource\'s **connection string** for this quickstart.
- [Create container registry](https://learn.microsoft.com/en-us/azure/developer/java/spring-framework/deploy-spring-boot-java-app-on-linux#create-an-azure-container-registry-to-use-as-a-private-docker-registry)
- [Configuring the webhook](https://learn.microsoft.com/en-us/azure/devops/service-hooks/services/webhooks?view=azure-devops) for **Microsoft.Communication.IncomingCall** event.


## Before running the sample for the first time

1. Open an instance of PowerShell, Windows Terminal, Command Prompt or equivalent and navigate to the directory that you'd like to clone the sample to.
2. git clone https://github.com/Azure-Samples/Communication-Services-java-quickstarts.git.
3. After this add the config keys to the **config.properties** file.
	- ResourceConnectionString: Azure Communication Service resource's connection string.
	- ACSAlternatePhoneNumber: Azure Communication Service acquired phone number.
	- CallbackUriBase: URI of the deployed app service or ngrok url.
	- ParticipantToAdd: Target phone number to add as participant.

### Locally running the Call Automation Simple IVR app
1. Build java code.

	```bash
	mvn clean install  
	```

2. Run app locally.

	```bash
	mvn spring-boot:run
	```

3. Used the ngrok URl and point to localhost.

### Publish the Call Automation Simple IVR to Azure WebApp

1. Build java code :

	```bash
	mvn clean install  
	```

2. Run app locally using :

	```bash
	mvn spring-boot:run
 	```

3. Configure Maven to build image to your Azure Container Registry.

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


4. Login to Azure.

	```azurecli
	az login
	```

5. Login to Azure Container Registry.

	```azurecli
	az acr login --name <registryName>
	```

6. Build docker image.

	```bash
	mvn compile jib:dockerBuild  
	```

7. Run image locally to validate.

	```bash
	docker run -it --rm -p 8080:8080   <registryName>.azurecr.io/<dockerImageName>
	```

8. Push docker image to Azure Container Registry.

	```bash
	docker push <registryName>.azurecr.io/<dockerImageName>
	```

9. Create web app by following steps in link : [Create Web App](https://learn.microsoft.com/en-us/azure/developer/java/spring-framework/deploy-spring-boot-java-app-on-linux#create-a-web-app-on-linux-on-azure-app-service-using-your-container-image)

10. After publishing, add the following configurations on azure portal (under app service's configuration section).

	- ResourceConnectionString: Azure Communication Service resource's connection string.
	- ACSAlternatePhoneNumber: Azure Communication Service acquired phone number.
	- CallbackUriBase: URI of the deployed app service or ngrok url.
	- ParticipantToAdd: Target phone number to add as participant.

### Create Webhook for Microsoft.Communication.IncomingCall event and Microsoft.Communication.RecordingFileStatusUpdated event
1. Configure webhook from ACS events tab for incoming call event.
 	- Manually configuring the webhook using this command. use above published "'https://<IncomingCallMediaStreaming-URL>/OnIncomingCall" URL as webhook URL.

	```
	armclient put "/subscriptions/<Subscriptin-ID>/resourceGroups/<resource group name>/providers/Microsoft.Communication/CommunicationServices/<CommunicationService Name>/providers/Microsoft.EventGrid/eventSubscriptions/<WebHookName>?api-version=2020-06-01" "{'properties':{'destination':{'properties':{'endpointUrl':'<webhookurl>'},'endpointType':'WebHook'},'filter':{'includedEventTypes': ['Microsoft.Communication.IncomingCall']}}}" -verbose

	```



2. Detailed instructions on publishing the app to Azure are available at [Publish a Web app](https://docs.microsoft.com/visualstudio/deployment/quickstart-deploy-to-azure?view=vs-2019).

**Note**: While you may use http://localhost for local testing, the sample when deployed will only work when served over https. The SDK [does not support http](https://docs.microsoft.com/azure/communication-services/concepts/voice-video-calling/calling-sdk-features#user-webrtc-over-https).

### Troubleshooting

1. Solution doesn\'t build / It throws errors during MVN installation/build

	-  Check if all the config keys are present, and rebuild with `mvn package`, then `mvn clean install`

	- After installing the JDK and building, if you see "invalid target release: 11", verify that your JAVA_HOME variable does in fact point to your Java 11 installation (as opposed to a previous installation).
