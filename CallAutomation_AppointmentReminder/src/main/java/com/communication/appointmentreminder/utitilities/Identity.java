package com.communication.appointmentreminder.utitilities;

import com.azure.communication.common.CommunicationUserIdentifier;
import com.azure.communication.identity.CommunicationIdentityClient;
import com.azure.communication.identity.CommunicationIdentityClientBuilder;

public class Identity {
    /// <summary>
    /// Create new user
    /// </summary>
    public static String createUser(String connectionString) {
        CommunicationIdentityClient client = new CommunicationIdentityClientBuilder().connectionString(connectionString)
                .buildClient();
        CommunicationUserIdentifier user = client.createUser();
        return user.getId();
    }

    /// <summary>
    /// Delete the user
    /// </summary>
    public static void deleteUser(String connectionString, String source) {
        CommunicationIdentityClient client = new CommunicationIdentityClientBuilder().connectionString(connectionString)
                .buildClient();
        client.deleteUser(new CommunicationUserIdentifier(source));
    }
}
