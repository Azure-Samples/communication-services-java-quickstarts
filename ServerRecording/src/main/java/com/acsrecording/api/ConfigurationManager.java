// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.acsrecording.api;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
            configurationManager.loadAppSettings();
        }
        return configurationManager;
    }

    public void loadAppSettings() {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream("config.properties");
            assert inputStream != null;
            Reader reader = new InputStreamReader(inputStream);
            appSettings.load(reader);
            reader.close();
        } catch (Exception ex) {
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