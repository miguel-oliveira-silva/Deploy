package com.markovitz.userservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ============================================================================
 * CONFIGURAÇÃO DO RABBITMQ
 * ============================================================================
 *
 * Esta classe configura toda a infraestrutura de mensageria do user-service.
 *
 * CONCEITOS FUNDAMENTAIS DO RABBITMQ:
 * ─────────────────────────────────────────────────────────────────────────
 *
 * 1. PRODUCER (Publicador)
 *    Quem envia mensagens. No user-service, o UserService publica eventos
 *    quando um usuário se cadastra.
 *
 * 2. EXCHANGE (Roteador)
 *    Recebe a mensagem do Producer e decide para qual(is) fila(s) enviá-la.
 *    Tipos de Exchange:
 *      - Direct:  roteia por routing key exata
 *      - Topic:   roteia por padrão (ex: "user.*" pega "user.registered")
 *      - Fanout:  envia para TODAS as filas vinculadas (broadcast)
 *      - Headers: roteia por atributos do cabeçalho
 *
 *    Usaremos TopicExchange para flexibilidade máxima.
 *
 * 3. QUEUE (Fila)
 *    Armazena as mensagens até que um consumidor as processe.
 *    As filas são persistentes — as mensagens não se perdem se o RabbitMQ reiniciar.
 *
 * 4. BINDING (Vínculo)
 *    Liga um Exchange a uma Queue usando uma routing key (chave de roteamento).
 *    Ex: mensagens com routing key "user.registered" vão para "user.registered.queue"
 *
 * 5. CONSUMER (Consumidor)
 *    Quem lê e processa as mensagens da fila.
 *    No nosso sistema, o notification-service consumirá "user.registered.queue".
 *
 * FLUXO COMPLETO:
 *   UserService → RabbitTemplate → Exchange → (routing key) → Queue → notification-service
 *
 * ============================================================================
 *
 * @Configuration → diz ao Spring que esta classe contém definições de beans
 * @Bean → diz ao Spring para gerenciar o objeto retornado pelo método
 */
@Configuration
public class RabbitMQConfig {

    // =========================================================================
    // CONSTANTES — Nomes das filas, exchanges e routing keys
    // =========================================================================
    // Boas práticas: centralizar os nomes como constantes evita erros de digitação
    // e facilita manutenção. Outros serviços usarão os mesmos nomes.

    /** Nome do exchange principal do sistema Markovitz */
    public static final String EXCHANGE_NAME = "markovitz.exchange";

    /** Nome da fila onde eventos de usuário registrado são armazenados */
    public static final String USER_REGISTERED_QUEUE = "user.registered.queue";

    /**
     * Routing key para o evento de usuário registrado.
     * O "#" no final seria um wildcard em padrão topic, mas aqui usamos
     * a chave exata para enviar ao notification-service.
     */
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";

    // =========================================================================
    // BEANS DE CONFIGURAÇÃO
    // =========================================================================

    /**
     * Define o Exchange do tipo Topic.
     *
     * TopicExchange permite usar wildcards nas routing keys:
     *   "*" = substitui exatamente UMA palavra
     *   "#" = substitui zero ou mais palavras
     *
     * Ex: binding com "user.*" recebe tanto "user.registered" quanto "user.deleted"
     *
     * durable(true) = o exchange sobrevive a reinicializações do RabbitMQ
     */
    @Bean
    public TopicExchange markovitzExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
        //                                         ↑      ↑
        //                                     durable  autoDelete
        // autoDelete=false = não apaga o exchange quando não há consumidores
    }

    /**
     * Define a fila que armazenará eventos de usuário registrado.
     *
     * QueueBuilder oferece uma API fluente para configurar filas.
     * durable() = a fila sobrevive a reinicializações do RabbitMQ
     *
     * IMPORTANTE: Esta fila será consumida pelo notification-service,
     * mas ela precisa existir antes de alguém tentar consumir.
     * O Spring AMQP cria a fila automaticamente se ela não existir.
     */
    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder
                .durable(USER_REGISTERED_QUEUE)  // fila persistente com o nome definido
                .build();
    }

    /**
     * Cria o BINDING entre o Exchange e a Queue.
     *
     * Isso diz ao RabbitMQ:
     * "Toda mensagem que chegar no exchange 'markovitz.exchange'
     *  com a routing key 'user.registered'
     *  deve ser colocada na fila 'user.registered.queue'"
     *
     * BindingBuilder oferece uma API fluente para criar bindings.
     */
    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue,
                                         TopicExchange markovitzExchange) {
        return BindingBuilder
                .bind(userRegisteredQueue)          // queue de destino
                .to(markovitzExchange)              // exchange de origem
                .with(USER_REGISTERED_ROUTING_KEY); // routing key que ativa este binding
    }

    /**
     * Configura o conversor de mensagens para usar JSON.
     *
     * Por padrão, o Spring AMQP serializa objetos Java em formato binário (Java Serialization).
     * Isso é problemático porque:
     *   1. Outros serviços em outras linguagens não conseguem ler
     *   2. É mais difícil de debugar (não é legível por humanos)
     *
     * Jackson2JsonMessageConverter serializa/deserializa usando JSON — muito melhor!
     * Agora você pode ver as mensagens no RabbitMQ Management UI em formato JSON legível.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configura o RabbitTemplate — a ferramenta principal para PUBLICAR mensagens.
     *
     * RabbitTemplate é o equivalente do RestTemplate, mas para mensageria.
     * Ele fornece métodos como:
     *   - convertAndSend(exchange, routingKey, objeto) → serializa e envia
     *   - receiveAndConvert(queue) → recebe e deserializa
     *
     * Aqui configuramos para usar nosso conversor JSON ao invés do binário padrão.
     *
     * @param connectionFactory fornecido automaticamente pelo Spring Boot
     *        com base nas configurações do application.yml (host, port, user, pass)
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        // Usa o conversor JSON que definimos acima
        template.setMessageConverter(jsonMessageConverter());

        return template;
    }
}
