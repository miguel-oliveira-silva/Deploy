package com.markovitz.portfolioservice.dto;

import com.markovitz.portfolioservice.entity.Portfolio;
import com.markovitz.portfolioservice.entity.PortfolioAsset;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de resposta com os dados completos de uma carteira.
 * Inclui os pesos de cada ativo e as métricas do portfólio.
 *
 * EXEMPLO DE RESPOSTA (após otimização):
 * {
 *   "id": 1,
 *   "userId": 1,
 *   "name": "Minha aposentadoria",
 *   "status": "OTIMIZADO",
 *   "optimizationGoal": "MAX_SHARPE",
 *   "expectedReturn": 0.2900,       ← 29% ao ano esperado
 *   "portfolioRisk": 0.2240,        ← 22.4% de risco anual
 *   "sharpeRatio": 0.8482,          ← bom índice de Sharpe
 *   "assets": [
 *     { "ticker": "PETR4", "weight": 0.30, "expectedReturn": 0.32, "risk": 0.334 },
 *     { "ticker": "VALE3", "weight": 0.25, "expectedReturn": 0.28, "risk": 0.298 },
 *     { "ticker": "ITUB4", "weight": 0.25, "expectedReturn": 0.22, "risk": 0.241 },
 *     { "ticker": "WEGE3", "weight": 0.20, "expectedReturn": 0.35, "risk": 0.289 }
 *   ],
 *   "createdAt": "2024-01-15T14:30:00",
 *   "optimizedAt": "2024-01-15T14:30:05"
 * }
 */
public class PortfolioResponseDTO {

    private Long id;
    private Long userId;
    private String name;
    private Portfolio.PortfolioStatus status;
    private Portfolio.OptimizationGoal optimizationGoal;
    private Double expectedReturn;
    private Double portfolioRisk;
    private Double sharpeRatio;
    private String errorMessage;
    private List<AssetWeightDTO> assets;
    private LocalDateTime createdAt;
    private LocalDateTime optimizedAt;

    public PortfolioResponseDTO() {}

    // =========================================================================
    // INNER DTO — Peso de cada ativo
    // =========================================================================

    /**
     * Representa o peso e as métricas de um ativo na carteira.
     * Serializado como objeto aninhado no JSON de resposta.
     */
    public static class AssetWeightDTO {
        private String ticker;
        private Double weight;         // Percentual alocado (0.0 a 1.0)
        private Double expectedReturn; // Retorno anualizado do ativo
        private Double risk;           // Volatilidade anualizada do ativo

        public AssetWeightDTO() {}

        public AssetWeightDTO(String ticker, Double weight,
                              Double expectedReturn, Double risk) {
            this.ticker = ticker;
            this.weight = weight;
            this.expectedReturn = expectedReturn;
            this.risk = risk;
        }

        /** Factory method a partir da entidade PortfolioAsset */
        public static AssetWeightDTO from(PortfolioAsset pa) {
            return new AssetWeightDTO(
                    pa.getTicker(), pa.getWeight(),
                    pa.getExpectedReturn(), pa.getRisk()
            );
        }

        public String getTicker() { return ticker; }
        public void setTicker(String ticker) { this.ticker = ticker; }

        public Double getWeight() { return weight; }
        public void setWeight(Double weight) { this.weight = weight; }

        public Double getExpectedReturn() { return expectedReturn; }
        public void setExpectedReturn(Double expectedReturn) { this.expectedReturn = expectedReturn; }

        public Double getRisk() { return risk; }
        public void setRisk(Double risk) { this.risk = risk; }
    }

    // =========================================================================
    // FACTORY METHOD
    // =========================================================================

    public static PortfolioResponseDTO from(Portfolio portfolio,
                                             List<PortfolioAsset> assets) {
        PortfolioResponseDTO dto = new PortfolioResponseDTO();
        dto.setId(portfolio.getId());
        dto.setUserId(portfolio.getUserId());
        dto.setName(portfolio.getName());
        dto.setStatus(portfolio.getStatus());
        dto.setOptimizationGoal(portfolio.getOptimizationGoal());
        dto.setExpectedReturn(portfolio.getExpectedReturn());
        dto.setPortfolioRisk(portfolio.getPortfolioRisk());
        dto.setSharpeRatio(portfolio.getSharpeRatio());
        dto.setErrorMessage(portfolio.getErrorMessage());
        dto.setCreatedAt(portfolio.getCreatedAt());
        dto.setOptimizedAt(portfolio.getOptimizedAt());
        dto.setAssets(assets.stream().map(AssetWeightDTO::from).toList());
        return dto;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Portfolio.PortfolioStatus getStatus() { return status; }
    public void setStatus(Portfolio.PortfolioStatus status) { this.status = status; }

    public Portfolio.OptimizationGoal getOptimizationGoal() { return optimizationGoal; }
    public void setOptimizationGoal(Portfolio.OptimizationGoal g) { this.optimizationGoal = g; }

    public Double getExpectedReturn() { return expectedReturn; }
    public void setExpectedReturn(Double expectedReturn) { this.expectedReturn = expectedReturn; }

    public Double getPortfolioRisk() { return portfolioRisk; }
    public void setPortfolioRisk(Double portfolioRisk) { this.portfolioRisk = portfolioRisk; }

    public Double getSharpeRatio() { return sharpeRatio; }
    public void setSharpeRatio(Double sharpeRatio) { this.sharpeRatio = sharpeRatio; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public List<AssetWeightDTO> getAssets() { return assets; }
    public void setAssets(List<AssetWeightDTO> assets) { this.assets = assets; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getOptimizedAt() { return optimizedAt; }
    public void setOptimizedAt(LocalDateTime optimizedAt) { this.optimizedAt = optimizedAt; }
}
