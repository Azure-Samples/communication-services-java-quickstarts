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
        

        // Issue an access token with validity of 24 hours and the "voip" scope for a user identity
        List<CommunicationTokenScope> scopes = new ArrayList<>(Arrays.asList(CommunicationTokenScope.VOIP));
        AccessToken accessToken = communicationIdentityClient.getToken(user, scopes);
        OffsetDateTime expiresAt = accessToken.getExpiresAt();
        String token = accessToken.getToken();
        System.out.println("\nIssued an access token with 'voip' scope that expires at " + expiresAt + ": " + token);

        // Issue an access token with validity of an hour and the "voip" scope for a user identity
        Duration tokenExpiresIn = Duration.ofHours(1);
        accessToken = communicationIdentityClient.getToken(user, scopes, tokenExpiresIn);
        expiresAt = accessToken.getExpiresAt();
        token = accessToken.getToken();
        System.out.println("\nIssued an access token with 'voip' scope and custom expiration time that expires at " + expiresAt + ": " + token);

        //Create an identity and issue token with validity of 24 hours in one call
        List<CommunicationTokenScope> scope = Arrays.asList(CommunicationTokenScope.CHAT);
        CommunicationUserIdentifierAndToken result = communicationIdentityClient.createUserAndToken(scope);
        CommunicationUserIdentifier userIdentifier = result.getUser();
        System.out.println("\nCreated a user identity with ID: " + userIdentifier.getId());
        AccessToken userTokenResult = result.getUserToken();
        OffsetDateTime expiresTime = userTokenResult.getExpiresAt();
        String userToken = userTokenResult.getToken();
        System.out.println("\nIssued an access token with 'chat' scope that expires at " + expiresTime + ": " + userToken);

        //Create an identity and issue token with validity of an hour in one call
        result = communicationIdentityClient.createUserAndToken(scope, tokenExpiresIn);
        userIdentifier = result.getUser();
        System.out.println("\nCreated a user identity with ID: " + userIdentifier.getId());
        userTokenResult = result.getUserToken();
        expiresTime = userTokenResult.getExpiresAt();
        userToken = userTokenResult.getToken();
        System.out.println("\nIssued an access token with 'chat' scope that expires at " + expiresTime + ": " + userToken);

        // Refresh access tokens - existingIdentity represents identity of Azure Communication Services stored during identity creation
        CommunicationUserIdentifier identity = new CommunicationUserIdentifier(userIdentifier.getId());
        AccessToken response = communicationIdentityClient.getToken(identity, scope);

        // Revoke access tokens
        communicationIdentityClient.revokeTokens(userIdentifier);
        System.out.println("\nSuccessfully revoked all access tokens for user identity with ID: " + userIdentifier.getId());

        // Delete an identity
        communicationIdentityClient.deleteUser(userIdentifier);
        System.out.println("\nDeleted the user identity with ID: " + userIdentifier.getId());

    }
}