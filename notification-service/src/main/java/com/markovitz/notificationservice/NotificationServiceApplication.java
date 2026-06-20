package com.markovitz.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================================
 * NOTIFICATION SERVICE — Fechando o Ciclo de Comunicação Assíncrona
 * ============================================================================
 *
 * Este é o microsserviço mais "passivo" do sistema — ele apenas escuta
 * e reage a eventos publicados pelos outros serviços.
 *
 * RESPONSABILIDADES:
 * ─────────────────────────────────────────────────────────────────────────
 *
 *   1. CONSUMIR EVENTOS DO RABBITMQ (sem publicar nada)
 *      a) "user.registered"    → boas-vindas ao novo usuário
 *      b) "portfolio.optimized" → informar resultado da otimização
 *
 *   2. PERSISTIR NOTIFICAÇÕES
 *      → Cada evento gera uma notificação salva no banco
 *      → Status: PENDENTE → ENVIADA (ou FALHA)
 *
 *   3. EXPOR ENDPOINT REST
 *      → GET /api/notifications/user/{userId}
 *      → Permite que o front-end consulte as notificações do usuário
 *
 * O FLUXO COMPLETO DO SISTEMA (de ponta a ponta):
 * ─────────────────────────────────────────────────────────────────────────
 *
 *  [1] user-service    → POST /api/users/register
 *                     → publica "user.registered" no RabbitMQ
 *
 *  [2] notification-service ← consome "user.registered"
 *                           → salva notificação de boas-vindas no banco
 *
 *  [3] asset-service   → POST /api/assets/PETR4/prices
 *                     → publica "asset.price.updated" no RabbitMQ
 *
 *  [4] portfolio-service ← consome "asset.price.updated"
 *                        → re-otimiza carteiras com PETR4
 *                        → publica "portfolio.optimized" no RabbitMQ
 *
 *  [5] notification-service ← consome "portfolio.optimized"
 *                           → salva notificação com resultado da otimização
 *
 *  [6] Front-end → GET /api/notifications/user/1
 *               ← lista de notificações do usuário
 *
 * ============================================================================
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
