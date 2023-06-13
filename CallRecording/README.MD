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
- Download and install [Ngrok](https://www.ngrok.com/download). As the sample is run locally, Ngrok will enable the receiving of all the events.
- Generate Ngrok Url by using below steps.
    - Open command prompt or powershell window.
    - Navigate to the directory, where Ngrok.exe file is located. Then, run:
    - ngrok http {portNumber}(For e.g. ngrok http 8080)
    - Get Ngrok Url generated. Ngrok Url will be in the form of e.g. "https://95b6-43-230-212-228.ngrok-free.app"
- [Create a webhook](https://learn.microsoft.com/en-us/azure/communication-services/quickstarts/voice-video-calling/download-recording-file-sample) and subscribe to the recording events.

## Code structure

- ./src/main/java/com/acsrecording/api/Controller : Server app core logic to make Api calls that connect with the Azure Communication Services Web Calling SDK
- ./pom.xml : XML file which contains project and package configurations
- ./src/main/resources/config.properties : config file which contains user level configurations

## Clone the code local and update appsettings

1. Open an instance of PowerShell, Windows Terminal, Command Prompt or equivalent and navigate to the directory that you'd like to clone the sample to.
2. Run `git clone https://github.com/Azure-Samples/communication-services-java-quickstarts`
3. Once you get the code on local machine, navigate to **CallRecording//src/main/resources/config.properties** file found under the CallRecording folder.
4. Update the values for below.

	| Key | Value | Description |
	| -------- | -------- | -------- |
	| `Connectionstring`    | \<ACS Connection String>   | Input your ACS connection string in the variable   |
	| `ACSAcquiredPhoneNumber`    | \<ACS Acquired Number>   | Phone number associated with the Azure Communication Service resource   |
	| `BaseUri`    | \<dev tunnel url>   | Base url is the Ngrok Url.    |
	| `targetPhoneNumber`    | \<target Phone Number>   | Target Phone Number   |

## Locally run the sample app

1. Build java code.

	```bash
	mvn clean install  
	```

2. Run app locally.

	```bash
	mvn spring-boot:run
	```

3. Import `call-recording\data\Recordings API.postman_collection.json` file into postman, you get `Recordings API` collection. For details, see [Importing and exporting data](https://learning.postman.com/docs/getting-started/importing-and-exporting-data/)

4. Select `Recordings API`, under Variables, update `HOST_URL` and `TARGET_PHONE_NUMBER` before making calls.

## Step by step guid for testing recording APIs via postman.

Once App is running locally,
1. Create webhook (Follow "Create Webhook for Microsoft.Communication.RecordingFileStatus event" section below )
2. Start a call by invoking Qutbound Call request. 
	- Accept the call on Target PSTN Phone number, Keep call running.
5. Send Request to StartRecording.
	- Recording would be started.
6. (Optional) PauseRecording, ResumeRecording and GetRecordingState.
7. Send Request for StopRecording.
8. Send Request for DownloadRecording from server, only last recorded file will be downloaded.
9. Send Request for DeleteRecording at server.

 For complete details on how to build a client application using ACS calling SDK, refer to https://learn.microsoft.com/en-us/azure/communication-services/samples/calling-hero-sample?pivots=platform-web


## Create Webhook for Microsoft.Communication.RecordingFileStatus event

Call Recording enables you to record multiple calling scenarios available in Azure Communication Services by providing you with a set of APIs to start, stop, pause and resume recording. To learn more about it, see [this guide](https://learn.microsoft.com/en-us/azure/communication-services/concepts/voice-video-calling/call-recording). 
1. Navigate to your Communication Service resource on Azure portal and select `Events` from the left side blade.
2. Click `+ Event Subscription` to create a new subscription, provide `Name` field value. 
3. Under Topic details, choose a System Topic or create new, no changes required if its already have topic name.  
4. Under `Event Types` Filter for `Recording File Status Updated(Preview)` event. 
5. Choose `Endpoint Type` as `Web Hook` and provide the public url generated by Ngrok. It would look like `https://95b6-43-230-212-228.ngrok-free.app/getRecordingFile`.  
6. Click `Create` to complete the event grid subscription. The subscription is ready when the provisioning status is marked as succeeded.  
**Note:** Application should be running to able to create the `Web Hook` successfully.

### Troubleshooting

1. Solution doesn\'t build / It throws errors during MVN installation/build
	- Check if all the config keys are present, and rebuild with `mvn package`, then `mvn clean install`
	- After installing the JDK and building, if you see "invalid target release: 11", verify that your JAVA_HOME variable does in fact point to your Java 11 installation (as opposed to a previous installation).

## Additional Reading

- [Azure Communication Calling SDK](https://learn.microsoft.com/en-us/azure/communication-services/concepts/voice-video-calling/calling-sdk-features) - To learn more about the calling web sdk.
