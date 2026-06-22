package com.markovitz.notificationservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade Notification - notificações persistidas no banco.
 *
 * Cada evento consumido do RabbitMQ gera uma Notification salva aqui.
 * Permite: histórico de notificações, rastreabilidade, re-envio em caso de falha.
 *
 * Ciclo de vida:
 *   PENDENTE → criada ao receber o evento
 *   ENVIADA  → processada com sucesso
 *   FALHA    → erro no envio
 *
 * Obs: neste projeto o "envio" é simulado (apenas log no console).
 * Em produção seria integrado com Spring Mail, Firebase, Twilio, etc.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID do usuário destinatário.
     * Referência ao user-service (não é FK - microsserviços têm bancos separados).
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * Tipo da notificação - indica qual evento originou.
     * @Enumerated(STRING) salva "BOAS_VINDAS" no banco, não "0" ou "1".
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /** Título curto da notificação */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Corpo da mensagem. VARCHAR(2000) para mensagens longas.
     */
    @Column(nullable = false, length = 2000)
    private String message;

    /** Status atual da notificação */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    /**
     * Nome do evento de origem para rastreabilidade.
     * Ex: "user.registered", "portfolio.optimized"
     */
    @Column(length = 100)
    private String sourceEvent;

    /**
     * ID do recurso de origem (ex: ID do portfólio otimizado).
     * Útil para links no front-end.
     */
    @Column
    private Long sourceId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime sentAt;

    // Enums

    public enum NotificationType {
        /** Notificação enviada quando um novo usuário se cadastra */
        BOAS_VINDAS,
        /** Notificação enviada quando uma carteira é otimizada */
        CARTEIRA_OTIMIZADA,
        /** Notificação genérica de sistema */
        SISTEMA
    }

    public enum NotificationStatus {
        PENDENTE,
        ENVIADA,
        FALHA
    }

    // Construtores

    public Notification() {}

    public Notification(Long id, Long userId, NotificationType type,
                         String title, String message, NotificationStatus status,
                         String sourceEvent, Long sourceId) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.status = status;
        this.sourceEvent = sourceEvent;
        this.sourceId = sourceId;
    }

    // Builder

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long userId;
        private NotificationType type;
        private String title;
        private String message;
        private NotificationStatus status;
        private String sourceEvent;
        private Long sourceId;

        public Builder id(Long id)                    { this.id = id; return this; }
        public Builder userId(Long uid)               { this.userId = uid; return this; }
        public Builder type(NotificationType t)       { this.type = t; return this; }
        public Builder title(String title)            { this.title = title; return this; }
        public Builder message(String msg)            { this.message = msg; return this; }
        public Builder status(NotificationStatus s)   { this.status = s; return this; }
        public Builder sourceEvent(String e)          { this.sourceEvent = e; return this; }
        public Builder sourceId(Long sid)             { this.sourceId = sid; return this; }

        public Notification build() {
            return new Notification(id, userId, type, title, message, status, sourceEvent, sourceId);
        }
    }

    // JPA Callback

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters e Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }

    public String getSourceEvent() { return sourceEvent; }
    public void setSourceEvent(String sourceEvent) { this.sourceEvent = sourceEvent; }

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    @Override
    public String toString() {
        return "Notification{id=" + id + ", userId=" + userId +
                ", type=" + type + ", status=" + status +
                ", title='" + title + "'}";
    }
}
