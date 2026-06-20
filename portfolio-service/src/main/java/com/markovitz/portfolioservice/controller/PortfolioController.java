package com.markovitz.portfolioservice.controller;

import com.markovitz.portfolioservice.dto.CreatePortfolioRequestDTO;
import com.markovitz.portfolioservice.dto.PortfolioResponseDTO;
import com.markovitz.portfolioservice.service.PortfolioService;
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
 * PORTFOLIO CONTROLLER — API REST do portfolio-service
 * ============================================================================
 *
 * ENDPOINTS:
 *
 *   POST   /api/portfolios               → criar carteira
 *   GET    /api/portfolios/{id}          → buscar carteira por ID
 *   GET    /api/portfolios/user/{userId} → listar carteiras de um usuário
 *   POST   /api/portfolios/{id}/optimize → EXECUTAR OTIMIZAÇÃO DE MARKOWITZ
 *
 * O endpoint de OTIMIZAÇÃO é o mais importante do sistema!
 * Ele coordena a chamada ao asset-service e executa o algoritmo.
 *
 * ============================================================================
 */
@RestController
@RequestMapping("/api/portfolios")
@Tag(name = "Carteiras", description = "Criação e otimização de carteiras de investimento (Algoritmo de Markowitz)")
public class PortfolioController {

    private static final Logger log = LoggerFactory.getLogger(PortfolioController.class);

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    /**
     * POST /api/portfolios
     *
     * Cria uma nova carteira no status PENDENTE.
     *
     * EXEMPLO:
     * POST http://localhost:8083/api/portfolios
     * {
     *   "userId": 1,
     *   "name": "Minha aposentadoria",
     *   "tickers": ["PETR4", "VALE3", "ITUB4", "WEGE3"],
     *   "optimizationGoal": "MAX_SHARPE"
     * }
     */
    @Operation(summary = "Criar carteira de investimento", description = "Cria uma nova carteira no status PENDENTE com os ativos (tickers) informados")
    @ApiResponse(responseCode = "201", description = "Carteira criada com sucesso")
    @PostMapping
    public ResponseEntity<PortfolioResponseDTO> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequestDTO requestDTO) {
        log.info("POST /api/portfolios - nome: '{}', ativos: {}",
                requestDTO.getName(), requestDTO.getTickers());
        PortfolioResponseDTO response = portfolioService.createPortfolio(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/portfolios/{id}
     *
     * Retorna a carteira com seus ativos e (se otimizada) os pesos calculados.
     */
    @Operation(summary = "Buscar carteira por ID")
    @GetMapping("/{id}")
    public ResponseEntity<PortfolioResponseDTO> findById(
            @Parameter(description = "ID da carteira") @PathVariable Long id) {
        log.debug("GET /api/portfolios/{}", id);
        return ResponseEntity.ok(portfolioService.findById(id));
    }

    /**
     * GET /api/portfolios/user/{userId}
     *
     * Lista todas as carteiras de um usuário.
     */
    @Operation(summary = "Listar carteiras de um usuário")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PortfolioResponseDTO>> findByUserId(
            @Parameter(description = "ID do usuário") @PathVariable Long userId) {
        log.debug("GET /api/portfolios/user/{}", userId);
        return ResponseEntity.ok(portfolioService.findByUserId(userId));
    }

    /**
     * POST /api/portfolios/{id}/optimize
     *
     * Executa o algoritmo de Markowitz para calcular os pesos ótimos.
     *
     * PRÉ-REQUISITOS:
     *   1. A carteira deve existir
     *   2. Cada ativo deve ter preços históricos no asset-service (min. 2)
     *   3. O asset-service deve estar rodando (porta 8082)
     *
     * EXEMPLO DE RESPOSTA (200 OK):
     * {
     *   "id": 1,
     *   "name": "Minha aposentadoria",
     *   "status": "OTIMIZADO",
     *   "optimizationGoal": "MAX_SHARPE",
     *   "expectedReturn": 0.29,   ← 29% ao ano esperado
     *   "portfolioRisk": 0.224,   ← 22.4% de risco anual
     *   "sharpeRatio": 0.848,
     *   "assets": [
     *     { "ticker": "PETR4", "weight": 0.30, "expectedReturn": 0.32, "risk": 0.334 },
     *     { "ticker": "VALE3", "weight": 0.25, "expectedReturn": 0.28, "risk": 0.298 }
     *   ],
     *   "optimizedAt": "2024-01-15T14:30:05"
     * }
     */
    @Operation(
        summary = "Executar otimização de Markowitz",
        description = "Maximiza o Índice de Sharpe consultando o asset-service e calcula os pesos ótimos de cada ativo. Após otimizar, publica evento 'portfolio.optimized' no RabbitMQ."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Carteira otimizada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Carteira não encontrada"),
        @ApiResponse(responseCode = "500", description = "Erro ao consultar asset-service ou dados insuficientes")
    })
    @PostMapping("/{id}/optimize")
    public ResponseEntity<PortfolioResponseDTO> optimize(
            @Parameter(description = "ID da carteira") @PathVariable Long id) {
        log.info("POST /api/portfolios/{}/optimize - iniciando otimização de Markowitz", id);
        PortfolioResponseDTO response = portfolioService.optimizePortfolio(id);
        return ResponseEntity.ok(response);
    }
}
