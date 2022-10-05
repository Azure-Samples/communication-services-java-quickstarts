package com.communication.CallPlayAudio.Ngrok;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.client.HttpClients;
import com.communication.CallPlayAudio.Logger;

public class NgrokConnector {

    /// The HTTP client.
    private final CloseableHttpClient httpClient;
    private final HttpGet request;

    NgrokConnector() {
        httpClient = HttpClients.createDefault();
        String ngrokTunnelUrl = "http://127.0.0.1:4040/api/tunnels";
        request = new HttpGet(ngrokTunnelUrl);
        // add request headers
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    }

    public JSONArray getAllTunnelsAsync() {
        JSONArray tunnelList = null;
        try {
            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                String tunnel = EntityUtils.toString(entity);
                JSONObject tunnelObject = (JSONObject) (new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(tunnel));
                tunnelList = (JSONArray) tunnelObject.get("tunnels");
            }
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR, "Failed to get Ngrok URL -- > " + ex.getMessage());
        }
        return tunnelList;
    }
}