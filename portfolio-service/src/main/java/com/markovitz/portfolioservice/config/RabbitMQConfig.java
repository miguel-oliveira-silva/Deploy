package com.markovitz.portfolioservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * ============================================================================
 * CONFIGURAÇÃO DO RABBITMQ + REST CLIENT — portfolio-service
 * ============================================================================
 *
 * Este serviço tem a configuração mais completa de mensageria:
 *
 * CONSOME:
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │ Exchange: markovitz.exchange                                │
 *   │ Queue:    asset.price.updated.queue                         │
 *   │ Routing:  asset.price.updated                               │
 *   │ Ação:     re-otimiza carteiras que contêm o ativo atualizado│
 *   └─────────────────────────────────────────────────────────────┘
 *
 * PUBLICA:
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │ Exchange: markovitz.exchange                                │
 *   │ Queue:    portfolio.optimized.queue                         │
 *   │ Routing:  portfolio.optimized                               │
 *   │ Ação:     notifica o notification-service                   │
 *   └─────────────────────────────────────────────────────────────┘
 *
 * RESTTEMPLATE:
 *   Configuramos o RestTemplate aqui para chamar o asset-service
 *   via HTTP REST e buscar as estatísticas dos ativos (μ e σ).
 *
 * ============================================================================
 */
@Configuration
public class RabbitMQConfig {

    // =========================================================================
    // CONSTANTES
    // =========================================================================

    public static final String EXCHANGE_NAME = "markovitz.exchange";

    /** Fila que este serviço CONSOME — publicada pelo asset-service */
    public static final String ASSET_PRICE_UPDATED_QUEUE    = "asset.price.updated.queue";
    public static final String ASSET_PRICE_UPDATED_ROUTING_KEY = "asset.price.updated";

    /** Fila que este serviço PUBLICA — consumida pelo notification-service */
    public static final String PORTFOLIO_OPTIMIZED_QUEUE       = "portfolio.optimized.queue";
    public static final String PORTFOLIO_OPTIMIZED_ROUTING_KEY = "portfolio.optimized";

    // =========================================================================
    // EXCHANGE — único e compartilhado por todos os serviços
    // =========================================================================

    @Bean
    public TopicExchange markovitzExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    // =========================================================================
    // FILAS
    // =========================================================================

    /**
     * Fila de eventos de preço atualizado — o portfolio-service a CONSOME.
     * Declarada aqui para garantir que existe antes de começar a consumir.
     * O asset-service também a declara — o RabbitMQ garante idempotência.
     */
    @Bean
    public Queue assetPriceUpdatedQueue() {
        return QueueBuilder.durable(ASSET_PRICE_UPDATED_QUEUE).build();
    }

    /**
     * Fila de eventos de carteira otimizada — o portfolio-service PUBLICA aqui.
     * O notification-service irá consumir desta fila.
     */
    @Bean
    public Queue portfolioOptimizedQueue() {
        return QueueBuilder.durable(PORTFOLIO_OPTIMIZED_QUEUE).build();
    }

    // =========================================================================
    // BINDINGS
    // =========================================================================

    @Bean
    public Binding assetPriceUpdatedBinding(Queue assetPriceUpdatedQueue,
                                             TopicExchange markovitzExchange) {
        return BindingBuilder
                .bind(assetPriceUpdatedQueue)
                .to(markovitzExchange)
                .with(ASSET_PRICE_UPDATED_ROUTING_KEY);
    }

    @Bean
    public Binding portfolioOptimizedBinding(Queue portfolioOptimizedQueue,
                                              TopicExchange markovitzExchange) {
        return BindingBuilder
                .bind(portfolioOptimizedQueue)
                .to(markovitzExchange)
                .with(PORTFOLIO_OPTIMIZED_ROUTING_KEY);
    }

    // =========================================================================
    // CONVERSOR E TEMPLATE
    // =========================================================================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    // =========================================================================
    // REST TEMPLATE — Cliente HTTP para chamar outros microsserviços
    // =========================================================================

    /**
     * RestTemplate é o cliente HTTP do Spring para fazer chamadas REST
     * entre microsserviços.
     *
     * COMUNICAÇÃO SÍNCRONA vs ASSÍNCRONA:
     * ─────────────────────────────────────────────────────────────────────
     * RestTemplate → SÍNCRONA: o portfolio-service AGUARDA a resposta
     *   do asset-service antes de continuar. É um bloqueio!
     *
     * RabbitMQ → ASSÍNCRONA: o serviço envia e SEGUE EM FRENTE.
     *
     * Quando usar cada uma?
     *   - REST síncrono: quando PRECISA da resposta para continuar
     *     (ex: buscar as estatísticas dos ativos para poder otimizar)
     *   - Mensageria assíncrona: quando é um EVENTO de notificação
     *     (ex: "carteira foi otimizada" → notifique o usuário)
     *
     * Em produção com muitos serviços, usaríamos Spring WebClient
     * (não bloqueante) ou OpenFeign (declarativo) ao invés de RestTemplate.
     * Mas para fins didáticos, RestTemplate é mais simples de entender.
     *
     * @return instância do RestTemplate pronta para uso
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
