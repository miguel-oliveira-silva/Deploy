package com.markovitz.userservice.event;

import com.markovitz.userservice.entity.User;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * EVENT — Objeto de Evento para Comunicação Assíncrona via RabbitMQ
 * ============================================================================
 *
 * O que é um Evento?
 * ─────────────────────────────────────────────────────────────────────────
 * Um evento representa algo que ACONTECEU no sistema — um fato no passado.
 * O nome sempre é no particípio passado: "UserRegistered" (Usuário Registrado).
 *
 * Eventos vs. Comandos na mensageria:
 *   - EVENTO: "Usuário foi registrado" → notifica quem estiver interessado
 *   - COMANDO: "Envie um email" → instrução direta para um receptor
 *
 * COMUNICAÇÃO ASSÍNCRONA:
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Quando o user-service publica este evento no RabbitMQ:
 *   1. O user-service NÃO espera resposta — continua seu trabalho
 *   2. O RabbitMQ armazena o evento na fila
 *   3. O notification-service consome o evento quando estiver disponível
 *   4. Se o notification-service estiver offline, o evento fica na fila
 *      e será processado quando ele voltar  ESILIÊNCIA
 *
 * Comparação com chamada síncrona (REST):
 *   - REST:       user-service → [aguarda] → notification-service
 *   - RabbitMQ:   user-service → [segue em frente] → notification-service (processa depois)
 *
 * SERIALIZAÇÃO:
 * ─────────────────────────────────────────────────────────────────────────
 * O Jackson2JsonMessageConverter (configurado em RabbitMQConfig) converte
 * este objeto Java em JSON para publicar na fila, e converte de volta em
 * Java ao consumir. Por isso:
 *   - Todos os campos precisam ser públicos (via getters) ou ter @JsonProperty
 *   - A classe precisa ter construtor vazio para desserialização
 *
 * EXEMPLO DO JSON publicado na fila:
 * {
 *   "userId": 1,
 *   "userName": "João Silva",
 *   "userEmail": "joao@email.com",
 *   "riskProfile": "MODERADO",
 *   "occurredAt": "2024-01-15T14:30:00"
 * }
 *
 * ============================================================================
 */
public class UserRegisteredEvent {

    /**
     * ID do usuário recém-cadastrado.
     * O notification-service usa este ID para saber para qual usuário
     * enviar a notificação.
     */
    private Long userId;

    /** Nome do usuário — para personalizar a mensagem de boas-vindas */
    private String userName;

    /** Email do usuário */
    private String userEmail;

    /** Perfil de risco — o notification-service pode incluir dicas na boas-vindas */
    private User.RiskProfile riskProfile;

    /**
     * Momento em que o evento ocorreu.
     *
     * BOAS PRÁTICAS:
     * Sempre inclua um timestamp no evento para:
     *   - Rastreabilidade (quando exatamente aconteceu?)
     *   - Ordenação temporal de eventos
     *   - Detecção de eventos duplicados (idempotência)
     */
    private LocalDateTime occurredAt;

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================

    /** Construtor padrão — obrigatório para o Jackson desserializar */
    public UserRegisteredEvent() {}

    /** Construtor completo */
    public UserRegisteredEvent(Long userId, String userName, String userEmail,
                               User.RiskProfile riskProfile, LocalDateTime occurredAt) {
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.riskProfile = riskProfile;
        this.occurredAt = occurredAt;
    }

    // =========================================================================
    // MÉTODO DE FÁBRICA
    // =========================================================================

    /**
     * Método de fábrica para criar o evento a partir da entidade User.
     * Isso evita que o Service precise conhecer os detalhes internos do evento.
     *
     * @param user o usuário recém-cadastrado
     * @return evento pronto para ser publicado no RabbitMQ
     */
    public static UserRegisteredEvent from(User user) {
        return new UserRegisteredEvent(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRiskProfile(),
                LocalDateTime.now() // momento exato do evento
        );
    }

    // =========================================================================
    // Getters e Setters
    // =========================================================================

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public User.RiskProfile getRiskProfile() { return riskProfile; }
    public void setRiskProfile(User.RiskProfile riskProfile) { this.riskProfile = riskProfile; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    @Override
    public String toString() {
        return "UserRegisteredEvent{userId=" + userId +
                ", userName='" + userName + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", riskProfile=" + riskProfile +
                ", occurredAt=" + occurredAt + '}';
    }
}
