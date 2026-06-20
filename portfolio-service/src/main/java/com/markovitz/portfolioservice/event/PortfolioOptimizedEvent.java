package com.markovitz.portfolioservice.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Evento PUBLICADO pelo portfolio-service após uma otimização bem-sucedida.
 * O notification-service consome este evento para notificar o usuário.
 *
 * EXEMPLO DO JSON publicado na fila:
 * {
 *   "portfolioId": 1,
 *   "portfolioName": "Minha aposentadoria",
 *   "userId": 1,
 *   "optimizationGoal": "MAX_SHARPE",
 *   "expectedReturn": 0.29,
 *   "portfolioRisk": 0.224,
 *   "sharpeRatio": 0.848,
 *   "assetWeights": { "PETR4": 0.30, "VALE3": 0.25, "ITUB4": 0.25, "WEGE3": 0.20 },
 *   "occurredAt": "2024-01-15T14:30:05"
 * }
 */
public class PortfolioOptimizedEvent {

    private Long portfolioId;
    private String portfolioName;
    private Long userId;
    private String optimizationGoal;
    private Double expectedReturn;
    private Double portfolioRisk;
    private Double sharpeRatio;
    /** Mapa de ticker → peso, para exibir na notificação */
    private Map<String, Double> assetWeights;
    private LocalDateTime occurredAt;

    public PortfolioOptimizedEvent() {}

    public PortfolioOptimizedEvent(Long portfolioId, String portfolioName, Long userId,
                                    String optimizationGoal, Double expectedReturn,
                                    Double portfolioRisk, Double sharpeRatio,
                                    Map<String, Double> assetWeights) {
        this.portfolioId = portfolioId;
        this.portfolioName = portfolioName;
        this.userId = userId;
        this.optimizationGoal = optimizationGoal;
        this.expectedReturn = expectedReturn;
        this.portfolioRisk = portfolioRisk;
        this.sharpeRatio = sharpeRatio;
        this.assetWeights = assetWeights;
        this.occurredAt = LocalDateTime.now();
    }

    public Long getPortfolioId() { return portfolioId; }
    public void setPortfolioId(Long portfolioId) { this.portfolioId = portfolioId; }

    public String getPortfolioName() { return portfolioName; }
    public void setPortfolioName(String portfolioName) { this.portfolioName = portfolioName; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getOptimizationGoal() { return optimizationGoal; }
    public void setOptimizationGoal(String optimizationGoal) { this.optimizationGoal = optimizationGoal; }

    public Double getExpectedReturn() { return expectedReturn; }
    public void setExpectedReturn(Double expectedReturn) { this.expectedReturn = expectedReturn; }

    public Double getPortfolioRisk() { return portfolioRisk; }
    public void setPortfolioRisk(Double portfolioRisk) { this.portfolioRisk = portfolioRisk; }

    public Double getSharpeRatio() { return sharpeRatio; }
    public void setSharpeRatio(Double sharpeRatio) { this.sharpeRatio = sharpeRatio; }

    public Map<String, Double> getAssetWeights() { return assetWeights; }
    public void setAssetWeights(Map<String, Double> assetWeights) { this.assetWeights = assetWeights; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
}
