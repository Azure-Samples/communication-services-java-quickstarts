package com.communication.outboundcallreminder.Ngrok;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.client.HttpClients;
import com.communication.outboundcallreminder.Logger;

public class NgrokConnector {

    /// The HTTP client.
    private CloseableHttpClient httpClient = null;
    private HttpGet request = null;
    private final String ngrokTunnelUrl = "http://127.0.0.1:4040/api/tunnels";

    NgrokConnector() {
        httpClient = HttpClients.createDefault();
        request = new HttpGet(ngrokTunnelUrl);
        // add request headers
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    }

    public JSONArray GetAllTunnelsAsync() {
        JSONArray tunnelList = null;
        try {
            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                String tunnel = EntityUtils.toString(entity);
                JSONObject tunnelObject = (JSONObject) (new JSONParser().parse(tunnel));
                tunnelList = (JSONArray) tunnelObject.get("tunnels");
            }
        } catch (Exception ex) {
            Logger.LogMessage(Logger.MessageType.ERROR, "Failed to get Ngrok URL -- > " + ex.getMessage());
        }
        return tunnelList;
    }
}