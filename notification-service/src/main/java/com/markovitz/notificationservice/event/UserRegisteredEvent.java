package com.markovitz.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * Evento consumido do user-service via RabbitMQ.
 * ============================================================================
 *
 * Publicado quando um usuário se cadastra no sistema.
 * Os campos devem corresponder ao JSON publicado pelo user-service.
 *
 * @JsonIgnoreProperties(ignoreUnknown = true)
 * ─────────────────────────────────────────────────────────────────────────
 * Esta anotação instrui o Jackson a IGNORAR campos desconhecidos no JSON.
 *
 * Por que é importante em microsserviços?
 *
 * Cenário sem esta anotação:
 *   - user-service adiciona um campo novo ao evento: "phoneNumber"
 *   - notification-service NÃO tem este campo na classe
 *   - Ao deserializar, Jackson lança exceção → mensagem vai para a DLQ!
 *   → O sistema quebra por causa de uma mudança em OUTRO serviço.
 *
 * Cenário COM esta anotação:
 *   - Jackson simplesmente ignora "phoneNumber"
 *   - notification-service processa normalmente
 *   → Resiliência e desacoplamento!
 *
 * Boas práticas em microsserviços: sempre use @JsonIgnoreProperties nos
 * eventos consumidos de outros serviços. Cada serviço evolui de forma
 * independente — não force acoplamento rígido de versões de schema.
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRegisteredEvent {

    private Long userId;
    private String userName;
    private String userEmail;
    private Object riskProfile;   // Object para tolerar qualquer variação do enum
    private LocalDateTime occurredAt;

    public UserRegisteredEvent() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public Object getRiskProfile() { return riskProfile; }
    public void setRiskProfile(Object riskProfile) { this.riskProfile = riskProfile; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    @Override
    public String toString() {
        return "UserRegisteredEvent{userId=" + userId + ", userName='" + userName +
                "', userEmail='" + userEmail + "'}";
    }
}
