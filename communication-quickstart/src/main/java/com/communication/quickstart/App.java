package com.communication.quickstart;

import com.azure.communication.common.*;
import com.azure.communication.identity.*;
import com.azure.communication.identity.models.*;
import com.azure.core.credential.*;

import java.io.IOException;
import java.time.*;
import java.util.*;

public class App
{
    public static void main( String[] args ) throws IOException
    {
        System.out.println("Azure Communication Services - Access Tokens Quickstart");
       
        // Your can find your connection string from your resource in the Azure portal
        String connectionString = "<connection_string>";

        CommunicationIdentityClient communicationIdentityClient = new CommunicationIdentityClientBuilder()
            .connectionString(connectionString)
            .buildClient();

         // Create an identity
        CommunicationUserIdentifier user = communicationIdentityClient.createUser();
        System.out.println("\nCreated an identity with ID: " + user.getId());
        

        // Issue an access token with the "voip" scope for a user identity
        List<CommunicationTokenScope> scopes = new ArrayList<>(Arrays.asList(CommunicationTokenScope.VOIP));
        AccessToken accessToken = communicationIdentityClient.getToken(user, scopes);
        OffsetDateTime expiresAt = accessToken.getExpiresAt();
        String token = accessToken.getToken();
        System.out.println("\nIssued an access token with 'voip' scope that expires at: " + expiresAt + ": " + token);
               
        //Create an identity and issue token in one call
        List<CommunicationTokenScope> scopes = Arrays.asList(CommunicationTokenScope.CHAT);
        CommunicationUserIdentifierAndToken result = communicationIdentityClient.createUserAndToken(scopes);
        CommunicationUserIdentifier user = result.getUser();
        System.out.println("\nCreated a user identity with ID: " + user.getId());
        AccessToken accessToken = result.getUserToken();
        OffsetDateTime expiresAt = accessToken.getExpiresAt();
        String token = accessToken.getToken();
        System.out.println("\nIssued an access token with 'chat' scope that expires at: " + expiresAt + ": " + token);

        // Refresh access tokens - existingIdentity represents identity of Azure Communication Services stored during identity creation
        CommunicationUserIdentifier identity = new CommunicationUserIdentifier(user.getId());
        AccessToken response = communicationIdentityClient.getToken(identity, scopes);

        // Revoke access tokens
        communicationIdentityClient.revokeTokens(user);
        System.out.println("\nSuccessfully revoked all access tokens for user identity with ID: " + user.getId());

        // Delete an identity
        communicationIdentityClient.deleteUser(user);
        System.out.println("\nDeleted the user identity with ID: " + user.getId());

    }
}