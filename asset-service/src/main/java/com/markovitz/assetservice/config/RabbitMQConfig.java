package com.markovitz.assetservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ============================================================================
 * CONFIGURAÇÃO DO RABBITMQ — asset-service
 * ============================================================================
 *
 * REVISÃO: Como funciona o roteamento no RabbitMQ?
 * ─────────────────────────────────────────────────────────────────────────
 *
 *  PRODUCER          EXCHANGE              QUEUE              CONSUMER
 *  ──────────        ─────────────         ────────────────   ──────────────
 *  asset-service  →  markovitz.exchange →  asset.price       portfolio-service
 *                    (TopicExchange)        .updated.queue    (consome e re-otimiza)
 *
 * ROUTING KEY usado: "asset.price.updated"
 *
 * Por que usar o MESMO exchange do user-service?
 * ─────────────────────────────────────────────────────────────────────────
 * Usamos um exchange único "markovitz.exchange" para todo o sistema.
 * Isso é um padrão comum: um exchange central com múltiplas filas,
 * cada uma com seu binding e routing key diferente.
 *
 * O RabbitMQ roteia as mensagens pela routing key:
 *   "user.registered"    → vai para user.registered.queue
 *   "asset.price.updated" → vai para asset.price.updated.queue
 *   "portfolio.optimized" → vai para portfolio.optimized.queue
 *
 * ============================================================================
 */
@Configuration
public class RabbitMQConfig {

    // =========================================================================
    // CONSTANTES
    // =========================================================================

    /** Exchange central do sistema — o mesmo usado pelo user-service */
    public static final String EXCHANGE_NAME = "markovitz.exchange";

    /**
     * Fila onde o portfolio-service irá consumir eventos de preço atualizado.
     * Quando um novo preço chega, o portfolio-service re-otimiza as carteiras
     * que contêm este ativo.
     */
    public static final String ASSET_PRICE_UPDATED_QUEUE = "asset.price.updated.queue";

    /**
     * Routing key do evento de preço atualizado.
     * O portfolio-service faz o binding com esta chave para receber os eventos.
     */
    public static final String ASSET_PRICE_UPDATED_ROUTING_KEY = "asset.price.updated";

    // =========================================================================
    // BEANS
    // =========================================================================

    /**
     * Exchange do tipo Topic — o mesmo que o user-service declarou.
     *
     * IMPORTANTE: O Spring AMQP é "idempotente" na declaração de exchanges e filas.
     * Se o exchange já existir com os mesmos parâmetros, não há erro.
     * Se existir com parâmetros DIFERENTES, lança exceção.
     *
     * Por isso, todos os serviços devem declarar o exchange com os mesmos parâmetros.
     */
    @Bean
    public TopicExchange markovitzExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    /**
     * Fila para eventos de preço atualizado.
     * Esta fila será consumida pelo portfolio-service.
     * O asset-service apenas a DECLARA para garantir que ela exista
     * antes de alguém tentar consumir.
     */
    @Bean
    public Queue assetPriceUpdatedQueue() {
        return QueueBuilder
                .durable(ASSET_PRICE_UPDATED_QUEUE)
                .build();
    }

    /**
     * Binding: liga o exchange à fila usando a routing key.
     *
     * "Toda mensagem no exchange 'markovitz.exchange' com routing key
     *  'asset.price.updated' deve ir para 'asset.price.updated.queue'"
     */
    @Bean
    public Binding assetPriceUpdatedBinding(Queue assetPriceUpdatedQueue,
                                             TopicExchange markovitzExchange) {
        return BindingBuilder
                .bind(assetPriceUpdatedQueue)
                .to(markovitzExchange)
                .with(ASSET_PRICE_UPDATED_ROUTING_KEY);
    }

    /** Conversor JSON: serializa objetos Java ↔ JSON nas mensagens */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** RabbitTemplate configurado com conversor JSON */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
