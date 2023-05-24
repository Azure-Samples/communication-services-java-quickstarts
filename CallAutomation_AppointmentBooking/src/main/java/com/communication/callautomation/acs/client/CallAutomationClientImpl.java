package com.communication.callautomation.acs.client;

import com.azure.communication.callautomation.CallAutomationAsyncClient;
import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;
import com.azure.core.http.HttpClient;
import com.communication.callautomation.config.AcsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class CallAutomationClientImpl implements CallAutomationClientFactory {
    private final AcsConfig acsConfig;
    private final HttpClient azureHttpClient;
    private final Map<String, CallAutomationClient> clientMap = new ConcurrentHashMap<String, CallAutomationClient>();

    @Autowired
    public CallAutomationClientImpl(final AcsConfig acsConfig,
                                    final HttpClient azureHttpClient) {
        this.acsConfig = acsConfig;
        this.azureHttpClient = azureHttpClient;
    }
    @Override
    public CallAutomationClient getCallAutomationClient() {
        log.debug("Start: getCallAutomationClient");
        String connectionString = acsConfig.getConnectionString();
        CallAutomationClient callAutomationClient;
        callAutomationClient = clientMap.get(connectionString);
        if (callAutomationClient == null) {
            callAutomationClient = createCallAutomationClient(connectionString);
        }
        log.debug("End: getCallAutomationClient");
        return callAutomationClient;
    }

    private synchronized CallAutomationClient createCallAutomationClient(final String connectionString) {
        log.debug("Start: createCallAutomationClient");
        CallAutomationClientBuilder callAutomationClientBuilder = new CallAutomationClientBuilder()
                .httpClient(azureHttpClient)
                .connectionString(connectionString);
        CallAutomationClient callAutomationClient = callAutomationClientBuilder.buildClient();
        clientMap.put(connectionString, callAutomationClient);
        log.debug("End: createCallAutomationClient");
        return callAutomationClient;
    }
}
