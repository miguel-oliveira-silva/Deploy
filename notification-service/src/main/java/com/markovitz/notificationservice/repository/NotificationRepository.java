package com.markovitz.notificationservice.repository;

import com.markovitz.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório de notificações.
 *
 * MÉTODOS USADOS:
 *   - save()          → herdado do JpaRepository
 *   - findByUserId()  → gerado automaticamente pelo nome do método
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Lista todas as notificações de um usuário, ordenadas da mais recente para a mais antiga.
     *
     * Spring Data JPA gera:
     *   SELECT * FROM notifications
     *   WHERE user_id = ?
     *   ORDER BY created_at DESC
     *
     * "OrderByCreatedAtDesc":
     *   OrderBy    → ORDER BY
     *   CreatedAt  → coluna created_at
     *   Desc       → decrescente (mais recente primeiro)
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Lista notificações por usuário e status.
     * Útil para buscar notificações PENDENTES para re-envio.
     */
    List<Notification> findByUserIdAndStatus(Long userId, Notification.NotificationStatus status);
}
