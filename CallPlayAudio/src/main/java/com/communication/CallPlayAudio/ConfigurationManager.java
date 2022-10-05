package com.communication.CallPlayAudio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class ConfigurationManager {
    private static ConfigurationManager configurationManager = null;
    private final Properties appSettings = new Properties();

    private ConfigurationManager() {
    }

    // static method to create instance of ConfigurationManager class
    public static ConfigurationManager getInstance() {
        if (configurationManager == null) {
            configurationManager = new ConfigurationManager();
        }
        return configurationManager;
    }

    public void loadAppSettings() {
        try {
            File configFile = new File("src/main/java/com/communication/CallPlayAudio/config.properties");
            FileReader reader = new FileReader(configFile);
            appSettings.load(reader);
            reader.close();
        } catch (FileNotFoundException ex) {
            Logger.logMessage(Logger.MessageType.INFORMATION,"Loading app settings failed with error -- > " + ex.getMessage());
        } catch (IOException ex) {
            Logger.logMessage(Logger.MessageType.ERROR,"Loading app settings failed with error -- > " + ex.getMessage());
        }
    }

    public String getAppSettings(String key) {
        if (!key.isEmpty()) {
            return appSettings.getProperty(key);
        }
        return "";
    }
}