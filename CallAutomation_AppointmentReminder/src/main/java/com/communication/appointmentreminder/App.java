package com.communication.appointmentreminder;

import com.communication.appointmentreminder.models.CallAutomationClientConfiguration;
import com.communication.appointmentreminder.utitilities.Identity;
import com.communication.appointmentreminder.utitilities.Logger;
import com.communication.appointmentreminder.utitilities.Speech;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        AppointmentReminder.setCConfiguration();

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
