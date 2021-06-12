---
page_type: sample
languages:
- javascript
- nodejs
- java
products:
- azure
- azure-communication-services
---

[![Deploy to Azure](https://aka.ms/deploytoazurebutton)](https://portal.azure.com/#create/Microsoft.Template/uri/https%3A%2F%2Fraw.githubusercontent.com%2FAzure-Samples%2Fcommunication-services-web-calling-hero%2Fmain%2Fdeploy%2Fazuredeploy.json)

# Group Calling Sample

This is a sample application to show how the Azure Communication Services Calling Web SDK can be used to build a group calling experience.
The client-side application is a React based user interface which uses Redux for handling complex state while leveraging Microsoft Fluent UI.
Powering this front-end is a Java web application powered by Spring boot to connect this application with Azure Communication Services.

A separate branch with Teams Interop capabilities is [available](https://github.com/Azure-Samples/communication-services-web-calling-hero/blob/teams-interop/README.md). Teams Interop is in public preview and uses beta SDKs that are not meant for production use. Please use the main branch sample for any production scenarios.

Additional documentation for this sample can be found on [Microsoft Docs](https://docs.microsoft.com/azure/communication-services/samples/calling-hero-sample).

![Homepage](./Media/homepage-sample-calling.png)

## Prerequisites

- Create an Azure account with an active subscription. For details, see [Create an account for free](https://azure.microsoft.com/free/?WT.mc_id=A261C142F).
- [Node.js (12.18.4 and above)](https://nodejs.org/en/download/).
- [InetelliJ (2021 or above)](https://www.jetbrains.com/idea/).
- [Visual Studio code (2019 and above)](https://code.visualstudio.com/download).
- [Spring boot framework v- 2.5.0](https://spring.io/projects/spring-boot).
- Create an Azure Communication Services resource. For details, see [Create an Azure Communication Resource](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource). You'll need to record your resource **connection string** for this quickstart.

## Code structure

- ./Calling/ClientApp: frontend client
	- ./Calling/ClientApp/src
		- ./Calling/ClientApp/src/Components : React components to help build the client app calling experience
		- ./Calling/ClientApp/src/Containers : Connects the redux functionality to the React components
		- ./Calling/ClientApp/src/Core : Containers a redux wrapper around the Azure Communication Services Web Calling SDK
	- ./ClientApp/src/index.js : Entry point for the client app
- ./Calling/acsCalling/src/main/java/com/acsCalling/acsCalling/controllers : Server app core logic for client app to get a token to use with the Azure Communication Services Web Calling SDK
- ./Calling/acsCalling/src/main/java/com/acsCalling/acsCalling/AcsCallingApplication.java  : Entry point for the server app program logic
- ./Calling/acsCalling/pom.xml : XML file which contains project and package configurations
- ./Calling/acsCalling/src/main/resources/config.properties : config file which contains user level configurations

## Call recording management from client app

[!NOTE] This API is provided as a preview for developers and may change based on feedback that we receive. Do not use this API in a production environment. To use this api please use 'beta' release of ACS Calling Web SDK

Call recording is an extended feature of the core Call API. You first need to obtain the recording feature API object:

```JavaScript
const callRecordingApi = call.api(Features.Recording);
```
Then, to check if the call is being recorded, inspect the `isRecordingActive` property of `callRecordingApi`, it returns Boolean.

```JavaScript
const isRecordingActive = callRecordingApi.isRecordingActive;
```
You can also subscribe to recording changes:

```JavaScript
const isRecordingActiveChangedHandler = () => {
  console.log(callRecordingApi.isRecordingActive);
};

callRecordingApi.on('isRecordingActiveChanged', isRecordingActiveChangedHandler);
```

Get server call id which can be used to start or stop a recording sessions:

Once the call is connected use the `getServerCallId` method to get the server call id.

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

## Create a calling server client

To create a calling server client, you'll use your Communication Services connection string and pass to `CallingServerClientBuilder` class.

```java
NettyAsyncHttpClientBuilder httpClientBuilder = new NettyAsyncHttpClientBuilder();
CallingServerClientBuilder builder = new CallingServerClientBuilder().httpClient(httpClientBuilder.build())
		.connectionString(connectionString);
CallingServerClient callingServerClient = builder.buildClient();
```

## Initialize server call

To initialize `ServerCall` object, you will use `CallingServerClient` object and `serverCallId` received in response of method `getServerCallId` on client side.

```java
this.serverCall = this.callingServerClient.initializeServerCall(serverCallId);
```

## Start recording session using 'startRecordingWithResponse' server API

Use the  server call id received in response of method `getServerCallId`.

```java
recordingStateCallbackUri = new URI(recordingStateCallbackUrl);
 Response<StartCallRecordingResponse> response = this.serverCall.startRecordingWithResponse(String.valueOf(recordingStateCallbackUri),null);
```
The `startRecordingWithResponse` API response contains the recording id of the recording session.

## Stop recording session using 'stopRecordingWithResponse' server API

Use the  recording id received in response of  `startRecordingWithResponse`.

```java
 this.serverCall.stopRecordingWithResponse(recordingId, null);
```

## Before running the sample for the first time
1. Open an instance of PowerShell, Windows Terminal, Command Prompt or equivalent and navigate to the directory that you'd like to clone the sample to.
2. `git clone https://github.com/Azure-Samples/communication-services-web-calling-hero.git`
3. Get the `Connection String` from the Azure portal. For more information on connection strings, see [Create an Azure Communication Resources](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource)
4. Once you get the `Connection String`, add the connection string to the **resources/config.properties** file found under the ./Calling/acsCalling/src/main/resources folder. Input your connection string in the variable: `Connectionstring`.

## Locally deploying the sample app

1. Build frontend code from .\Calling\acsCalling\src\main\frontend using following commands
	- npm install
	- npm run build

2. Build java code from .\Calling\acsCalling : mvn clean install  

3. Run app locally
	- mvn spring-boot:run
	- Browse http://localhost:8080 

### Troubleshooting

1. Solution doesn\'t build, it throws errors during NPM installation/build
	- Check if all the config keys are present
	- Run mvn package, then mvn clean install

2. The app shows an "Unsupported browser" screen but I am on a [supported browser](https://docs.microsoft.com/azure/communication-services/concepts/voice-video-calling/calling-sdk-features#calling-client-library-browser-support).

	If your app is being served over a hostname other then localhost, you must serve traffic over https and not http.

## Publish to Azure
1. Build frontend code from .\Calling\acsCalling\src\main\frontend using following commands
	- npm install
	- npm run build

2. Build java code from .\Calling\acsCalling using : mvn clean install  

3. Create Azure container Registry and update created registry name in pom.xml under the tag <docker.image.prefix> - follow steps in [link](https://docs.microsoft.com/en-us/azure/developer/java/spring-framework/deploy-spring-boot-java-app-on-linux) on how to create Azure Container Registry

4. Login to Azure and Azure Container Registry :

	- az login
	- az acr login --name <Azure Container Registry name>

5. Build docker image using : mvn compile jib:dockerBuild
	- It creates a docker image and gives name as <Azure Container Registry name>/acscalling

6. Run image locally to validate using : docker run -it --rm -p 8080:8080 <Azure Container Registry name>/acscalling

7. Push docker image to ACR  using : docker push <Azure Container Registry name>/acscalling

8. Create web app - follow steps in [link](https://docs.microsoft.com/en-us/azure/developer/java/spring-framework/deploy-spring-boot-java-app-on-linux)

**Note**: While you may use http://localhost for local testing, the sample when deployed will only work when served over https. The SDK [does not support http](https://docs.microsoft.com/en-us/azure/communication-services/concepts/voice-video-calling/calling-sdk-features#user-webrtc-over-https).

## Building off of the sample

If you would like to build off of this sample to add calling capabilities to your own awesome application, keep a few things in mind:

- The sample serves a Single Page Application. This has a few implications.
  - By default, the backend disables Cross-Origin Resource Sharing (CORS). If you'd like to serve the backend APIs from a different domain than the static content, you must enable (restricted) CORS. This can be done by configuring a middleware in the backend in ./Calling/Startup.cs, or by configuring your server framework to modify HTTP response headers.

## Additional Reading

- [Azure Communication Calling SDK](https://docs.microsoft.com/azure/communication-services/concepts/voice-video-calling/calling-sdk-features) - To learn more about the calling web sdk
- [Redux](https://redux.js.org/) - Client-side state management
- [FluentUI](https://developer.microsoft.com/fluentui#/) - Microsoft powered UI library
- [React](https://reactjs.org/) - Library for building user interfaces
- [Spring boot](https://www.jetbrains.com/help/idea/creating-and-running-your-first-restful-web-service.html) - Spring boot - Your first RESTful web service