package com.markovitz.notificationservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ============================================================================
 * CONFIGURAÇÃO DO SWAGGER / OPENAPI
 * ============================================================================
 *
 * Acesse a UI em: http://localhost:8084/swagger-ui.html
 * Acesse o JSON em: http://localhost:8084/v3/api-docs
 *
 * ============================================================================
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification Service API")
                        .description(
                                "Microsserviço de notificações do Sistema Markovitz.\n\n" +
                                "Responsável por:\n" +
                                "- Consumir eventos assíncronos do RabbitMQ:\n" +
                                "  - 'user.registered' → gera notificação de boas-vindas\n" +
                                "  - 'portfolio.optimized' → gera notificação com resultado da otimização\n" +
                                "- Disponibilizar as notificações via API REST para o aplicativo móvel"
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe Markovitz")
                                .email("markovitz@ads.edu.br")
                        )
                );
    }
}
