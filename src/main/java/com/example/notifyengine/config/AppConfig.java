package com.example.notifyengine.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Motor de Regras para Notificações")
                        .description("Decide se, quando, por qual canal e para quem enviar notificações, com base em regras configuráveis.")
                        .version("1.0.0"))
                // Habilita o botão "Authorize" do Swagger UI para informar a chave de API.
                .components(new Components().addSecuritySchemes("apiKey",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(ApiKeyAuthFilter.HEADER)))
                .addSecurityItem(new SecurityRequirement().addList("apiKey"));
    }
}
