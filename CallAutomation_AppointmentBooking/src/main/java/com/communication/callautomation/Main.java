package com.communication.callautomation;

import com.azure.core.http.HttpClient;
import com.communication.callautomation.config.AcsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(value = AcsConfig.class)
public class Main {

    @Bean
    HttpClient azureHttpClient() {
        return HttpClient.createDefault();
    }
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}