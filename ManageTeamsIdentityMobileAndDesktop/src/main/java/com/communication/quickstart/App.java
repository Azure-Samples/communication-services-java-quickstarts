package com.communication.quickstart;

import com.azure.communication.identity.CommunicationIdentityClient;
import com.azure.communication.identity.CommunicationIdentityClientBuilder;
import com.azure.communication.identity.models.GetTokenForTeamsUserOptions;
import com.azure.core.credential.AccessToken;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.InteractiveRequestParameters;
import com.microsoft.aad.msal4j.PublicClientApplication;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class App {

    public static void main( String[] args ) throws Exception {
        System.out.println("Azure Communication Services - Communication access token Quickstart");
        // Quickstart code goes here

        // You need to provide your Azure AD client ID and tenant ID
        String appId = "<contoso_application_id>";
        String tenantId = "<contoso_tenant_id>";
        String authority = "https://login.microsoftonline.com/" + tenantId;

        // Create an instance of PublicClientApplication
        PublicClientApplication pca = PublicClientApplication.builder(appId)
                .authority(authority)
                .build();

        String redirectUri = "http://localhost";
        Set<String> scope = new HashSet<String>();
        scope.add("https://auth.msft.communication.azure.com/Teams.ManageCalls");
        scope.add("https://auth.msft.communication.azure.com/Teams.ManageChats");

        // Create an instance of InteractiveRequestParameters for acquiring the AAD token and object ID of a Teams user
        InteractiveRequestParameters parameters = InteractiveRequestParameters
                .builder(new URI(redirectUri))
                .scopes(scope)
                .build();

        // Retrieve the AAD token and object ID of a Teams user
        IAuthenticationResult result = pca.acquireToken(parameters).get();
        String teamsUserAadToken = result.accessToken();
        String[] accountIds = result.account().homeAccountId().split("\\.");
        String userObjectId = accountIds[0];
        System.out.println("Teams token: " + teamsUserAadToken);

        // You can find your connection string from your resource in the Azure portal
        String connectionString = "<connection_string>";

        // Instantiate the identity client
        CommunicationIdentityClient communicationIdentityClient = new CommunicationIdentityClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        // Exchange the Azure AD access token of the Teams User for a Communication Identity access token
        GetTokenForTeamsUserOptions options = new GetTokenForTeamsUserOptions(teamsUserAadToken, appId, userObjectId);
        AccessToken accessToken = communicationIdentityClient.getTokenForTeamsUser(options);
        System.out.println("Token: " + accessToken.getToken());
    }
}
