package com.communication.incomingcallquickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.*;

@SpringBootApplication
public class App 
{
    final static String url = "http://localhost:9007";
    final static String serverPort = "9007";
    public static void main( String[] args )
    {
        SpringApplication application = new SpringApplication(App.class);
        application.setDefaultProperties(Collections.singletonMap("server.port", serverPort));
        application.run(args);

        // Logger.logMessage(Logger.MessageType.INFORMATION, "Starting ACS InComing Call Sample App ");

        // Get configuration properties
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        configurationManager.loadAppSettings();
    }
}
