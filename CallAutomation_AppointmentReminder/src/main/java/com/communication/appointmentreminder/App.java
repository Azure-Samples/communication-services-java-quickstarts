package com.communication.appointmentreminder;

import com.communication.appointmentreminder.utitilities.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Collections;

@SpringBootApplication
public class App {
    private static final int SERVER_PORT = 9007;

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(App.class);
        application.setDefaultProperties(Collections.singletonMap("server.port", SERVER_PORT));
        application.run(args);

        Logger.logMessage(Logger.MessageType.INFORMATION, "Starting ACS Sample App ");

        // Get configuration properties
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        configurationManager.loadAppSettings();
        AppointmentReminder.setConfiguration();

        runSample();
        Logger.logMessage(Logger.MessageType.INFORMATION, "Listening to Events. Press 'Ctrl + C' to exit the sample");
    }

    private static void runSample() {
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        String identity = configurationManager.getAppSettings("DestinationIdentity");
        try {
            AppointmentReminder.executeReminder(identity.trim());
        } catch (Exception ex) {
            Logger.logMessage(Logger.MessageType.ERROR, "Failed to initiate the Appointment Reminder call. Exception -- > " + ex.getMessage());
        }
    }
}
