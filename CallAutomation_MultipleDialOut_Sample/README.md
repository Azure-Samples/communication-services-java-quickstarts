| page_type | languages                             | products                                                                    |
| --------- | ------------------------------------- | --------------------------------------------------------------------------- |
| sample    | <table><tr><td>Java</td></tr></table> | <table><tr><td>azure</td><td>azure-communication-services</td></tr></table> |

# Azure Communication Services Call Automation - Multiple Dial Out Sample

This sample demonstrates how to utilize the Call Automation SDK to implement a Move Participants Call scenario.

## Features

- **Create calls** for user, ACS Test Identity 2, and ACS Test Identity 3
- **Move participants** between calls using the ACS SDK `MoveParticipantsOptions` API
- **Monitor call status** with a live HTML status page showing connection IDs, caller/callee info
- **Terminate all calls** with a single endpoint
- **Event Grid integration** for incoming call handling and call redirection scenarios
- **Automatic call redirection** based on workflow call types
- **Comprehensive logging** and error handling

# Design

![Move Participant](./resources/Move_Participant_Sample.jpg)

## Setup and Host Your Azure DevTunnel

```bash
devtunnel create --allow-anonymous
devtunnel port create -p 8080
devtunnel host
```

## Configuration

Before running the application, configure your ACS settings in `src/main/resources/application.yml`:

```yaml
acs:
  acsConnectionString: "endpoint=https://your-acs-resource.communication.azure.com/;accesskey=your-access-key"
  acsInboundPhoneNumber: "+1234567890"
  acsOutboundPhoneNumber: "+0987654321"
  userPhoneNumber: "+1122334455"
  acsTestIdentity2: "8:acs:your-acs-resource-id:user-identity-2"
  acsTestIdentity3: "8:acs:your-acs-resource-id:user-identity-3"
  callbackUriHost: "https://your-devtunnel-url.devtunnels.ms"
```

**Note:** The application now automatically loads configuration from the `application.yml` file on startup. The previous `/api/setConfiguration` endpoint has been removed.

## Running the Application

1. Navigate to the directory containing `pom.xml`.
2. **Configure your settings** in `src/main/resources/application.yml` (see Configuration section above).
3. Compile the application:
   ```bash
   mvn compile
   ```
4. Build the package:
   ```bash
   mvn package
   ```
5. Execute the app:
   ```bash
   mvn exec:java
   ```
6. Access Swagger UI at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
7. Use the `/api/getConfiguration` endpoint to verify your configuration is loaded correctly.
8. Use the other GET endpoints to interact with the call automation features.

## Main Endpoints

| Endpoint                    | Method | Description                                            |
| --------------------------- | ------ | ------------------------------------------------------ |
| `/api/getConfiguration`     | GET    | View current configuration loaded from application.yml |
| `/userCallToCallAutomation` | GET    | Create a call from user phone to ACS inbound number    |
| `/createCall2`              | GET    | Create a call for ACS Test Identity 2                  |
| `/createCall3`              | GET    | Create a call for ACS Test Identity 3                  |
| `/moveParticipant2`         | GET    | Move participant to ACS Test Identity 2                |
| `/moveParticipant3`         | GET    | Move participant to ACS Test Identity 3                |
| `/getStatus`                | GET    | View current call status (HTML table)                  |
| `/terminateCalls`           | GET    | Terminate all active calls                             |

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

1. **Setup Event Grid subscription** in Azure Portal pointing to your dev tunnel URL.
2. **Configure** the application with required ACS settings.
3. **Create the main call** from user phone to ACS inbound number.
4. **Create additional calls** for ACS Test Identities 2 and 3.
5. **Move participants** from the additional calls to the main call.
6. **Monitor status** to see all active connections and participant info.
7. **Terminate** all calls when done.

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
