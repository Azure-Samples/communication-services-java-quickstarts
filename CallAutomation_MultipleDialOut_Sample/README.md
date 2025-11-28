| page_type | languages                             | products                                                                    |
| --------- | ------------------------------------- | --------------------------------------------------------------------------- |
| sample    | <table><tr><td>Java</td></tr></table> | <table><tr><td>azure</td><td>azure-communication-services</td></tr></table> |

# Call Automation - Move Participants Sample

This sample demonstrates how to use the Call Automation SDK to implement a Move Participants Call scenario with Azure Communication Services.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Design](#design)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Main Endpoints](#main-endpoints)
- [Event Grid Integration](#event-grid-integration)
- [Call Status Monitoring](#call-status-monitoring)
- [Move Participants](#move-participants)
- [Workflow Overview](#workflow-overview)
- [Error Handling](#error-handling)
- [Troubleshooting](#troubleshooting)

---

## Overview

This project provides a sample implementation for moving participants between calls using Azure Communication Services and the Call Automation SDK with Java and Spring Boot.

## Features

- **Create calls** for user, ACS Test Identity 2, and ACS Test Identity 3
- **Move participants** between calls using the ACS SDK `MoveParticipantsOptions` API
- **Monitor call status** with a live HTML status page showing connection IDs, caller/callee info
- **Terminate all calls** with a single endpoint
- **Event Grid integration** for incoming call handling and call redirection scenarios
- **Automatic call redirection** based on workflow call types
- **Comprehensive logging** and error handling

---

## Design

![Move Participant](./resources/Move_Participant_Sample.jpg)

---

## Prerequisites

- **Azure Account:** An Azure account with an active subscription.  
  https://azure.microsoft.com/free/?WT.mc_id=A261C142F.
- **Communication Services Resource:** A deployed Communication Services resource.  
  https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource.
- **Phone Number:** A https://learn.microsoft.com/en-us/azure/communication-services/quickstarts/telephony/get-phone-number in your Azure Communication Services resource that can make outbound calls.
- **Azure Dev Tunnel:** https://learn.microsoft.com/en-us/azure/developer/dev-tunnels/get-started.

- [Java](https://www.oracle.com/java/technologies/javase-downloads.html) 8 or above.
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

```bash
devtunnel create --allow-anonymous
devtunnel port create -p 8080
devtunnel host
```

---

## Configuration

Before running the application, configure your ACS settings in `src/main/resources/application.yml`:

| Setting                  | Description                                                                                                                                          | Example Value                                                            |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| `acsConnectionString`    | The connection string for your Azure Communication Services resource. Find this in the Azure Portal under your resource Keys section.                | `"endpoint=https://<RESOURCE>.communication.azure.com/;accesskey=<KEY>"` |
| `callbackUriHost`        | The base URL where your app will listen for incoming events from Azure Communication Services. For local development, use your Azure Dev Tunnel URL. | `"https://<your-dev-tunnel>.devtunnels.ms"`                              |
| `acsOutboundPhoneNumber` | The Azure Communication Services phone number used to make outbound calls. Must be purchased and configured in your ACS resource.                    | `"+1XXXXXXXXXX"`                                                         |
| `acsInboundPhoneNumber`  | The Azure Communication Services phone number used to receive inbound calls. Must also be configured in your ACS resource.                           | `"+1XXXXXXXXXX"`                                                         |
| `userPhoneNumber`        | The phone number of the external user to initiate the first call. Any valid phone number for testing.                                                | `"+1XXXXXXXXXX"`                                                         |
| `acsTestIdentity2`       | An Azure Communication Services user identity, generated using the ACS web client or SDK, used for testing participant movement.                     | `"8:acs:<GUID>"`                                                         |
| `acsTestIdentity3`       | Another ACS user identity, generated similarly, for additional test scenarios.                                                                       | `"8:acs:<GUID>"`                                                         |

### How to Obtain These Values

- **acsConnectionString:**

  1. Go to the Azure Portal.
  2. Navigate to your Communication Services resource.
  3. Select "Keys & Connection String."
  4. Copy the "Connection String" value.

- **callbackUriHost:**

  1. Set up an Azure Dev Tunnel as described in the prerequisites.
  2. Use the public URL provided by the Dev Tunnel as your callback URI host.

- **acsOutboundPhoneNumber / acsInboundPhoneNumber:**

  1. In your Communication Services resource, go to "Phone numbers."
  2. Purchase or use an existing phone number.
  3. Assign the number as needed for outbound/inbound use.

- **userPhoneNumber:**  
  Use any valid phone number you have access to for testing outbound calls.

- **acsTestIdentity2 / acsTestIdentity3:**
  1. Use the ACS web client or SDK to generate user identities.
  2. Store the generated identity strings here.

#### Example `application.yml`

```yaml
acs:
  acsConnectionString: "<acsConnectionString>"
  acsInboundPhoneNumber: "<acsInboundPhoneNumber>"
  acsOutboundPhoneNumber: "<acsOutboundPhoneNumber>"
  userPhoneNumber: "<userPhoneNumber>"
  acsTestIdentity2: "<acsTestIdentity2>"
  acsTestIdentity3: "<acsTestIdentity3>"
  callbackUriHost: "<callbackUriHost>"
```

**Note:** The application automatically loads configuration from the `application.yml` file on startup. No manual configuration endpoint is needed.

---

## Running the Application

1. **Create an azure event grid subscription for incoming calls:**

   - Set up a Web hook(`https://<dev-tunnel-url>/api/moveParticipantEvent`) for callback.
   - Add Filters:
     - Key: `data.From.PhoneNumber.Value`, operator: `string contains`, value: `userPhoneNumber, Inbound Number (ACS)`
     - Key: `data.to.rawid`, operator: `string does not begin`, value: `8`
   - Deploy the event subscription.

2. **Compile and Run the Application:**

   - Navigate to the directory containing `pom.xml`.
   - Configure your settings in `src/main/resources/application.yml` (see Configuration section above).
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
     mvn exec:java
     ```
   - Access Swagger UI at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

3. **Workflow Execution**

> **Note:**  
> The phone numbers used here are taken from the Azure Communication Services resource.  
> The phone numbers are released and become available when the call is answered.
>
> **Call 2 and Call 3 must be answered after redirecting and before moving participants.**

##### Call 1

1. `userPhoneNumber` calls `acsInboundPhoneNumber`.
2. When the call is created, note the Call Connection Id as **Target Call Connection Id**.
3. Call Automation answers the call and assigns a bot as the receiver.
4. `acsInboundPhoneNumber` is released from the call after it is answered and assigned to the bot.

##### Call 2

1. `acsInboundPhoneNumber` makes a call to `acsOutboundPhoneNumber`.
2. When the call is created, note the Call Connection Id as **Source Call Connection Id**.
3. Call Automation answers the call, redirects to `acsTestIdentity2`, and releases `acsOutboundPhoneNumber` from the call.
4. The call connection id generated while redirection is an internal connection id; **do not use this connection id for the Move operation**.

##### Move Participant Operation

- **Inputs:**
  - Source Connection Id (from Call 2): the connection to move the participant from.
  - Target Connection Id (from Call 1): the connection to move the participant to.
  - Participant (initial participant before call is redirected) from Source call (Call 2): `acsOutboundPhoneNumber`
- Participants list after `MoveParticipantSucceeded` event: 3

##### Call 3

1. `acsInboundPhoneNumber` makes a call to `acsOutboundPhoneNumber`.
2. When the call is created, note the Call Connection Id as **Source Call Connection Id**.
3. Call Automation answers the call, redirects to `acsTestIdentity3`, and releases `acsOutboundPhoneNumber` from the call.
4. The call connection id generated while redirection is an internal connection id; **do not use this connection id for the Move operation**.

##### Move Participant Operation

- Inputs:
  - Source Connection Id (from Call 3): the connection to move the participant from.
  - Target Connection Id (from Call 1): the connection to move the participant to.
  - Participant (initial participant before call is redirected) from Source call (Call 3): `acsOutboundPhoneNumber`
- Participants list after `MoveParticipantSucceeded` event: 4

---

## Main Endpoints

| Endpoint                    | Method | Description                                         |
| --------------------------- | ------ | --------------------------------------------------- |
| `/userCallToCallAutomation` | GET    | Create a call from user phone to ACS inbound number |
| `/createCall2`              | GET    | Create a call for ACS Test Identity 2               |
| `/createCall3`              | GET    | Create a call for ACS Test Identity 3               |
| `/moveParticipant2`         | GET    | Move participant to ACS Test Identity 2             |
| `/moveParticipant3`         | GET    | Move participant to ACS Test Identity 3             |
| `/getStatus`                | GET    | View current call status (HTML table)               |
| `/terminateCalls`           | GET    | Terminate all active calls                          |

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

## API Testing with Swagger

You can explore and test the available API endpoints using the built-in Swagger UI:

- **Swagger URL:**  
  [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

> If running in a dev tunnel or cloud environment, replace `localhost:8080` with your tunnel's public URL (e.g., `https://<your-dev-tunnel>.devtunnels.ms/swagger-ui.html`).

---

## Workflow Overview

1. **Setup Event Grid subscription** in Azure Portal pointing to your dev tunnel URL.
2. **Configure** the application settings in `application.yml` with your ACS details.
3. **Start the application** - configuration is loaded automatically.
4. **Create the main call** from user phone to ACS inbound number.
5. **Create additional calls** for ACS Test Identities 2 and 3.
6. **Move participants** from the additional calls to the main call.
7. **Monitor status** to see all active connections and participant info.
8. **Terminate** all calls when done.

## Error Handling

The application includes comprehensive error handling:

- Configuration validation with detailed error messages
- Call creation and participant move error handling
- Graceful termination of calls with warning logs for failed hangups
- Event processing error handling with detailed logging

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
  - Make sure the application is running and listening on the correct port.

### 3. Phone Number Problems

- **Error:** "Phone number not provisioned" or "Invalid phone number"  
  **Solution:**
  - Confirm that the phone numbers in `acsOutboundPhoneNumber` and `acsInboundPhoneNumber` are purchased and assigned in your Azure Communication Services resource.
  - Use format (e.g., `+1XXXXXXXXXX`).

### 4. Identity or Participant Issues

- **Error:** "Invalid ACS identity"  
  **Solution:**
  - Ensure `acsTestIdentity2` and `acsTestIdentity3` are valid ACS user identities generated via the ACS SDK or portal.
  - Regenerate identities if needed and update `application.yml`.

### 5. General Debugging Tips

- Check application logs for detailed error messages.
- Ensure all configuration settings are correct.
- Restart your application and Dev Tunnel after making configuration changes.
- Review Azure Portal for resource status and quotas.

**Still having trouble?**

- Review the official https://learn.microsoft.com/azure/communication-services/.
- Search for similar issues or ask questions on https://learn.microsoft.com/answers/topics/azure-communication-services.html.
- Contact your Azure administrator or support team if you suspect a permissions or resource issue.

## Notes

- Ensure your ACS resource and phone numbers are properly configured in Azure
- Use the dev tunnel URL for `callbackUriHost` during local development
- The sample uses the ACS Java SDK and Spring Boot for REST endpoints
- All endpoints include proper HTTP status codes and error responses
- The application maintains state for call connections and participant tracking

---

For more details, see the source code in `src/main/java/com/communication/callautomation/ProgramSample.java`.
