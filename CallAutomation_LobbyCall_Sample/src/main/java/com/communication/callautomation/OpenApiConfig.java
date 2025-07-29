package com.communication.callautomation;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Server httpsServer = new Server();
        httpsServer.setUrl("https://localhost:8443");
        httpsServer.setDescription("HTTPS Server (Primary)");

        Server httpServer = new Server();
        httpServer.setUrl("http://localhost:8080");
        httpServer.setDescription("HTTP Server (Alternative)");

        return new OpenAPI()
                .info(new Info()
                        .title("Call Automation Lobby Call Sample API")
                        .description("Azure Communication Services Call Automation API for managing lobby calls.\n\n" +
                                "**Server Selection:**\n" +
                                "- HTTPS Server (Primary): https://localhost:8443 - Secure connection\n" +
                                "- HTTP Server (Alternative): http://localhost:8080 - Non-secure connection\n\n" +
                                "**Note:** Both servers are now available. HTTPS uses a self-signed certificate.")
                        .version("1.0.0"))
                .servers(List.of(httpsServer, httpServer));
    }
}
