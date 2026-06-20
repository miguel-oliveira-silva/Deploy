package com.markovitz.notificationservice.dto;

import com.markovitz.notificationservice.entity.Notification;
import java.time.LocalDateTime;

/**
 * DTO de resposta com os dados de uma notificação.
 *
 * EXEMPLO DE JSON RETORNADO:
 * {
 *   "id": 1,
 *   "userId": 1,
 *   "type": "CARTEIRA_OTIMIZADA",
 *   "title": "✅ Carteira otimizada com sucesso!",
 *   "message": "Sua carteira 'Minha aposentadoria' foi otimizada...",
 *   "status": "ENVIADA",
 *   "sourceEvent": "portfolio.optimized",
 *   "sourceId": 1,
 *   "createdAt": "2024-01-15T14:30:05",
 *   "sentAt": "2024-01-15T14:30:05"
 * }
 */
public class NotificationResponseDTO {

    private Long id;
    private Long userId;
    private Notification.NotificationType type;
    private String title;
    private String message;
    private Notification.NotificationStatus status;
    private String sourceEvent;
    private Long sourceId;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;

    public NotificationResponseDTO() {}

    /** Factory method: converte entidade → DTO */
    public static NotificationResponseDTO from(Notification n) {
        NotificationResponseDTO dto = new NotificationResponseDTO();
        dto.setId(n.getId());
        dto.setUserId(n.getUserId());
        dto.setType(n.getType());
        dto.setTitle(n.getTitle());
        dto.setMessage(n.getMessage());
        dto.setStatus(n.getStatus());
        dto.setSourceEvent(n.getSourceEvent());
        dto.setSourceId(n.getSourceId());
        dto.setCreatedAt(n.getCreatedAt());
        dto.setSentAt(n.getSentAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Notification.NotificationType getType() { return type; }
    public void setType(Notification.NotificationType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Notification.NotificationStatus getStatus() { return status; }
    public void setStatus(Notification.NotificationStatus status) { this.status = status; }

    public String getSourceEvent() { return sourceEvent; }
    public void setSourceEvent(String sourceEvent) { this.sourceEvent = sourceEvent; }

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
