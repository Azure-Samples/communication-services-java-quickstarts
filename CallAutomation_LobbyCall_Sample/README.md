|page_type| languages                             |products
|---|---------------------------------------|---|
|sample| <table><tr><td>Java</td></tr></table> |<table><tr><td>azure</td><td>azure-communication-services</td></tr></table>|

# Call Automation - Lobby Call Sample

This sample demonstrates how to utilize the Call Automation SDK to implement a Lobby Call scenario. Users join a lobby call and remain on hold until an user in the target call confirms their participation. Once approved, Call Automation (bot) automatically connects the lobby users to the designated target call.
The sample uses a client application (java script sample) available in [Web Client Quickstart](https://github.com/Azure-Samples/communication-services-javascript-quickstarts/tree/users/v-kuppu/LobbyCallConfirmSample).

## Features

- **Lobby Call Management**: Automatically answer incoming calls and place them in a lobby
- **Text-to-Speech**: Play waiting messages to lobby participants using Azure Cognitive Services
- **Participant Management**: Move participants between lobby and target calls
- **WebSocket Support**: Real-time communication for call state updates
- **Event-Driven Architecture**: Handle Call Automation events via webhooks
- **Dev Tunnel Integration**: Easy development with Azure Dev Tunnels for webhook delivery

# Design

![Lobby Call Support](./resources/Lobby_Call_Support_Scenario.jpg)


## Prerequisites

- Java 17 or later
- Maven 3.6 or later
- Azure Communication Services resource
- Azure Cognitive Services resource (for text-to-speech)
- Azure Dev Tunnels CLI (for local development)

## Setup and Configuration

### 1. Setup Azure Dev Tunnel

[Azure DevTunnels](https://learn.microsoft.com/en-us/azure/developer/dev-tunnels/overview) enables you to expose your local development server to the internet for webhook delivery.

```bash
# Create a new dev tunnel
devtunnel create --allow-anonymous

# Create a port mapping for the application
devtunnel port create -p 8443

# Start the tunnel
devtunnel host
```

Copy the HTTPS URL provided by dev tunnel (e.g., `https://abc123-8443.inc1.devtunnels.ms`) for use in configuration.

### 2. Build and Run the Application

Navigate to the project directory and run:

```bash
# Compile the application
mvn compile

# Build the package
mvn package

# Run the application
mvn spring-boot:run
```

Alternative execution method:
```bash
mvn exec:java
```

The application will start on port 8443 and be accessible at:
- **Local**: http://localhost:8443/swagger-ui/index.html
- **Dev Tunnel**: https://your-tunnel-url/swagger-ui/index.html

### 3. Configure Application Settings

Use the Swagger UI to configure the application via the `/api/setConfiguration` endpoint:

**Required Configuration Parameters:**

1. **`acsConnectionString`**: Your Azure Communication Services connection string
   - Format: `endpoint=https://your-acs-resource.communication.azure.com/;accesskey=your-access-key`

2. **`cognitiveServiceEndpoint`**: Azure Cognitive Services endpoint for text-to-speech
   - Format: `https://your-cognitive-service.cognitiveservices.azure.com/`

3. **`callbackUriHost`**: Your application's base URL for webhook callbacks
   - Local: `http://localhost:8443`
   - Dev Tunnel: `https://your-tunnel-url` (without trailing slash)

4. **`pmaEndpoint`**: Azure Communication Services endpoint
   - Format: `https://your-acs-resource.communication.azure.com`

5. **`acsGeneratedId`**: Communication user ID for receiving lobby calls
   - Generate using ACS Identity SDK or Azure portal

6. **`webSocketToken`**: Unique token for WebSocket endpoint security
   - Use any unique string (e.g., UUID or custom token)

## API Endpoints

The application provides the following REST endpoints:

### Core Endpoints
- **`POST /api/setConfiguration`** - Configure application settings
- **`POST /api/lobbyCallEventHandler`** - EventGrid webhook for incoming calls
- **`POST /api/callbacks`** - Call Automation event callbacks
- **`POST /targetCallToAcsUser`** - Create a target call to an ACS user
- **`GET /getParticipants`** - List participants in the lobby call
- **`GET /terminateCalls`** - Terminate all active calls

### WebSocket Endpoint
- **`/ws/{webSocketToken}`** - Real-time communication endpoint

## Usage Flow

1. **Configure the application** using `/api/setConfiguration`
2. **Set up EventGrid webhook** to point to `/api/lobbyCallEventHandler`
3. **Create a target call** using `/targetCallToAcsUser` with an ACS user ID
4. **Incoming calls** are automatically answered and placed in lobby
5. **Lobby participants** hear a waiting message via text-to-speech
6. **Participants are automatically moved** to the target call after the message completes

## Development Features

### Environment Variables
Configure dev tunnel support using environment variables:
- `DEVTUNNEL_URL`: Your dev tunnel URL
- `DEVTUNNEL_ENABLED`: Set to `true` to enable dev tunnel features

### WebSocket Integration
The application includes WebSocket support for real-time updates. WebSocket endpoints are dynamically configured based on the `webSocketToken` parameter.

### Dynamic Server Configuration
The Swagger UI automatically detects and displays available servers (local and dev tunnel) based on your configuration.

## Technology Stack

- **Spring Boot 3.0.6** - Web framework
- **Azure Communication Services Call Automation SDK** - Call management
- **Azure EventGrid** - Event handling
- **WebSocket** - Real-time communication
- **SpringDoc OpenAPI** - API documentation
- **Maven** - Build tool
- **Java 17** - Runtime environment

## Troubleshooting

### Common Issues

1. **Dev Tunnel 502 Errors**: Ensure your application is running on port 8443 before starting the tunnel
2. **Webhook Delivery Failures**: Verify your `callbackUriHost` matches your dev tunnel URL exactly
3. **Audio Issues**: Confirm your Cognitive Services endpoint is correctly configured
4. **Connection Issues**: Check that your ACS connection string is valid and has the required permissions

### Logs and Monitoring
The application provides detailed logging for all call events and operations. Check the console output for debugging information.

## Additional Resources

- [Azure Communication Services Documentation](https://docs.microsoft.com/en-us/azure/communication-services/)
- [Call Automation SDK Reference](https://docs.microsoft.com/en-us/azure/communication-services/concepts/call-automation/)
- [Azure Dev Tunnels Documentation](https://learn.microsoft.com/en-us/azure/developer/dev-tunnels/)
