package com.communication.outboundcallreminder.Ngrok;

import org.json.simple.JSONArray;
import java.io.*;
import java.util.*;

public class NgrokService {
    /// The NGROK process
    private Process ngrokProcess;
    // NgrokConnector connector;
    private NgrokConnector connector = null;

    public NgrokService(String ngrokPath, String authToken) {
        connector = new NgrokConnector();
        this.EnsureNgrokNotRunning();
        this.CreateNgrokProcess(ngrokPath, authToken);
    }

    /// <summary>
    /// Ensures that NGROK is not running.
    /// </summary>
    private void EnsureNgrokNotRunning() {
        BufferedReader input = null;
        String ngrokProcess = "ngrok.exe";
        String processFilter = "/nh /fi \"Imagename eq " + ngrokProcess + "\"";
        String tasksCmd = System.getenv("windir") + "/system32/tasklist.exe " + processFilter;
        try {
            Process process = Runtime.getRuntime().exec(tasksCmd);
            input = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = null;
            while ((line = input.readLine()) != null) {
                // It means ngrok.exe running
                if (line.contains(ngrokProcess)) {
                    System.out.println(
                            "Looks like NGROK is still running. Please kill it before running the provider again.");
                    System.exit(0);
                }
            }
            process.destroy();
            input.close();
        } catch (Exception ex) {
            System.out.println("Not able to find the process: " + ex.getMessage());
        }
    }

    /// <summary>
    /// Kill ngrok.exe process
    /// </summary>
    public void Dispose() {
        if (this.ngrokProcess != null) {
            this.ngrokProcess.destroy();
        }
    }

    /// <summary>
    /// Creates the NGROK process.
    /// </summary>
    private void CreateNgrokProcess(String ngrokPath, String authToken) {
        try {
            String authTokenArgs = "";
            if (authToken != null && !authToken.isEmpty()) {
                authTokenArgs = "--authtoken " + authToken;
            }

            String openCmd = "cmd /c start cmd.exe /k ";
            String fileName = ngrokPath + "ngrok.exe";
            String arguments = " http http://localhost:9007/ -host-header=/localhost:9007/" + authTokenArgs;
            String command = openCmd + fileName + arguments;

            this.ngrokProcess = Runtime.getRuntime().exec(command);
        } catch (Exception ex) {
            System.out.println("Failed to start Ngrok.exe : " + ex.getMessage());
        }
    }

    /// <summary>
    /// Get Ngrok URL
    /// </summary>
    public String GetNgrokUrl() {
        String ngrokUrl = null;
        int totalAttempts = 3;
        try {
            do {
                // Wait for fetching the ngrok url as ngrok process might not be started yet.
                Thread.sleep(2000);
                JSONArray tunnelList = this.connector.GetAllTunnelsAsync();

                if (tunnelList.iterator().hasNext()) {
                    Map<?, ?> tunnel = (Map<?, ?>) tunnelList.iterator().next();
                    Iterator<?> tunneIterator = tunnel.entrySet().iterator();

                    while (tunneIterator.hasNext()) {
                        Map.Entry<?, ?> keyVal = (Map.Entry<?, ?>) tunneIterator.next();
                        if ((keyVal.getKey()).equals("public_url")) {
                            ngrokUrl = (String) keyVal.getValue();
                            return ngrokUrl;
                        }
                    }
                }
            } while (--totalAttempts > 0);
        } catch (Exception ex) {
            System.out.println("Failed to get Ngrok url" + ex.getMessage());
        }
        return ngrokUrl;
    }
}