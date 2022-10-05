package com.communication.CallPlayAudio.Ngrok;

import net.minidev.json.JSONArray;
import com.communication.CallPlayAudio.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

public class NgrokService {
    /// The NGROK process
    private Process ngrokProcess;
    // NgrokConnector connector;
    private final NgrokConnector connector;

    public NgrokService(String ngrokPath, String authToken) {
        connector = new NgrokConnector();
        this.ensureNgrokNotRunning();
        this.createNgrokProcess(ngrokPath, authToken);
    }

    /// <summary>
    /// Ensures that NGROK is not running.
    /// </summary>
    private void ensureNgrokNotRunning() {
        BufferedReader input;
        String ngrokProcess = "ngrok.exe";
        String processFilter = "/nh /fi \"Imagename eq " + ngrokProcess + "\"";
        String tasksCmd = System.getenv("windir") + "/system32/tasklist.exe " + processFilter;
        try {
            Process process = Runtime.getRuntime().exec(tasksCmd);
            input = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = input.readLine()) != null) {
                // It means ngrok.exe running
                if (line.contains(ngrokProcess)) {
                    Logger.logMessage(Logger.MessageType.INFORMATION, "Looks like NGROK is still running. Please kill it before running the provider again.");
                    System.exit(0);
                }
            }
            process.destroy();
            input.close();
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR ,"Not able to find the process -- > " + ex.getMessage());
        }
    }

    /// <summary>
    /// Kill ngrok.exe process
    /// </summary>
    public void dispose() {
        if (this.ngrokProcess != null) {
            this.ngrokProcess.destroy();
        }
    }

    /// <summary>
    /// Creates the NGROK process.
    /// </summary>
    private void createNgrokProcess(String ngrokPath, String authToken) {
        try {
            String authTokenArgs = "";
            if (authToken != null && !authToken.isEmpty()) {
                authTokenArgs = " --authtoken " + authToken;
            }

            String openCmd = "cmd /c start cmd.exe /k ";
            String fileName = ngrokPath + "ngrok.exe";
            String arguments = " http http://localhost:9007/ -host-header=/localhost:9007/" + authTokenArgs;
            String command = openCmd + fileName + arguments;

            this.ngrokProcess = Runtime.getRuntime().exec(command);
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR,"Failed to start Ngrok.exe -- > " + ex.getMessage());
        }
    }

    /// <summary>
    /// Get Ngrok URL
    /// </summary>
    public String getNgrokUrl() {
        String ngrokUrl = null;
        int totalAttempts = 3;
        try {
            do {
                // Wait for fetching the ngrok url as ngrok process might not be started yet.
                Thread.sleep(2000);
                JSONArray tunnelList = this.connector.getAllTunnelsAsync();

                if (tunnelList.iterator().hasNext()) {
                    Map<?, ?> tunnel = (Map<?, ?>) tunnelList.iterator().next();

                    for (Map.Entry<?, ?> entry : tunnel.entrySet()) {
                        if ((entry.getKey()).equals("public_url")) {
                            ngrokUrl = (String) entry.getValue();
                            return ngrokUrl;
                        }
                    }
                }
            } while (--totalAttempts > 0);
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR, "Failed to get Ngrok url -- > " + ex.getMessage());
        }
        return ngrokUrl;
    }
}