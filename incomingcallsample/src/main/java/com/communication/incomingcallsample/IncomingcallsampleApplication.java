package com.communication.incomingcallsample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import java.util.*;

import com.communication.incomingcallsample.Controller.IncomingCallController;


@SpringBootApplication
@ComponentScan(basePackageClasses= IncomingCallController.class)
public class IncomingcallsampleApplication {
	final static String url = "http://localhost:9008";
    final static String serverPort = "9008";

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(IncomingcallsampleApplication.class);
        app.setDefaultProperties(Collections
          .singletonMap("server.port", serverPort));
        app.run(args);


	}

}
