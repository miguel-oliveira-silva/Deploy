package com.markovitz.assetservice.config;

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
 * Acesse a UI em: http://localhost:8082/swagger-ui.html
 * Acesse o JSON em: http://localhost:8082/v3/api-docs
 *
 * ============================================================================
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI assetServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Asset Service API")
                        .description(
                                "Microsserviço de gestão de ativos financeiros do Sistema Markovitz.\n\n" +
                                "Responsável por:\n" +
                                "- Cadastro de ativos (ações, FIIs, ETFs) por ticker\n" +
                                "- Registro de histórico de preços de fechamento\n" +
                                "- Cálculo de retorno médio (μ) e volatilidade (σ) — inputs do algoritmo de Markowitz\n" +
                                "- Publicação de eventos 'asset.price.updated' via RabbitMQ"
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe Markovitz")
                                .email("markovitz@ads.edu.br")
                        )
                );
    }
}
