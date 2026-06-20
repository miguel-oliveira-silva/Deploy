package com.markovitz.portfolioservice.config;

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
 * Acesse a UI em: http://localhost:8083/swagger-ui.html
 * Acesse o JSON em: http://localhost:8083/v3/api-docs
 *
 * ============================================================================
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI portfolioServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Portfolio Service API")
                        .description(
                                "Microsserviço de otimização de carteiras do Sistema Markovitz.\n\n" +
                                "Responsável por:\n" +
                                "- Criação e gestão de carteiras de investimento\n" +
                                "- Execução do algoritmo de Markowitz (Fronteira Eficiente)\n" +
                                "- Comunicação síncrona com o asset-service para obter μ e σ de cada ativo\n" +
                                "- Publicação de eventos 'portfolio.optimized' via RabbitMQ\n\n" +
                                "**Algoritmo:** Maximização do Índice de Sharpe = (Retorno − Taxa Livre de Risco) / Volatilidade"
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe Markovitz")
                                .email("markovitz@ads.edu.br")
                        )
                );
    }
}
