package com.acsrecording.api;

import java.io.*;
import java.util.Properties;

public class ConfigurationManager {
    private static ConfigurationManager configurationManager = null;
    private Properties appSettings = new Properties();

    private ConfigurationManager() {
    }

    // static method to create instance of ConfigurationManager class
    public static ConfigurationManager getInstance() {
        if (configurationManager == null) {
            configurationManager = new ConfigurationManager();
            configurationManager.loadAppSettings();
        }
        return configurationManager;
    }

    public void loadAppSettings() {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream("config.properties");
            Reader reader = new InputStreamReader(inputStream);
            appSettings.load(reader);
            reader.close();
        } catch (FileNotFoundException ex) {
            System.out.print("\n Loading app settings failed with error - " + ex.getMessage());
        } catch (IOException ex) {
            System.out.print("\n Loading app settings failed with error - " + ex.getMessage());
        }
    }

    public String getAppSettings(String key) {
        if (!key.isEmpty()) {
            return appSettings.getProperty(key);
        }
        return "";
    }
}