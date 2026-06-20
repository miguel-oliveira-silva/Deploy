package com.markovitz.notificationservice.controller;

import com.markovitz.notificationservice.dto.NotificationResponseDTO;
import com.markovitz.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================================
 * NOTIFICATION CONTROLLER — API REST do notification-service
 * ============================================================================
 *
 * ENDPOINTS:
 *
 *   GET /api/notifications/user/{userId}
 *     → Lista todas as notificações de um usuário
 *     → Ordenadas da mais recente para a mais antiga
 *
 * QUANDO USAR:
 *   O front-end pode chamar este endpoint para exibir o "sino de notificações"
 *   ou a central de notificações do usuário.
 *
 * EXEMPLO DE RESPOSTA:
 * GET http://localhost:8084/api/notifications/user/1
 *
 * [
 *   {
 *     "id": 2,
 *     "userId": 1,
 *     "type": "CARTEIRA_OTIMIZADA",
 *     "title": "✅ Carteira 'Minha aposentadoria' otimizada!",
 *     "message": "Sua carteira foi otimizada...\nRetorno: 29.00%...",
 *     "status": "ENVIADA",
 *     "sourceEvent": "portfolio.optimized",
 *     "sourceId": 1,
 *     "createdAt": "2024-01-15T14:30:05"
 *   },
 *   {
 *     "id": 1,
 *     "userId": 1,
 *     "type": "BOAS_VINDAS",
 *     "title": "🎉 Bem-vindo ao Markovitz, João!",
 *     "status": "ENVIADA",
 *     "createdAt": "2024-01-15T14:28:00"
 *   }
 * ]
 *
 * ============================================================================
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notificações", description = "Consulta de notificações geradas por eventos do sistema")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * GET /api/notifications/user/{userId}
     *
     * Retorna todas as notificações de um usuário, ordenadas por data decrescente.
     */
    @Operation(summary = "Listar notificações de um usuário", description = "Retorna notificações ordenadas da mais recente para a mais antiga")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponseDTO>> findByUserId(
            @Parameter(description = "ID do usuário") @PathVariable Long userId) {
        log.debug("GET /api/notifications/user/{}", userId);
        List<NotificationResponseDTO> notifications = notificationService.findByUserId(userId);
        return ResponseEntity.ok(notifications);
    }
}
