package com.markovitz.assetservice.controller;

import com.markovitz.assetservice.dto.*;
import com.markovitz.assetservice.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================================
 * ASSET CONTROLLER — API REST do asset-service
 * ============================================================================
 *
 * ENDPOINTS DISPONÍVEIS:
 *
 *   POST   /api/assets                   → cadastrar novo ativo
 *   GET    /api/assets                   → listar todos os ativos
 *   GET    /api/assets/{ticker}          → buscar ativo por ticker
 *   POST   /api/assets/{ticker}/prices   → adicionar preço histórico
 *   GET    /api/assets/{ticker}/stats    → calcular estatísticas (μ e σ)
 *
 * O endpoint de STATS é o mais importante para o portfólio-service!
 * Ele retorna os inputs necessários para o algoritmo de Markowitz.
 *
 * ============================================================================
 */
@RestController
@RequestMapping("/api/assets")
@Tag(name = "Ativos", description = "Cadastro de ativos financeiros e histórico de preços")
public class AssetController {

    private static final Logger log = LoggerFactory.getLogger(AssetController.class);

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    // =========================================================================
    // ENDPOINT 1: Cadastrar novo ativo
    // =========================================================================

    /**
     * POST /api/assets
     *
     * EXEMPLO DE REQUISIÇÃO:
     * POST http://localhost:8082/api/assets
     * {
     *   "ticker": "PETR4",
     *   "name": "Petrobras S.A. PN",
     *   "sector": "Energia"
     * }
     *
     * RESPOSTA (201 Created):
     * {
     *   "id": 1,
     *   "ticker": "PETR4",
     *   "name": "Petrobras S.A. PN",
     *   "sector": "Energia",
     *   "priceCount": 0,
     *   "createdAt": "2024-01-15T14:30:00"
     * }
     */
    @Operation(summary = "Cadastrar novo ativo", description = "Registra um ativo por ticker (ex: PETR4, VALE3)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Ativo criado com sucesso"),
        @ApiResponse(responseCode = "409", description = "Ticker já cadastrado")
    })
    @PostMapping
    public ResponseEntity<AssetResponseDTO> createAsset(
            @Valid @RequestBody AssetRequestDTO requestDTO) {
        log.info("POST /api/assets - ticker: {}", requestDTO.getTicker());
        AssetResponseDTO response = assetService.createAsset(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // ENDPOINT 2: Listar todos os ativos
    // =========================================================================

    /**
     * GET /api/assets
     *
     * EXEMPLO DE RESPOSTA (200 OK):
     * [
     *   { "ticker": "PETR4", "name": "Petrobras", "priceCount": 252 },
     *   { "ticker": "VALE3", "name": "Vale",      "priceCount": 252 }
     * ]
     */
    @Operation(summary = "Listar todos os ativos")
    @GetMapping
    public ResponseEntity<List<AssetResponseDTO>> findAll() {
        log.debug("GET /api/assets - listando todos os ativos");
        return ResponseEntity.ok(assetService.findAll());
    }

    // =========================================================================
    // ENDPOINT 3: Buscar ativo por ticker
    // =========================================================================

    /**
     * GET /api/assets/{ticker}
     *
     * EXEMPLO: GET http://localhost:8082/api/assets/PETR4
     *
     * @PathVariable String ticker → extrai "PETR4" da URL
     */
    @Operation(summary = "Buscar ativo por ticker")
    @ApiResponse(responseCode = "404", description = "Ticker não encontrado")
    @GetMapping("/{ticker}")
    public ResponseEntity<AssetResponseDTO> findByTicker(
            @Parameter(description = "Código do ativo (ex: PETR4)") @PathVariable String ticker) {
        log.debug("GET /api/assets/{}", ticker);
        return ResponseEntity.ok(assetService.findByTicker(ticker));
    }

    // =========================================================================
    // ENDPOINT 4: Adicionar preço histórico
    // =========================================================================

    /**
     * POST /api/assets/{ticker}/prices
     *
     * Registra o preço de fechamento de um ativo em uma data específica.
     * Após salvar, publica evento assíncrono no RabbitMQ.
     *
     * EXEMPLO DE REQUISIÇÃO:
     * POST http://localhost:8082/api/assets/PETR4/prices
     * {
     *   "price": 36.50,
     *   "priceDate": "2024-01-15"
     * }
     *
     * PARA TESTAR: você pode adicionar vários preços seguidos.
     * Após pelo menos 2 preços, o endpoint /stats estará disponível.
     */
    @Operation(summary = "Adicionar preço histórico", description = "Registra o preço de fechamento de um ativo em uma data. Publica evento no RabbitMQ após salvar.")
    @PostMapping("/{ticker}/prices")
    public ResponseEntity<AssetResponseDTO> addPrice(
            @Parameter(description = "Código do ativo") @PathVariable String ticker,
            @Valid @RequestBody PriceRequestDTO requestDTO) {
        log.info("POST /api/assets/{}/prices - price: {}", ticker, requestDTO.getPrice());
        AssetResponseDTO response = assetService.addPrice(ticker, requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // ENDPOINT 5: Calcular estatísticas financeiras (MARKOWITZ INPUT)
    // =========================================================================

    /**
     * GET /api/assets/{ticker}/stats
     *
     * Calcula e retorna as estatísticas financeiras do ativo:
     *   - Retorno médio diário (μ)
     *   - Volatilidade diária (σ)
     *   - Retorno anualizado (μ × 252)
     *   - Volatilidade anualizada (σ × √252)
     *
     * ESTES VALORES SÃO OS INPUTS DO ALGORITMO DE MARKOWITZ!
     * O portfolio-service usa estas métricas para construir a fronteira eficiente.
     *
     * EXEMPLO DE RESPOSTA (200 OK):
     * {
     *   "ticker": "PETR4",
     *   "name": "Petrobras S.A. PN",
     *   "priceCount": 252,
     *   "averageDailyReturn": 0.00127,
     *   "dailyVolatility": 0.02106,
     *   "annualizedReturn": 0.32004,      ← 32% ao ano
     *   "annualizedVolatility": 0.33434   ← 33.4% de risco anual
     * }
     *
     * INTERPRETAÇÃO:
     *   - Retorno anual esperado: 32% → ativo com bom retorno
     *   - Volatilidade anual: 33.4% → ativo arriscado (alta volatilidade)
     *   - Sharpe (estimativa): (32% - 10.75% SELIC) / 33.4% ≈ 0.64
     *
     * REQUISITO: pelo menos 2 preços históricos cadastrados.
     */
    @Operation(
        summary = "Calcular estatísticas financeiras (INPUT DO MARKOWITZ)",
        description = "Retorna retorno médio (μ) e volatilidade (σ) diários e anualizados. Exige mínimo 2 preços históricos cadastrados."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estatísticas calculadas"),
        @ApiResponse(responseCode = "400", description = "Preços insuficientes (min. 2)")
    })
    @GetMapping("/{ticker}/stats")
    public ResponseEntity<AssetStatsDTO> getStats(
            @Parameter(description = "Código do ativo") @PathVariable String ticker) {
        log.info("GET /api/assets/{}/stats - calculando estatísticas", ticker);
        AssetStatsDTO stats = assetService.calculateStats(ticker);
        return ResponseEntity.ok(stats);
    }
}
