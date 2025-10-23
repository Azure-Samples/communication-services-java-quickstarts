
|page_type| languages |products|
|---|---|---|
|sample| <table><tr><td>Java</td></tr></table> |<table><tr><td>azure</td><td>azure-communication-services</td></tr></table>|

# Azure Communication Services Call Automation - Multiple Dial Out Sample

This sample demonstrates how to use the Azure Communication Services Call Automation SDK to manage multiple dial-out scenarios, including moving participants between calls and monitoring call status.

## Features

- **Set configuration** for ACS connection, phone numbers, identities, callback, and PMA endpoint with validation
- **Create calls** for user, ACS Test Identity 2, and ACS Test Identity 3
- **Move participants** between calls using the ACS SDK `MoveParticipantsOptions` API
- **Monitor call status** with a live HTML status page showing connection IDs, caller/callee info
- **Terminate all calls** with a single endpoint
- **Event Grid integration** for incoming call handling and call redirection scenarios
- **Automatic call redirection** based on workflow call types
- **Comprehensive logging** and error handling

## Setup and Host Your Azure DevTunnel

[Azure DevTunnels](https://learn.microsoft.com/en-us/azure/developer/dev-tunnels/overview) enables you to share local web services on the internet. Use the commands below to expose your local development environment for ACS event notifications:

```bash
devtunnel create --allow-anonymous
devtunnel port create -p 8080
devtunnel host
```

## Running the Application

1. Navigate to the directory containing `pom.xml`.
2. Compile the application:
    ```bash
    mvn compile
    ```
3. Build the package:
    ```bash
    mvn package
    ```
4. Execute the app:
    ```bash
    mvn exec:java
    ```
5. Access Swagger UI at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
6. Use the GET and POST endpoints to interact with the sample.

## Configuration

Use the `/api/setConfiguration` endpoint (via Swagger UI or REST client) to set the following **required** values:

- `acsConnectionString`: Azure Communication Service resource connection string
- `acsInboundPhoneNumber`: Inbound phone number (e.g., "+1425XXXAAAA")
- `acsOutboundPhoneNumber`: Outbound phone number (e.g., "+1425XXXAAAA")
- `userPhoneNumber`: User phone number (e.g., "+1425XXXAAAA")
- `acsTestIdentity2`: ACS Communication Identifier for participant 2
- `acsTestIdentity3`: ACS Communication Identifier for participant 3
- `callbackUriHost`: Base URL of the app (use your dev tunnel URL for local development)
- `pmaEndpoint`: PMA service endpoint

> **Note:** All configuration fields are validated and required. The endpoint will return an error if any field is missing or empty.

## Main Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/setConfiguration` | POST | Set ACS and app configuration |
| `/userCallToCallAutomation` | GET | Create a call from user phone to ACS inbound number |
| `/createCall2` | GET | Create a call for ACS Test Identity 2 |
| `/createCall3` | GET | Create a call for ACS Test Identity 3 |
| `/moveParticipant2` | GET | Move participant to ACS Test Identity 2 |
| `/moveParticipant3` | GET | Move participant to ACS Test Identity 3 |
| `/getStatus` | GET | View current call status (HTML table) |
| `/terminateCalls` | GET | Terminate all active calls |

## Event Grid Integration

- **Webhook endpoint:** `/api/moveParticipantEvent`
  - Handles Event Grid subscription validation
  - **Scenario 1:** User incoming calls - automatically answers calls from the configured user phone number
  - **Scenario 2:** Workflow call redirection - redirects calls based on `lastWorkflowCallType` to appropriate ACS identities
- **Callback endpoint:** `/api/callbacks`
  - Handles ACS call automation events (CallConnected, CreateCallFailed, etc.)
  - Provides comprehensive logging of all call events

## Call Status Monitoring

Visit `/getStatus` in your browser to see a live HTML table showing:
- All call connection IDs
- Caller and callee information for each call
- Status indicators (Active/Inactive)
- Styled table with alternating row colors for easy reading

## Move Participants

The `/moveParticipant2` and `/moveParticipant3` endpoints demonstrate the **Move Participants** functionality:
- Uses the ACS SDK `MoveParticipantsOptions` API
- Supports both phone numbers (format: `+1234567890`) and ACS user identities (format: `8:acs:...`)
- Moves participants from source calls to the main user call (Call 1)
- Automatically updates caller/callee tracking and resets source call connections

## Workflow Overview

1. **Configure** the application with required ACS settings
2. **Create the main call** from user phone to ACS inbound number
3. **Create additional calls** for ACS Test Identities 2 and 3
4. **Move participants** from the additional calls to the main call
5. **Monitor status** to see all active connections and participant info
6. **Terminate** all calls when done

## Error Handling

The application includes comprehensive error handling:
- Configuration validation with detailed error messages
- Call creation and participant move error handling
- Graceful termination of calls with warning logs for failed hangups
- Event processing error handling with detailed logging

## Notes

- Ensure your ACS resource and phone numbers are properly configured in Azure
- Use the dev tunnel URL for `callbackUriHost` during local development
- The sample uses the ACS Java SDK and Spring Boot for REST endpoints
- All endpoints include proper HTTP status codes and error responses
- The application maintains state for call connections and participant tracking

---
For more details, see the source code in `src/main/java/com/communication/callautomation/ProgramSample.java`.
