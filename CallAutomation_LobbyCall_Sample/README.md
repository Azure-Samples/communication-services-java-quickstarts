| page_type | languages                             | products                                                                    |
| --------- | ------------------------------------- | --------------------------------------------------------------------------- |
| sample    | <table><tr><td>Java</td></tr></table> | <table><tr><td>azure</td><td>azure-communication-services</td></tr></table> |

# Call Automation - Lobby Call Sample

This sample demonstrates how to utilize the Call Automation SDK to implement a Lobby Call scenario. Users join a lobby call and remain on hold until an user in the target call confirms their participation. Once approved, Call Automation (bot) automatically connects the lobby users to the designated target call.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Design](#design)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Endpoints](#api-endpoints)
- [Event Grid Integration](#event-grid-integration)
- [WebSocket Support](#websocket-support)
- [Usage Flow](#usage-flow)
- [Development Features](#development-features)
- [Technology Stack](#technology-stack)
- [Troubleshooting](#troubleshooting)
- [Additional Resources](#additional-resources)

---

## Overview

This project provides a sample implementation for managing lobby calls using Azure Communication Services and the Call Automation SDK with Java and Spring Boot. The sample uses a client application (JavaScript sample) available in [Web Client Quickstart](https://github.com/Azure-Samples/communication-services-javascript-quickstarts/tree/users/v-kuppu/LobbyCallConfirmSample).

## Features

- **Lobby Call Management**: Automatically answer incoming calls and place them in a lobby
- **Text-to-Speech**: Play waiting messages to lobby participants using Azure Cognitive Services
- **Participant Management**: Move participants between lobby and target calls
- **WebSocket Support**: Real-time communication for call state updates
- **Event-Driven Architecture**: Handle Call Automation events via webhooks
- **Dev Tunnel Integration**: Easy development with Azure Dev Tunnels for webhook delivery

---

## Design

![Lobby Call Support](./resources/Lobby_Call_Support_Scenario.jpg)

---

## Prerequisites

- **Azure Account:** An Azure account with an active subscription.  
  https://azure.microsoft.com/free/?WT.mc_id=A261C142F.
- **Communication Services Resource:** A deployed Communication Services resource.  
  https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource.
- **Azure Cognitive Services Resource:** For text-to-speech functionality.  
  https://docs.microsoft.com/azure/cognitive-services/
- **Azure Dev Tunnel:** https://learn.microsoft.com/en-us/azure/developer/dev-tunnels/get-started.

- [Java](https://www.oracle.com/java/technologies/javase-downloads.html) 17 or above.
- [Maven](https://maven.apache.org/download.cgi) 3.6 or above.

---

## Getting Started

### Clone the Source Code

1. Open PowerShell, Windows Terminal, Command Prompt, or equivalent.
2. Navigate to your desired directory.
3. Clone the repository:
   ```sh
   git clone https://github.com/Azure-Samples/communication-services-java-quickstarts.git
   ```

### Setup and Host Azure Dev Tunnel

[Azure DevTunnels](https://learn.microsoft.com/en-us/azure/developer/dev-tunnels/overview) enables you to expose your local development server to the internet for webhook delivery.

```bash
devtunnel create --allow-anonymous
devtunnel port create -p 8443
devtunnel host
```

Copy the HTTPS URL provided by dev tunnel (e.g., `https://abc123-8443.inc1.devtunnels.ms`) for use in configuration.

---

## Configuration

Before running the application, configure your ACS settings in `src/main/resources/application.yml`:

| Setting                    | Description                                                                                                                                          | Example Value                                                            |
| -------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| `acsConnectionString`      | The connection string for your Azure Communication Services resource. Find this in the Azure Portal under your resource Keys section.                | `"endpoint=https://<RESOURCE>.communication.azure.com/;accesskey=<KEY>"` |
| `cognitiveServiceEndpoint` | Azure Cognitive Services endpoint for text-to-speech functionality. Required for playing waiting messages to lobby participants.                     | `"https://<COGNITIVE-SERVICE>.cognitiveservices.azure.com/"`             |
| `callbackUriHost`          | The base URL where your app will listen for incoming events from Azure Communication Services. For local development, use your Azure Dev Tunnel URL. | `"https://<your-dev-tunnel>.devtunnels.ms/api/lobbyCallEventHandler"`    |
| `acsGeneratedId`           | Communication user ID for receiving lobby calls. Generate using ACS Identity SDK or Azure portal.                                                    | `"8:acs:<GUID>"`                                                         |

### How to Obtain These Values

- **acsConnectionString:**

  1. Go to the Azure Portal.
  2. Navigate to your Communication Services resource.
  3. Select "Keys & Connection String."
  4. Copy the "Connection String" value.

- **cognitiveServiceEndpoint:**

  1. Go to the Azure Portal.
  2. Navigate to your Cognitive Services resource.
  3. Copy the "Endpoint" value from the Overview or Keys section.

- **callbackUriHost:**

  1. Set up an Azure Dev Tunnel as described in the prerequisites.
  2. Use the public URL provided by the Dev Tunnel with the full webhook path: `https://<your-dev-tunnel>.devtunnels.ms/api/lobbyCallEventHandler`

- **acsGeneratedId:**
  1. Use the ACS web client or SDK to generate user identities.
  2. Store the generated identity string here.

#### Example `application.yml`

```yaml
acs:
  acsConnectionString: "<acsConnectionString>"
  cognitiveServiceEndpoint: "<cognitiveServiceEndpoint>"
  callbackUriHost: "<callbackUriHost>"
  acsGeneratedId: "<acsGeneratedId>"
```

**Note:** The application automatically loads configuration from the `application.yml` file on startup. All settings must be configured before starting the application.

---

## Running the Application

1. **Configure the Application:**

   - Configure your settings in `src/main/resources/application.yml` (see Configuration section above).
   - Ensure all required values are set before starting the application.

2. **Compile and Run the Application:**

   - Navigate to the directory containing `pom.xml`.
   - Compile the application:
     ```bash
     mvn compile
     ```
   - Build the package:
     ```bash
     mvn package
     ```
   - Execute the app:
     ```bash
     mvn spring-boot:run
     ```
   - Alternative execution method:
     ```bash
     mvn exec:java
     ```
   - Access Swagger UI at:
     - **Local**: http://localhost:8443/swagger-ui/index.html
     - **Dev Tunnel**: https://your-tunnel-url/swagger-ui/index.html

3. **Set up Event Grid Webhook:**

   - Create an Event Grid subscription for incoming calls.
   - Set up a Web hook (`https://<dev-tunnel-url>/api/lobbyCallEventHandler`) for callback.
   - Configure filter for your use case; key: `data.to.rawid`, operator: `string contains`, value: `8:acs`.
   - Deploy the event subscription.

---

## API Endpoints

The application provides the following REST endpoints:

| Endpoint                     | Method | Description                                  |
| ---------------------------- | ------ | -------------------------------------------- |
| `/api/lobbyCallEventHandler` | POST   | EventGrid webhook for incoming calls         |
| `/api/callbacks`             | POST   | Call Automation event callbacks              |
| `/targetCallToAcsUser`       | POST   | Create a target call to an ACS user          |
| `/getParticipants`           | GET    | List participants in the lobby call          |
| `/terminateCalls`            | GET    | Terminate all active calls                   |
| `/ws`                        | WS     | Real-time communication endpoint (WebSocket) |

## Event Grid Integration

- **Webhook endpoint:** `/api/lobbyCallEventHandler`
  - Handles Event Grid subscription validation
  - Automatically answers incoming calls and places them in the lobby
  - Processes call events and triggers appropriate actions
- **Callback endpoint:** `/api/callbacks`
  - Handles ACS call automation events (CallConnected, CreateCallFailed, etc.)
  - Provides comprehensive logging of all call events
  - Manages participant movement between lobby and target calls

## WebSocket Support

The application includes WebSocket support for real-time communication and call state updates:

- **Endpoint:** `/ws`
- **Purpose:** Real-time updates for client applications
- **Usage:** Client applications can connect to receive live updates about call states and participant movements
- **Integration:** Works with the JavaScript client application for lobby participant approval
- **Connection URL:** `ws://localhost:8443/ws` (local) or `wss://your-tunnel-url/ws` (dev tunnel)

## Usage Flow

1. **Configure the application** in `application.yml` with all required settings
2. **Start the application** and verify configuration is loaded successfully
3. **Set up EventGrid webhook** to point to `/api/lobbyCallEventHandler`
4. **Create a target call** using `/targetCallToAcsUser` with an ACS user ID
5. **Incoming calls** are automatically answered and placed in lobby
6. **Lobby participants** hear a waiting message via text-to-speech
7. **Use the WebSocket client** to approve and move participants to the target call

---

## Development Features

### Environment Variables

Configure dev tunnel support using environment variables or `application.yml`:

- `DEVTUNNEL_URL`: Your dev tunnel URL (optional)
- `DEVTUNNEL_ENABLED`: Set to `true` to enable dev tunnel features

### WebSocket Integration

The application includes WebSocket support for real-time updates. The WebSocket endpoint is available at `/ws` for client applications to connect and interact with the lobby call management system.

### Dynamic Server Configuration

The Swagger UI automatically detects and displays available servers (local and dev tunnel) based on your configuration.

---

## Technology Stack

- **Spring Boot 3.0.6** - Web framework
- **Azure Communication Services Call Automation SDK** - Call management
- **Azure EventGrid** - Event handling
- **WebSocket** - Real-time communication
- **SpringDoc OpenAPI** - API documentation
- **Maven** - Build tool
- **Java 17** - Runtime environment

---

## Troubleshooting

If you encounter issues while setting up or running the Call Automation sample, refer to the following troubleshooting tips:

### 1. Azure Communication Services Connection Issues

- **Error:** "Invalid connection string"  
  **Solution:** Double-check your `acsConnectionString`. Ensure there are no extra spaces or missing characters. Obtain the connection string directly from the Azure Portal.

- **Error:** "Resource not found"  
  **Solution:** Verify that your Azure Communication Services resource exists and is in the correct subscription and region.

### 2. Dev Tunnel or Callback Issues

- **Error:** "Callback URL not reachable" or events not triggering  
  **Solution:**
  - Ensure your Azure Dev Tunnel is running and the URL in `callbackUriHost` matches the tunnel's public URL.
  - Confirm your firewall or network settings allow inbound connections to your local machine.
  - Make sure the application is running and listening on the correct port (8443).

### 3. Cognitive Services Issues

- **Error:** "Audio not playing" or text-to-speech failures  
  **Solution:**
  - Confirm that the `cognitiveServiceEndpoint` is correctly configured in your application settings.
  - Verify that your Cognitive Services resource is active and accessible.
  - Check that your Azure subscription has the necessary permissions for the Cognitive Services resource.

### 4. Identity or Configuration Issues

- **Error:** "Invalid ACS identity"  
  **Solution:**

  - Ensure `acsGeneratedId` is a valid ACS user identity generated via the ACS SDK or portal.
  - Regenerate identities if needed and update `application.yml`, then restart the application.

- **Error:** "Configuration validation failed" or missing configuration  
  **Solution:**
  - Ensure all required properties are set in `application.yml` before starting the application.
  - Check application logs for specific configuration errors.
  - Verify the format of connection strings and endpoints.

### 5. General Debugging Tips

- Check application logs for detailed error messages.
- Ensure all configuration settings are correct.
- Restart your application and Dev Tunnel after making configuration changes.
- Review Azure Portal for resource status and quotas.
- Verify Event Grid webhook configuration and test event delivery.

**Still having trouble?**

- Review the official https://learn.microsoft.com/azure/communication-services/.
- Search for similar issues or ask questions on https://learn.microsoft.com/answers/topics/azure-communication-services.html.
- Contact your Azure administrator or support team if you suspect a permissions or resource issue.

---

## Notes

- Ensure your ACS resource and Cognitive Services are properly configured in Azure
- Use the dev tunnel URL with the full webhook path for `callbackUriHost` during local development
- The sample uses the ACS Java SDK and Spring Boot for REST endpoints
- All endpoints include proper HTTP status codes and error responses
- The application maintains state for call connections and participant tracking
- Configuration is loaded from `application.yml` at startup and validated automatically
- The application uses WebSocket to communicate with client applications for lobby participant approval

---

For more details, see the source code in `src/main/java/com/communication/callautomation/`.

## Additional Resources

- [Azure Communication Services Documentation](https://docs.microsoft.com/en-us/azure/communication-services/)
- [Call Automation SDK Reference](https://docs.microsoft.com/en-us/azure/communication-services/concepts/call-automation/)
- [Azure Dev Tunnels Documentation](https://learn.microsoft.com/en-us/azure/developer/dev-tunnels/)
- [Web Client Quickstart](https://github.com/Azure-Samples/communication-services-javascript-quickstarts/tree/users/v-kuppu/LobbyCallConfirmSample)
