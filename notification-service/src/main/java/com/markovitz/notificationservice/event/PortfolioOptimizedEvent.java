package com.markovitz.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Evento consumido do portfolio-service via RabbitMQ.
 * Publicado quando uma carteira é otimizada com sucesso.
 *
 * @JsonIgnoreProperties(ignoreUnknown = true) → mesma razão do UserRegisteredEvent:
 * tolerância a mudanças de schema do serviço publicador.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortfolioOptimizedEvent {

    private Long portfolioId;
    private String portfolioName;
    private Long userId;
    private String optimizationGoal;
    private Double expectedReturn;
    private Double portfolioRisk;
    private Double sharpeRatio;

    /**
     * Mapa de ticker → peso.
     * Ex: { "PETR4": 0.30, "VALE3": 0.25, "ITUB4": 0.25, "WEGE3": 0.20 }
     */
    private Map<String, Double> assetWeights;
    private LocalDateTime occurredAt;

    public PortfolioOptimizedEvent() {}

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
