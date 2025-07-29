package com.communication.callautomation;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.ArrayList;

@Configuration
public class OpenApiConfig {

    @Value("${devtunnel.url:}")
    private String devTunnelUrl;
    
    @Value("${devtunnel.enabled:false}")
    private boolean devTunnelEnabled;
    
    @Value("${server.port:8443}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        List<Server> servers = new ArrayList<>();
        
        // Local server
        Server localServer = new Server();
        localServer.setUrl("http://localhost:" + serverPort);
        localServer.setDescription("Local Server (HTTP on port " + serverPort + ")");
        servers.add(localServer);

        // Dev Tunnel server (only if configured)
        if (devTunnelEnabled && StringUtils.hasText(devTunnelUrl)) {
            Server devTunnelServer = new Server();
            devTunnelServer.setUrl(devTunnelUrl);
            devTunnelServer.setDescription("Dev Tunnel Server (HTTPS via tunnel)");
            servers.add(devTunnelServer);
        }

        String description = buildDescription();

        return new OpenAPI()
                .info(new Info()
                        .title("Call Automation Lobby Call Sample API")
                        .description(description)
                        .version("1.0.0"))
                .servers(servers);
    }
    
    private String buildDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("Azure Communication Services Call Automation API for managing lobby calls.\n");
        desc.append("**🚀 Server Access Information:**\n");
        desc.append("- **Local**: [http://localhost:").append(serverPort).append("/swagger-ui/index.html](http://localhost:").append(serverPort).append("/swagger-ui/index.html)\n");
        
        if (devTunnelEnabled && StringUtils.hasText(devTunnelUrl)) {
            desc.append("- **Dev Tunnel**: [").append(devTunnelUrl).append("/swagger-ui/index.html](").append(devTunnelUrl).append("/swagger-ui/index.html)\n");
        } else {
            desc.append("- **Dev Tunnel**: Not configured (set DEVTUNNEL_URL environment variable)\n");
        }
        
        return desc.toString();
    }
}
