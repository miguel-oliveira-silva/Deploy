package com.markovitz.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ============================================================================
 * CONFIGURAÇÃO DO RABBITMQ — notification-service
 * ============================================================================
 *
 * Este serviço é APENAS CONSUMIDOR — não publica nenhuma mensagem.
 *
 * Filas consumidas:
 * ─────────────────────────────────────────────────────────────────────────
 *
 *  Exchange: markovitz.exchange
 *  ┌──────────────────────────────┬────────────────────────────────────────┐
 *  │ Routing Key                  │ Queue                                  │
 *  ├──────────────────────────────┼────────────────────────────────────────┤
 *  │ user.registered              │ user.registered.queue                  │
 *  │ portfolio.optimized          │ portfolio.optimized.queue              │
 *  └──────────────────────────────┴────────────────────────────────────────┘
 *
 * IMPORTANTE: Por que declarar as filas aqui também?
 * ─────────────────────────────────────────────────────────────────────────
 * Se o notification-service iniciar ANTES dos outros serviços,
 * as filas podem não existir ainda no RabbitMQ.
 * Ao declarar aqui, garantimos que as filas existem quando
 * o @RabbitListener tentar se conectar a elas.
 *
 * O RabbitMQ é IDEMPOTENTE: declarar uma fila que já existe
 * (com os mesmos parâmetros) não causa erro — a declaração é ignorada.
 *
 * ============================================================================
 */
@Configuration
public class RabbitMQConfig {

    // Exchange compartilhada por todos os serviços
    public static final String EXCHANGE_NAME = "markovitz.exchange";

    // Fila publicada pelo user-service, consumida aqui
    public static final String USER_REGISTERED_QUEUE      = "user.registered.queue";
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";

    // Fila publicada pelo portfolio-service, consumida aqui
    public static final String PORTFOLIO_OPTIMIZED_QUEUE       = "portfolio.optimized.queue";
    public static final String PORTFOLIO_OPTIMIZED_ROUTING_KEY = "portfolio.optimized";

    @Bean
    public TopicExchange markovitzExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    // =========================================================================
    // FILAS — declaradas para garantir existência antes de consumir
    // =========================================================================

    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable(USER_REGISTERED_QUEUE).build();
    }

    @Bean
    public Queue portfolioOptimizedQueue() {
        return QueueBuilder.durable(PORTFOLIO_OPTIMIZED_QUEUE).build();
    }

    // =========================================================================
    // BINDINGS
    // =========================================================================

    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue,
                                          TopicExchange markovitzExchange) {
        return BindingBuilder
                .bind(userRegisteredQueue)
                .to(markovitzExchange)
                .with(USER_REGISTERED_ROUTING_KEY);
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

    /**
     * RabbitTemplate: necessário mesmo que só consumamos.
     * O Spring AMQP precisa dele para configurar o conversor de mensagens
     * nos listeners (@RabbitListener).
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
