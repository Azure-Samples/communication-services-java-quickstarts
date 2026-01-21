package com.communication.callautomation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${acs.callbackUriHost}")
    private String callbackUriHost;

    @Bean
    public OpenAPI customOpenAPI() {
        Server server = new Server();
        server.setUrl(callbackUriHost);
        server.setDescription("Deployed Server");
        
        return new OpenAPI()
                .info(new Info()
                        .title("Call Automation GCCH API")
                        .description("Call Automation API for GCCH")
                        .version("1.0"))
                .servers(List.of(server));
    }
}