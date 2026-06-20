package com.markovitz.assetservice.service;

import com.markovitz.assetservice.config.RabbitMQConfig;
import com.markovitz.assetservice.dto.*;
import com.markovitz.assetservice.entity.Asset;
import com.markovitz.assetservice.entity.AssetPrice;
import com.markovitz.assetservice.event.AssetPriceUpdatedEvent;
import com.markovitz.assetservice.exception.AssetNotFoundException;
import com.markovitz.assetservice.exception.TickerAlreadyExistsException;
import com.markovitz.assetservice.repository.AssetPriceRepository;
import com.markovitz.assetservice.repository.AssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================================
 * ASSET SERVICE — Lógica de Negócio e Cálculos Financeiros
 * ============================================================================
 *
 * Este é o serviço mais matematicamente rico do sistema.
 * Além do CRUD de ativos e preços, implementa os cálculos estatísticos
 * que são os INPUTS do algoritmo de Markowitz no portfolio-service.
 *
 * CÁLCULOS IMPLEMENTADOS:
 * ─────────────────────────────────────────────────────────────────────────
 *
 *   1. Retornos diários: rₜ = (Pₜ - Pₜ₋₁) / Pₜ₋₁
 *   2. Retorno médio:    μ  = Σrₜ / n
 *   3. Variância:        σ² = Σ(rₜ - μ)² / (n-1)
 *   4. Volatilidade:     σ  = √σ²
 *   5. Anualização:      σ_anual = σ_diária × √252
 *
 * Veja os comentários inline para explicação passo a passo.
 *
 * ============================================================================
 */
@Service
public class AssetService {

    private static final Logger log = LoggerFactory.getLogger(AssetService.class);

    /**
     * Número de dias de pregão em um ano na B3 (Bolsa brasileira).
     * Usado para anualizar as métricas diárias.
     * Ex: volatilidade_anual = volatilidade_diaria × √252
     */
    private static final int TRADING_DAYS_PER_YEAR = 252;

    private final AssetRepository assetRepository;
    private final AssetPriceRepository assetPriceRepository;
    private final RabbitTemplate rabbitTemplate;

    public AssetService(AssetRepository assetRepository,
                        AssetPriceRepository assetPriceRepository,
                        RabbitTemplate rabbitTemplate) {
        this.assetRepository = assetRepository;
        this.assetPriceRepository = assetPriceRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    // =========================================================================
    // OPERAÇÕES DE ATIVO (CRUD)
    // =========================================================================

    /**
     * Cadastra um novo ativo financeiro no sistema.
     *
     * O ticker é convertido para MAIÚSCULAS antes de salvar.
     * Ex: "petr4" → "PETR4"
     *
     * @param requestDTO dados do ativo
     * @return dados do ativo criado com quantidade de preços (0 inicialmente)
     */
    @Transactional
    public AssetResponseDTO createAsset(AssetRequestDTO requestDTO) {
        // Normaliza o ticker: sempre maiúsculo e sem espaços
        String ticker = requestDTO.getTicker().toUpperCase().trim();

        log.info("Cadastrando novo ativo: {}", ticker);

        // Verifica duplicata
        if (assetRepository.existsByTicker(ticker)) {
            throw new TickerAlreadyExistsException(
                    "O ativo com ticker '" + ticker + "' já está cadastrado");
        }

        Asset asset = Asset.builder()
                .ticker(ticker)
                .name(requestDTO.getName())
                .sector(requestDTO.getSector())
                .build();

        Asset saved = assetRepository.save(asset);
        log.info("Ativo '{}' cadastrado com ID: {}", ticker, saved.getId());

        return AssetResponseDTO.from(saved, 0);
    }

    /**
     * Busca um ativo pelo ticker e retorna com contagem de preços.
     */
    @Transactional(readOnly = true)
    public AssetResponseDTO findByTicker(String ticker) {
        Asset asset = getAssetByTicker(ticker.toUpperCase());
        long count = assetPriceRepository.countByAsset(asset);
        return AssetResponseDTO.from(asset, count);
    }

    /**
     * Lista todos os ativos cadastrados.
     */
    @Transactional(readOnly = true)
    public List<AssetResponseDTO> findAll() {
        return assetRepository.findAll()
                .stream()
                .map(asset -> {
                    long count = assetPriceRepository.countByAsset(asset);
                    return AssetResponseDTO.from(asset, count);
                })
                .toList();
    }

    // =========================================================================
    // OPERAÇÕES DE PREÇO
    // =========================================================================

    /**
     * Adiciona um preço histórico a um ativo e publica evento no RabbitMQ.
     *
     * FLUXO:
     * 1. Valida que o ativo existe
     * 2. Cria e salva o AssetPrice
     * 3. Publica evento assíncrono "asset.price.updated"
     * 4. Retorna confirmação
     *
     * @param ticker     ticker do ativo (ex: "PETR4")
     * @param requestDTO preço e data
     * @return DTO de resposta do ativo atualizado
     */
    @Transactional
    public AssetResponseDTO addPrice(String ticker, PriceRequestDTO requestDTO) {
        String normalizedTicker = ticker.toUpperCase();
        Asset asset = getAssetByTicker(normalizedTicker);

        log.info("Adicionando preço {} para {} em {}",
                requestDTO.getPrice(), normalizedTicker, requestDTO.getPriceDate());

        // Cria a entidade AssetPrice vinculada ao ativo
        AssetPrice price = AssetPrice.builder()
                .asset(asset)
                .price(requestDTO.getPrice())
                .priceDate(requestDTO.getPriceDate())
                .build();

        assetPriceRepository.save(price);

        long totalPrices = assetPriceRepository.countByAsset(asset);
        log.info("Preço salvo. Total de preços para {}: {}", normalizedTicker, totalPrices);

        // Publica evento assíncrono no RabbitMQ
        publishPriceUpdatedEvent(asset, requestDTO.getPrice(), requestDTO.getPriceDate());

        return AssetResponseDTO.from(asset, totalPrices);
    }

    // =========================================================================
    // CÁLCULO DE ESTATÍSTICAS — A PARTE MATEMÁTICA DO MARKOWITZ
    // =========================================================================

    /**
     * Calcula as estatísticas financeiras de um ativo.
     *
     * Este método é o núcleo matemático do asset-service.
     * Ele calcula μ e σ — as duas métricas fundamentais de Markowitz.
     *
     * REQUISITO MÍNIMO:
     * Para calcular estatísticas significativas, precisamos de pelo menos
     * 2 preços (para ter pelo menos 1 retorno).
     * Em produção, exigiríamos 30+ pontos para confiabilidade estatística.
     *
     * @param ticker ticker do ativo
     * @return estatísticas com retorno médio, volatilidade e valores anualizados
     */
    @Transactional(readOnly = true)
    public AssetStatsDTO calculateStats(String ticker) {
        Asset asset = getAssetByTicker(ticker.toUpperCase());

        // Busca preços em ORDEM CRONOLÓGICA CRESCENTE (essencial para calcular retornos)
        List<AssetPrice> prices = assetPriceRepository.findByAssetOrderByPriceDateAsc(asset);

        // Validação: precisamos de pelo menos 2 preços para calcular 1 retorno
        if (prices.size() < 2) {
            throw new IllegalArgumentException(
                    "O ativo '" + ticker + "' precisa de pelo menos 2 preços históricos " +
                    "para calcular estatísticas. Preços disponíveis: " + prices.size()
            );
        }

        log.info("Calculando estatísticas para {} com {} preços", ticker, prices.size());

        // =====================================================================
        // PASSO 1: Calcular os retornos diários
        // =====================================================================
        // Um retorno diário = quanto o preço mudou percentualmente de um dia para o próximo
        // rₜ = (Pₜ - Pₜ₋₁) / Pₜ₋₁

        double[] returns = calculateDailyReturns(prices);
        // returns agora tem (N-1) elementos para N preços
        // Ex: 5 preços → 4 retornos

        // =====================================================================
        // PASSO 2: Calcular o Retorno Médio (μ)
        // =====================================================================
        double meanReturn = calculateMean(returns);

        // =====================================================================
        // PASSO 3: Calcular a Volatilidade (σ = desvio padrão)
        // =====================================================================
        double volatility = calculateStandardDeviation(returns, meanReturn);

        // =====================================================================
        // PASSO 4: Anualizar as métricas
        // =====================================================================
        // Convertemos de base diária para base anual para comparação intuitiva
        //
        // Retorno anual: simplesmente multiplica por 252 (dias de pregão)
        //   μ_anual = μ_diário × 252
        //
        // Volatilidade anual: multiplica pela raiz quadrada de 252
        //   σ_anual = σ_diário × √252
        //   (porque a variância é aditiva, a raiz quadrada cancela)
        double annualizedReturn     = meanReturn * TRADING_DAYS_PER_YEAR;
        double annualizedVolatility = volatility * Math.sqrt(TRADING_DAYS_PER_YEAR);

        log.info("Estatísticas de {}: retorno_diario={}, volatilidade_diaria={}, retorno_anual={}, volatilidade_anual={}",
                ticker,
                String.format("%.4f", meanReturn),
                String.format("%.4f", volatility),
                String.format("%.4f", annualizedReturn),
                String.format("%.4f", annualizedVolatility));

        return new AssetStatsDTO(
                asset.getTicker(),
                asset.getName(),
                prices.size(),
                meanReturn,
                volatility,
                annualizedVolatility,
                annualizedReturn
        );
    }

    // =========================================================================
    // MÉTODOS PRIVADOS — Cálculos matemáticos
    // =========================================================================

    /**
     * Calcula os retornos diários a partir de uma lista de preços ordenados.
     *
     * FÓRMULA: rₜ = (Pₜ - Pₜ₋₁) / Pₜ₋₁
     *
     * Exemplo:
     *   Preços:  [36.50, 37.20, 36.80, 37.90]
     *   Retornos: [+1.92%, -1.08%, +2.99%]
     *
     * @param prices lista de AssetPrice em ordem CRONOLÓGICA
     * @return array de retornos diários (tamanho = preços - 1)
     */
    private double[] calculateDailyReturns(List<AssetPrice> prices) {
        // O array de retornos tem 1 elemento a menos que o array de preços
        // (não existe retorno para o primeiro dia, pois não há dia anterior)
        double[] returns = new double[prices.size() - 1];

        for (int i = 1; i < prices.size(); i++) {
            // Preço atual (Pₜ)
            BigDecimal currentPrice = prices.get(i).getPrice();

            // Preço anterior (Pₜ₋₁)
            BigDecimal previousPrice = prices.get(i - 1).getPrice();

            // Retorno: rₜ = (Pₜ - Pₜ₋₁) / Pₜ₋₁
            // Convertemos BigDecimal → double para fazer a aritmética
            double current  = currentPrice.doubleValue();
            double previous = previousPrice.doubleValue();

            returns[i - 1] = (current - previous) / previous;
        }

        return returns;
    }

    /**
     * Calcula a média aritmética de um array de valores.
     *
     * FÓRMULA: μ = (r₁ + r₂ + ... + rₙ) / n
     *
     * @param values array de valores
     * @return média aritmética
     */
    private double calculateMean(double[] values) {
        double sum = 0.0;

        // Soma todos os valores
        for (double value : values) {
            sum += value;
        }

        // Divide pelo número de valores para obter a média
        return sum / values.length;
    }

    /**
     * Calcula o desvio padrão amostral de um array de retornos.
     *
     * FÓRMULA:
     *   Variância amostral: σ² = Σ(rₜ - μ)² / (n - 1)
     *   Desvio padrão:      σ  = √σ²
     *
     * POR QUE (n-1) E NÃO n?
     * ─────────────────────────────────────────────────────────────────────
     * Quando calculamos estatísticas de uma AMOSTRA (não da população toda),
     * dividir por (n-1) ao invés de n corrige um viés estatístico.
     * Isso é chamado de "Correção de Bessel".
     *
     * Intuição: com n=1, não podemos calcular variabilidade (não há comparação).
     * Com (n-1), quando n=1, teríamos divisão por zero — indicando que
     * precisamos de mais dados. Correto!
     *
     * Em finanças, sempre usamos desvio padrão AMOSTRAL pois trabalhamos
     * com uma amostra do histórico, não com o histórico completo do ativo.
     *
     * @param returns  array de retornos diários
     * @param mean     média já calculada (para evitar recalcular)
     * @return desvio padrão amostral
     */
    private double calculateStandardDeviation(double[] returns, double mean) {
        double sumSquaredDeviations = 0.0;

        for (double r : returns) {
            // Desvio de cada retorno em relação à média: (rₜ - μ)
            double deviation = r - mean;

            // Eleva ao quadrado: (rₜ - μ)²
            // Por que quadrado? Porque desvios positivos e negativos se cancelariam
            // se simplesmente somássemos. O quadrado sempre é positivo.
            sumSquaredDeviations += deviation * deviation;
        }

        // Variância amostral: divide por (n-1), não por n (Correção de Bessel)
        double variance = sumSquaredDeviations / (returns.length - 1);

        // Desvio padrão = raiz quadrada da variância
        // Retorna à mesma unidade dos retornos (de % ao quadrado → %)
        return Math.sqrt(variance);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    /**
     * Busca um ativo pelo ticker ou lança exceção se não encontrar.
     * Método auxiliar reutilizado em vários métodos do Service.
     */
    private Asset getAssetByTicker(String ticker) {
        return assetRepository.findByTicker(ticker)
                .orElseThrow(() -> new AssetNotFoundException(
                        "Ativo com ticker '" + ticker + "' não encontrado"));
    }

    /**
     * Publica o evento de preço atualizado no RabbitMQ.
     * "Fire and forget" — não espera confirmação do consumidor.
     */
    private void publishPriceUpdatedEvent(Asset asset, BigDecimal price,
                                           java.time.LocalDate priceDate) {
        try {
            AssetPriceUpdatedEvent event = new AssetPriceUpdatedEvent(
                    asset.getTicker(),
                    asset.getName(),
                    price,
                    priceDate,
                    LocalDateTime.now()
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ASSET_PRICE_UPDATED_ROUTING_KEY,
                    event
            );

            log.info("✉ Evento 'asset.price.updated' publicado para o ticker: {}",
                    asset.getTicker());

        } catch (Exception e) {
            // Não deixa o cadastro do preço falhar por causa do RabbitMQ
            log.error("Falha ao publicar evento de preço para {}: {}",
                    asset.getTicker(), e.getMessage());
        }
    }
}
