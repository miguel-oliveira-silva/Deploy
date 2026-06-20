package com.markovitz.portfolioservice.service;

import com.markovitz.portfolioservice.client.AssetServiceClient;
import com.markovitz.portfolioservice.client.AssetServiceClient.AssetStatsResponse;
import com.markovitz.portfolioservice.config.RabbitMQConfig;
import com.markovitz.portfolioservice.dto.CreatePortfolioRequestDTO;
import com.markovitz.portfolioservice.dto.PortfolioResponseDTO;
import com.markovitz.portfolioservice.entity.Portfolio;
import com.markovitz.portfolioservice.entity.Portfolio.OptimizationGoal;
import com.markovitz.portfolioservice.entity.Portfolio.PortfolioStatus;
import com.markovitz.portfolioservice.entity.PortfolioAsset;
import com.markovitz.portfolioservice.event.AssetPriceUpdatedEvent;
import com.markovitz.portfolioservice.event.PortfolioOptimizedEvent;
import com.markovitz.portfolioservice.exception.PortfolioNotFoundException;
import com.markovitz.portfolioservice.markowitz.MarkowitzOptimizer;
import com.markovitz.portfolioservice.markowitz.MarkowitzOptimizer.AssetData;
import com.markovitz.portfolioservice.markowitz.MarkowitzOptimizer.OptimizationResult;
import com.markovitz.portfolioservice.repository.PortfolioAssetRepository;
import com.markovitz.portfolioservice.repository.PortfolioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ============================================================================
 * PORTFOLIO SERVICE — Orquestrador do Sistema
 * ============================================================================
 *
 * Este Service coordena três atores:
 *
 *   1. CLIENTE REST (AssetServiceClient)
 *      → Busca μ e σ de cada ativo no asset-service
 *
 *   2. OTIMIZADOR (MarkowitzOptimizer)
 *      → Calcula os pesos ótimos usando a Teoria de Markowitz
 *
 *   3. MENSAGERIA (RabbitTemplate + @RabbitListener)
 *      → Consome eventos do asset-service (preço atualizado)
 *      → Publica eventos para o notification-service (portfólio otimizado)
 *
 * FLUXO PRINCIPAL (otimização sob demanda):
 * ─────────────────────────────────────────────────────────────────────────
 *   POST /api/portfolios              → createPortfolio()
 *   POST /api/portfolios/{id}/optimize → optimizePortfolio()
 *     ├─ Busca stats de cada ativo no asset-service (REST síncrono)
 *     ├─ Chama MarkowitzOptimizer para calcular pesos
 *     ├─ Salva pesos e métricas no banco
 *     └─ Publica PortfolioOptimizedEvent no RabbitMQ
 *
 * FLUXO REATIVO (re-otimização automática):
 * ─────────────────────────────────────────────────────────────────────────
 *   RabbitMQ → AssetPriceUpdatedEvent → handleAssetPriceUpdated()
 *     ├─ Encontra todas as carteiras OTIMIZADAS que contêm o ativo
 *     └─ Para cada carteira: chama optimizePortfolio() novamente
 *
 * ============================================================================
 */
@Service
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    private final PortfolioRepository portfolioRepository;
    private final PortfolioAssetRepository portfolioAssetRepository;
    private final AssetServiceClient assetServiceClient;
    private final MarkowitzOptimizer optimizer;
    private final RabbitTemplate rabbitTemplate;

    public PortfolioService(PortfolioRepository portfolioRepository,
                             PortfolioAssetRepository portfolioAssetRepository,
                             AssetServiceClient assetServiceClient,
                             MarkowitzOptimizer optimizer,
                             RabbitTemplate rabbitTemplate) {
        this.portfolioRepository     = portfolioRepository;
        this.portfolioAssetRepository = portfolioAssetRepository;
        this.assetServiceClient      = assetServiceClient;
        this.optimizer               = optimizer;
        this.rabbitTemplate          = rabbitTemplate;
    }

    // =========================================================================
    // OPERAÇÕES CRUD DE CARTEIRA
    // =========================================================================

    /**
     * Cria uma nova carteira no status PENDENTE.
     *
     * A otimização não acontece aqui — o usuário deve chamar
     * POST /api/portfolios/{id}/optimize separadamente.
     * Isso permite que o usuário revise a carteira antes de otimizar.
     *
     * @param requestDTO dados da carteira (userId, nome, tickers, objetivo)
     * @return carteira criada no status PENDENTE
     */
    @Transactional
    public PortfolioResponseDTO createPortfolio(CreatePortfolioRequestDTO requestDTO) {
        log.info("Criando carteira '{}' para o usuário {}", requestDTO.getName(), requestDTO.getUserId());

        // Cria e salva a carteira
        Portfolio portfolio = Portfolio.builder()
                .userId(requestDTO.getUserId())
                .name(requestDTO.getName())
                .status(PortfolioStatus.PENDENTE)
                .optimizationGoal(requestDTO.getOptimizationGoal())
                .build();

        Portfolio saved = portfolioRepository.save(portfolio);

        // Cria os PortfolioAsset inicialmente sem peso (null — ainda não otimizado)
        List<PortfolioAsset> assets = new ArrayList<>();
        for (String ticker : requestDTO.getTickers()) {
            PortfolioAsset pa = PortfolioAsset.builder()
                    .portfolio(saved)
                    .ticker(ticker.toUpperCase())
                    .weight(null)      // será preenchido após otimização
                    .build();
            assets.add(portfolioAssetRepository.save(pa));
        }

        log.info("Carteira criada com ID {} e {} ativos", saved.getId(), assets.size());
        return PortfolioResponseDTO.from(saved, assets);
    }

    /**
     * Busca uma carteira por ID com seus ativos.
     */
    @Transactional(readOnly = true)
    public PortfolioResponseDTO findById(Long id) {
        Portfolio portfolio = getPortfolioById(id);
        List<PortfolioAsset> assets = portfolioAssetRepository.findByPortfolio(portfolio);
        return PortfolioResponseDTO.from(portfolio, assets);
    }

    /**
     * Lista todas as carteiras de um usuário.
     */
    @Transactional(readOnly = true)
    public List<PortfolioResponseDTO> findByUserId(Long userId) {
        return portfolioRepository.findByUserId(userId).stream()
                .map(p -> {
                    List<PortfolioAsset> assets = portfolioAssetRepository.findByPortfolio(p);
                    return PortfolioResponseDTO.from(p, assets);
                })
                .toList();
    }

    // =========================================================================
    // OTIMIZAÇÃO DE MARKOWITZ
    // =========================================================================

    /**
     * Executa o algoritmo de Markowitz para otimizar uma carteira.
     *
     * FLUXO DETALHADO:
     * ─────────────────────────────────────────────────────────────────────
     *
     *  1. Atualiza status → OTIMIZANDO
     *  2. Para cada ativo da carteira:
     *     a. Chama GET /api/assets/{ticker}/stats no asset-service
     *     b. Recebe μ (retorno anualizado) e σ (volatilidade anualizada)
     *  3. Chama MarkowitzOptimizer com os dados dos ativos
     *     a. Se goal = MIN_VARIANCE → minimizeVariance()
     *     b. Se goal = MAX_SHARPE   → maximizeSharpe()
     *  4. Salva os pesos calculados em cada PortfolioAsset
     *  5. Salva métricas da carteira (μₚ, σₚ, Sharpe)
     *  6. Atualiza status → OTIMIZADO
     *  7. Publica PortfolioOptimizedEvent no RabbitMQ
     *
     * @param portfolioId ID da carteira a otimizar
     * @return carteira com pesos calculados e métricas
     */
    @Transactional
    public PortfolioResponseDTO optimizePortfolio(Long portfolioId) {
        log.info("Iniciando otimização da carteira {}", portfolioId);

        Portfolio portfolio = getPortfolioById(portfolioId);

        // =====================================================================
        // ATUALIZAR STATUS → OTIMIZANDO
        // =====================================================================
        portfolio.setStatus(PortfolioStatus.OTIMIZANDO);
        portfolioRepository.save(portfolio);

        List<PortfolioAsset> portfolioAssets = portfolioAssetRepository.findByPortfolio(portfolio);

        if (portfolioAssets.size() < 2) {
            return markPortfolioError(portfolio, portfolioAssets,
                    "A carteira precisa de pelo menos 2 ativos para otimização.");
        }

        try {
            // =================================================================
            // PASSO 1: Buscar estatísticas de cada ativo no asset-service
            // =================================================================
            // Esta é a COMUNICAÇÃO SÍNCRONA entre microsserviços!
            // O portfolio-service chama o asset-service via HTTP REST
            // e aguarda a resposta antes de continuar.
            // =================================================================
            log.info("Buscando estatísticas de {} ativos no asset-service...",
                    portfolioAssets.size());

            List<AssetData> assetDataList = new ArrayList<>();

            for (PortfolioAsset pa : portfolioAssets) {
                // Chamada HTTP GET para o asset-service
                AssetStatsResponse stats = assetServiceClient.getAssetStats(pa.getTicker());

                // Converte a resposta para o formato que o Optimizer precisa
                AssetData assetData = new AssetData(
                        pa.getTicker(),
                        stats.getAnnualizedReturn(),     // μ anualizado
                        stats.getAnnualizedVolatility()  // σ anualizado
                );
                assetDataList.add(assetData);

                // Salva as métricas individuais no PortfolioAsset
                pa.setExpectedReturn(stats.getAnnualizedReturn());
                pa.setRisk(stats.getAnnualizedVolatility());
            }

            // =================================================================
            // PASSO 2: Executar o algoritmo de Markowitz
            // =================================================================
            log.info("Executando algoritmo de Markowitz (goal: {})...",
                    portfolio.getOptimizationGoal());

            OptimizationResult result;

            if (portfolio.getOptimizationGoal() == OptimizationGoal.MIN_VARIANCE) {
                result = optimizer.minimizeVariance(assetDataList);
            } else {
                result = optimizer.maximizeSharpe(assetDataList);
            }

            // =================================================================
            // PASSO 3: Salvar os pesos calculados em cada PortfolioAsset
            // =================================================================
            for (int i = 0; i < portfolioAssets.size(); i++) {
                PortfolioAsset pa = portfolioAssets.get(i);
                pa.setWeight(result.weights()[i]);
                portfolioAssetRepository.save(pa);

                log.info("  {} → peso: {}%",
                        pa.getTicker(),
                        String.format("%.2f", result.weights()[i] * 100));
            }

            // =================================================================
            // PASSO 4: Atualizar métricas da carteira
            // =================================================================
            portfolio.setExpectedReturn(result.portfolioReturn());
            portfolio.setPortfolioRisk(result.portfolioRisk());
            portfolio.setSharpeRatio(result.sharpeRatio());
            portfolio.setStatus(PortfolioStatus.OTIMIZADO);
            portfolio.setOptimizedAt(LocalDateTime.now());
            portfolio.setErrorMessage(null);
            portfolioRepository.save(portfolio);

            log.info("Carteira {} otimizada! Retorno: {}%, Risco: {}%, Sharpe: {}",
                    portfolioId,
                    String.format("%.2f", result.portfolioReturn() * 100),
                    String.format("%.2f", result.portfolioRisk() * 100),
                    String.format("%.3f", result.sharpeRatio()));

            // =================================================================
            // PASSO 5: Publicar evento de otimização concluída no RabbitMQ
            // =================================================================
            publishPortfolioOptimizedEvent(portfolio, portfolioAssets);

            return PortfolioResponseDTO.from(portfolio, portfolioAssets);

        } catch (Exception e) {
            // Algo deu errado → marca a carteira com erro
            log.error("Erro ao otimizar carteira {}: {}", portfolioId, e.getMessage());
            return markPortfolioError(portfolio, portfolioAssets, e.getMessage());
        }
    }

    // =========================================================================
    // CONSUMIDOR DE EVENTOS — @RabbitListener
    // =========================================================================

    /**
     * CONSOME o evento "asset.price.updated" do RabbitMQ.
     *
     * COMO @RabbitListener FUNCIONA:
     * ─────────────────────────────────────────────────────────────────────
     * O Spring AMQP mantém uma thread ouvindo a fila em background.
     * Quando uma mensagem chega:
     *   1. O Spring deserializa o JSON → AssetPriceUpdatedEvent
     *   2. Chama este método com o objeto deserializado
     *   3. Se o método lançar exceção → mensagem é rejeitada (pode re-enfileirar)
     *   4. Se concluir com sucesso → mensagem é confirmada (acknowledge)
     *
     * Este método é chamado AUTOMATICAMENTE — não precisa de polling!
     * Isso é a beleza da comunicação assíncrona por mensageria.
     *
     * RE-OTIMIZAÇÃO AUTOMÁTICA:
     * Quando o asset-service adiciona novos preços a um ativo,
     * as estatísticas mudam. Carteiras que contêm esse ativo devem
     * ser re-otimizadas com os dados mais recentes.
     *
     * @param event evento recebido do asset-service
     */
    @RabbitListener(queues = RabbitMQConfig.ASSET_PRICE_UPDATED_QUEUE)
    public void handleAssetPriceUpdated(AssetPriceUpdatedEvent event) {
        log.info("📨 Evento recebido: asset.price.updated para ticker '{}'", event.getTicker());

        // Encontra todas as carteiras OTIMIZADAS que contêm este ativo
        List<PortfolioAsset> affectedAssets = portfolioAssetRepository
                .findByTickerAndPortfolio_Status(event.getTicker(), PortfolioStatus.OTIMIZADO);

        if (affectedAssets.isEmpty()) {
            log.debug("Nenhuma carteira otimizada contém o ativo '{}'. Nada a re-otimizar.",
                    event.getTicker());
            return;
        }

        // Coleta IDs únicos das carteiras afetadas
        Set<Long> affectedPortfolioIds = new HashSet<>();
        for (PortfolioAsset pa : affectedAssets) {
            affectedPortfolioIds.add(pa.getPortfolio().getId());
        }

        log.info("Re-otimizando {} carteiras afetadas pelo ticker '{}'...",
                affectedPortfolioIds.size(), event.getTicker());

        // Re-otimiza cada carteira afetada
        for (Long portfolioId : affectedPortfolioIds) {
            try {
                optimizePortfolio(portfolioId);
                log.info("Carteira {} re-otimizada com sucesso.", portfolioId);
            } catch (Exception e) {
                log.error("Falha ao re-otimizar carteira {}: {}", portfolioId, e.getMessage());
                // Continua para as próximas carteiras — uma falha não para as outras
            }
        }
    }

    // =========================================================================
    // MÉTODOS PRIVADOS
    // =========================================================================

    private Portfolio getPortfolioById(Long id) {
        return portfolioRepository.findById(id)
                .orElseThrow(() -> new PortfolioNotFoundException(
                        "Carteira com ID " + id + " não encontrada"));
    }

    /**
     * Marca a carteira como ERRO e salva a mensagem de erro.
     */
    private PortfolioResponseDTO markPortfolioError(Portfolio portfolio,
                                                      List<PortfolioAsset> assets,
                                                      String errorMessage) {
        portfolio.setStatus(PortfolioStatus.ERRO);
        portfolio.setErrorMessage(errorMessage);
        portfolioRepository.save(portfolio);
        return PortfolioResponseDTO.from(portfolio, assets);
    }

    /**
     * Publica o evento de portfólio otimizado no RabbitMQ.
     * O notification-service consume este evento e notifica o usuário.
     */
    private void publishPortfolioOptimizedEvent(Portfolio portfolio,
                                                 List<PortfolioAsset> assets) {
        try {
            // Monta o mapa ticker → peso para a notificação
            Map<String, Double> weights = new LinkedHashMap<>();
            for (PortfolioAsset pa : assets) {
                weights.put(pa.getTicker(), pa.getWeight());
            }

            PortfolioOptimizedEvent event = new PortfolioOptimizedEvent(
                    portfolio.getId(),
                    portfolio.getName(),
                    portfolio.getUserId(),
                    portfolio.getOptimizationGoal().name(),
                    portfolio.getExpectedReturn(),
                    portfolio.getPortfolioRisk(),
                    portfolio.getSharpeRatio(),
                    weights
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.PORTFOLIO_OPTIMIZED_ROUTING_KEY,
                    event
            );

            log.info("✉ Evento 'portfolio.optimized' publicado para carteira ID: {}",
                    portfolio.getId());

        } catch (Exception e) {
            log.error("Falha ao publicar evento de portfólio otimizado: {}", e.getMessage());
        }
    }
}
