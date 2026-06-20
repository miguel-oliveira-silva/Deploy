package com.markovitz.portfolioservice.dto;

import com.markovitz.portfolioservice.entity.Portfolio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * DTO de entrada para criar uma nova carteira.
 *
 * EXEMPLO DE REQUISIÇÃO:
 * POST /api/portfolios
 * {
 *   "userId": 1,
 *   "name": "Minha aposentadoria",
 *   "tickers": ["PETR4", "VALE3", "ITUB4", "WEGE3"],
 *   "optimizationGoal": "MAX_SHARPE"
 * }
 */
public class CreatePortfolioRequestDTO {

    @NotNull(message = "O ID do usuário é obrigatório")
    @Positive(message = "O ID do usuário deve ser positivo")
    private Long userId;

    @NotBlank(message = "O nome da carteira é obrigatório")
    private String name;

    /**
     * Lista de tickers dos ativos a incluir na carteira.
     *
     * @NotEmpty → a lista não pode ser nula nem vazia
     *
     * Mínimo 2 ativos: com 1 ativo não há diversificação possível
     * (o "portfólio" seria apenas o ativo sozinho, sem otimização).
     */
    @NotEmpty(message = "A carteira deve conter pelo menos um ativo")
    private List<String> tickers;

    @NotNull(message = "O objetivo de otimização é obrigatório")
    private Portfolio.OptimizationGoal optimizationGoal;

    public CreatePortfolioRequestDTO() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getTickers() { return tickers; }
    public void setTickers(List<String> tickers) { this.tickers = tickers; }

    public Portfolio.OptimizationGoal getOptimizationGoal() { return optimizationGoal; }
    public void setOptimizationGoal(Portfolio.OptimizationGoal optimizationGoal) { this.optimizationGoal = optimizationGoal; }
}
