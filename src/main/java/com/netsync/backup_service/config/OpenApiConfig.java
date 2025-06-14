package com.netsync.backup_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI backupServiceApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Backup Service API")
                .description("API para gestionar backups de dispositivos")
                .version("1.0.0")
                .contact(new Contact()
                    .name("NETSYNC")
                    .email("soporte@netsync.com")
                )
            );
    }
}
