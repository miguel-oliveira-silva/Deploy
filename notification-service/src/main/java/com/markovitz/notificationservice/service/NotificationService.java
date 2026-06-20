package com.markovitz.notificationservice.service;

import com.markovitz.notificationservice.config.RabbitMQConfig;
import com.markovitz.notificationservice.dto.NotificationResponseDTO;
import com.markovitz.notificationservice.entity.Notification;
import com.markovitz.notificationservice.entity.Notification.NotificationStatus;
import com.markovitz.notificationservice.entity.Notification.NotificationType;
import com.markovitz.notificationservice.event.PortfolioOptimizedEvent;
import com.markovitz.notificationservice.event.UserRegisteredEvent;
import com.markovitz.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * NOTIFICATION SERVICE — Consumidor de Eventos e Gerador de Notificações
 * ============================================================================
 *
 * DOIS CONSUMIDORES DE EVENTOS:
 * ─────────────────────────────────────────────────────────────────────────
 *
 *   handleUserRegistered()    → consome "user.registered.queue"
 *   handlePortfolioOptimized() → consome "portfolio.optimized.queue"
 *
 * COMO @RabbitListener FUNCIONA (revisão aprofundada):
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Quando o Spring Boot inicia, o Spring AMQP:
 *   1. Cria uma thread ouvinte para cada @RabbitListener
 *   2. A thread fica em loop aguardando mensagens na fila
 *   3. Quando uma mensagem chega:
 *      a. Jackson deserializa JSON → objeto Java
 *      b. Spring chama o método anotado com o objeto
 *      c. Se retornar normalmente → ACK (mensagem confirmada e removida da fila)
 *      d. Se lançar exceção → NACK (mensagem pode ser re-enfileirada)
 *
 * PROCESSAMENTO PARALELO:
 * O Spring AMQP pode processar múltiplas mensagens em paralelo usando
 * um pool de threads. Configurado em application.yml:
 *   spring.rabbitmq.listener.simple.concurrency: 2
 *   spring.rabbitmq.listener.simple.max-concurrency: 5
 *
 * ============================================================================
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // =========================================================================
    // CONSUMIDOR 1: Evento de Usuário Cadastrado
    // =========================================================================

    /**
     * Processa o evento de usuário registrado e cria uma notificação de boas-vindas.
     *
     * @RabbitListener:
     * ─────────────────────────────────────────────────────────────────────
     * queues = RabbitMQConfig.USER_REGISTERED_QUEUE
     *   → Nome da fila a escutar: "user.registered.queue"
     *
     * O Spring AMQP:
     *   1. Recebe a mensagem JSON da fila
     *   2. Usa Jackson2JsonMessageConverter (configurado em RabbitMQConfig)
     *      para converter o JSON em UserRegisteredEvent
     *   3. Chama este método com o objeto convertido
     *
     * @param event evento de usuário cadastrado, deserializado automaticamente do JSON
     */
    @RabbitListener(queues = RabbitMQConfig.USER_REGISTERED_QUEUE)
    @Transactional
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("📨 [user.registered] Recebido para usuário: {} ({})",
                event.getUserName(), event.getUserEmail());

        try {
            // ================================================================
            // COMPOSIÇÃO DA MENSAGEM DE BOAS-VINDAS
            // ================================================================
            String title = "🎉 Bem-vindo ao Markovitz, " + event.getUserName() + "!";

            String message = String.join("\n",
                    "Olá, " + event.getUserName() + "! Seu cadastro foi realizado com sucesso.",
                    "",
                    "O sistema Markovitz usa a Teoria Moderna do Portfólio de Harry Markowitz",
                    "para ajudá-lo a construir carteiras de investimento otimizadas.",
                    "",
                    "Próximos passos:",
                    "1. Explore os ativos disponíveis em GET /api/assets",
                    "2. Crie sua primeira carteira em POST /api/portfolios",
                    "3. Execute a otimização em POST /api/portfolios/{id}/optimize",
                    "",
                    "Bons investimentos! 📈"
            );

            // ================================================================
            // CRIAÇÃO E PERSISTÊNCIA DA NOTIFICAÇÃO
            // ================================================================
            Notification notification = Notification.builder()
                    .userId(event.getUserId())
                    .type(NotificationType.BOAS_VINDAS)
                    .title(title)
                    .message(message)
                    .status(NotificationStatus.PENDENTE)
                    .sourceEvent("user.registered")
                    .sourceId(event.getUserId())
                    .build();

            notification = notificationRepository.save(notification);

            // ================================================================
            // SIMULAÇÃO DE ENVIO
            // ================================================================
            // Em produção, aqui você chamaria um serviço de email:
            //   emailService.send(event.getUserEmail(), title, message);
            // Ou um serviço de push notifications:
            //   pushService.send(event.getUserId(), title, message);
            //
            // Para este projeto, logamos no console para demonstrar que funciona.
            // ================================================================

            log.info("╔══════════════════════════════════════════════════════════════╗");
            log.info("║  📧 NOTIFICAÇÃO DE BOAS-VINDAS                               ║");
            log.info("╠══════════════════════════════════════════════════════════════╣");
            log.info("║  Para:    {} ({})           ", event.getUserName(), event.getUserEmail());
            log.info("║  Título:  {}                ", title);
            log.info("╚══════════════════════════════════════════════════════════════╝");

            // Marca como ENVIADA e registra o horário
            notification.setStatus(NotificationStatus.ENVIADA);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("✅ Notificação de boas-vindas enviada ao usuário {}", event.getUserId());

        } catch (Exception e) {
            log.error("❌ Erro ao processar notificação de boas-vindas para usuário {}: {}",
                    event.getUserId(), e.getMessage());
            // Salva com status FALHA para possível re-tentativa futura
            Notification failed = Notification.builder()
                    .userId(event.getUserId())
                    .type(NotificationType.BOAS_VINDAS)
                    .title("Boas-vindas (falha)")
                    .message("Erro: " + e.getMessage())
                    .status(NotificationStatus.FALHA)
                    .sourceEvent("user.registered")
                    .sourceId(event.getUserId())
                    .build();
            notificationRepository.save(failed);
        }
    }

    // =========================================================================
    // CONSUMIDOR 2: Evento de Carteira Otimizada
    // =========================================================================

    /**
     * Processa o evento de portfólio otimizado e cria uma notificação detalhada.
     *
     * Este método é o mais rico em conteúdo — ele formata os resultados
     * da otimização de Markowitz de forma legível para o usuário.
     *
     * @param event evento de portfólio otimizado, deserializado do JSON
     */
    @RabbitListener(queues = RabbitMQConfig.PORTFOLIO_OPTIMIZED_QUEUE)
    @Transactional
    public void handlePortfolioOptimized(PortfolioOptimizedEvent event) {
        log.info("📨 [portfolio.optimized] Recebido para carteira: '{}' (ID: {})",
                event.getPortfolioName(), event.getPortfolioId());

        try {
            // ================================================================
            // FORMATAÇÃO DOS RESULTADOS DA OTIMIZAÇÃO
            // ================================================================
            String goalDesc = "MAX_SHARPE".equals(event.getOptimizationGoal())
                    ? "Máximo Índice de Sharpe (melhor retorno/risco)"
                    : "Mínima Variância (menor risco possível)";

            String title = "✅ Carteira '" + event.getPortfolioName() + "' otimizada!";

            // Formata os pesos de cada ativo
            String weightsFormatted = formatWeights(event.getAssetWeights());

            String message = String.join("\n",
                    "Sua carteira '" + event.getPortfolioName() + "' foi otimizada com sucesso!",
                    "",
                    "═══ RESULTADO DA OTIMIZAÇÃO ═══",
                    "Objetivo:           " + goalDesc,
                    "Retorno esperado:   " + String.format("%.2f%%", safeDouble(event.getExpectedReturn()) * 100) + " ao ano",
                    "Risco (volatil.):   " + String.format("%.2f%%", safeDouble(event.getPortfolioRisk()) * 100) + " ao ano",
                    "Índice de Sharpe:   " + String.format("%.3f", safeDouble(event.getSharpeRatio())),
                    "",
                    "═══ ALOCAÇÃO SUGERIDA ═══",
                    weightsFormatted,
                    "",
                    "* Resultados baseados em dados históricos. Rentabilidade passada",
                    "  não garante rentabilidade futura."
            );

            // ================================================================
            // SALVA E ENVIA A NOTIFICAÇÃO
            // ================================================================
            Notification notification = Notification.builder()
                    .userId(event.getUserId())
                    .type(NotificationType.CARTEIRA_OTIMIZADA)
                    .title(title)
                    .message(message)
                    .status(NotificationStatus.PENDENTE)
                    .sourceEvent("portfolio.optimized")
                    .sourceId(event.getPortfolioId())
                    .build();

            notification = notificationRepository.save(notification);

            // Log formatado simulando o envio da notificação
            log.info("╔══════════════════════════════════════════════════════════════╗");
            log.info("║  📊 NOTIFICAÇÃO DE OTIMIZAÇÃO                                ║");
            log.info("╠══════════════════════════════════════════════════════════════╣");
            log.info("║  Carteira: '{}' (ID: {})          ", event.getPortfolioName(), event.getPortfolioId());
            log.info("║  Usuário:  ID {}                                              ", event.getUserId());
            log.info("║  Retorno:  {}%  |  Risco: {}%  |  Sharpe: {}",
                    String.format("%.2f", safeDouble(event.getExpectedReturn()) * 100),
                    String.format("%.2f", safeDouble(event.getPortfolioRisk()) * 100),
                    String.format("%.3f", safeDouble(event.getSharpeRatio())));
            log.info("║  Alocação: {}                                                 ", weightsFormatted);
            log.info("╚══════════════════════════════════════════════════════════════╝");

            notification.setStatus(NotificationStatus.ENVIADA);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("✅ Notificação de otimização enviada ao usuário {}", event.getUserId());

        } catch (Exception e) {
            log.error("❌ Erro ao processar notificação de otimização para carteira {}: {}",
                    event.getPortfolioId(), e.getMessage());

            Notification failed = Notification.builder()
                    .userId(event.getUserId())
                    .type(NotificationType.CARTEIRA_OTIMIZADA)
                    .title("Otimização concluída (falha na notificação)")
                    .message("Erro: " + e.getMessage())
                    .status(NotificationStatus.FALHA)
                    .sourceEvent("portfolio.optimized")
                    .sourceId(event.getPortfolioId())
                    .build();
            notificationRepository.save(failed);
        }
    }

    // =========================================================================
    // CONSULTAS REST
    // =========================================================================

    /**
     * Lista todas as notificações de um usuário, da mais recente para a mais antiga.
     *
     * @param userId ID do usuário
     * @return lista de DTOs de notificações
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> findByUserId(Long userId) {
        log.debug("Buscando notificações do usuário {}", userId);
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponseDTO::from)
                .toList();
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    /**
     * Formata o mapa de pesos em texto legível.
     * Ex: { "PETR4": 0.30, "VALE3": 0.25 } → "PETR4: 30.00% | VALE3: 25.00%"
     */
    private String formatWeights(Map<String, Double> weights) {
        if (weights == null || weights.isEmpty()) return "(sem dados)";

        return weights.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()) // do maior peso para o menor
                .map(e -> String.format("%s: %.1f%%", e.getKey(), e.getValue() * 100))
                .collect(Collectors.joining(" | "));
    }

    /** Retorna 0.0 se o valor for null (evita NullPointerException no formatador) */
    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }
}
