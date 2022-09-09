---
page_type: sample
languages:
- Java
products:
- azure
- azure-communication-services
---


# Create and manage Communication access tokens for Teams users in mobile and desktop applications

This code sample walks you through the process of acquiring a Communication Token Credential by exchanging an Azure AD token of a user with a Teams license for a valid Communication access token.

This sample application utilizes the [MSAL Java](https://github.com/AzureAD/microsoft-authentication-library-for-java) library for authentication against the Azure AD and acquisition of a token with delegated permissions. The token exchange itself is facilitated by the `azure-communication-identity` package.

To be able to use the token for Calling, use it to initialize the `CommunicationTokenCredential` from the `azure-communication-common` library.

## Prerequisites

- An Azure account with an active subscription. [Create an account for free](https://azure.microsoft.com/free/?WT.mc_id=A261C142F).
- Java Development Kit (JDK) version 8 or above.
- An active Communication Services resource and connection string. [Create a Communication Services resource](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource/).
- Azure Active Directory tenant with users that have a Teams license.

## Before running sample code

1. Complete the [Administrator actions](https://docs.microsoft.com/azure/communication-services/quickstarts/manage-teams-identity?pivots=programming-language-javascript#administrator-actions) from the [Manage access tokens for Teams users quickstart](https://docs.microsoft.com/azure/communication-services/quickstarts/manage-teams-identity).
    - Take a note of Fabrikam's Azure AD Tenant ID and Contoso's Azure AD App Client ID. You'll need the values in the following steps.
1. On the Authentication pane of your Azure AD App, add a new platform of the mobile and desktop application type with the Redirect URI of `http://localhost`.
1. Open an instance of Windows Terminal, PowerShell, or an equivalent command line and navigate to the directory that you'd like to clone the sample to.
1. `git clone https://github.com/Azure-Samples/communication-services-java-quickstarts.git`
1. Navigate to the `ManageTeamsIdentityMobileAndDesktop` directory.
1. With the Communication Services procured in pre-requisites, add connection string, an Azure AD client ID and tenant ID in the App.java file.

## Run the code

From a console prompt, navigate to the directory containing the `pom.xml` file, then execute the following node commands to run the app:

1. Compile the project by using command `mvn compile`
2. Then, build the package using command `mvn package`
3. Run the command to execute the app `mvn exec:java -Dexec.mainClass="com.communication.quickstart.App" -Dexec.cleanupDaemonThreads=false`. If you are using Powershell CLI try to execute as follow `mvn exec:java -D exec.mainClass="com.communication.quickstart.App" -D exec.cleanupDaemonThreads=false`.

You should be presented with a browser window and navigated to the Azure AD login form. If the authentication is successful, the application receives an Azure AD access token and exchanges it for a Communication access token.