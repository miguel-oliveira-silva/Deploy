package com.markovitz.portfolioservice.entity;

import jakarta.persistence.*;

/**
 * ============================================================================
 * ENTIDADE PORTFOLIOASSET — Ativo dentro de uma Carteira
 * ============================================================================
 *
 * Representa a participação de um ativo específico em uma carteira,
 * incluindo o PESO (percentual) calculado pelo algoritmo de Markowitz.
 *
 * EXEMPLO:
 *   Carteira "Minha aposentadoria" com 4 ativos após otimização:
 *
 *   ticker   │ weight │ expectedReturn │ risk
 *   ─────────┼────────┼────────────────┼──────
 *   PETR4    │  0.30  │  0.32 (32%aa) │ 0.334
 *   VALE3    │  0.25  │  0.28 (28%aa) │ 0.298
 *   ITUB4    │  0.25  │  0.22 (22%aa) │ 0.241
 *   WEGE3    │  0.20  │  0.35 (35%aa) │ 0.289
 *
 * A SOMA DOS PESOS DEVE SER SEMPRE 1.0 (100%)!
 * Isso é a restrição principal do problema de Markowitz.
 *
 * POR QUE NÃO USAR FK PARA A TABELA DE ATIVOS?
 * ─────────────────────────────────────────────────────────────────────────
 * O asset-service tem seu próprio banco de dados.
 * O portfolio-service não pode ter FK para a tabela do asset-service.
 * Em microsserviços, a integridade referencial entre serviços é
 * garantida por EVENTOS, não por constraints do banco.
 *
 * Por isso salvamos apenas o "ticker" (string) e as métricas relevantes,
 * não uma FK para um ID de ativo.
 *
 * ============================================================================
 */
@Entity
@Table(name = "portfolio_assets")
public class PortfolioAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * RELACIONAMENTO com a carteira.
     * ManyToOne: muitos PortfolioAsset para uma Portfolio.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    /**
     * TICKER do ativo (referência ao asset-service).
     * Não é FK — apenas uma referência por string.
     */
    @Column(nullable = false, length = 10)
    private String ticker;

    /**
     * PESO do ativo na carteira após otimização.
     * Valor entre 0.0 e 1.0 representando o percentual do capital.
     * Ex: 0.30 = 30% do capital investido neste ativo.
     *
     * Inicialmente null (antes da otimização).
     * Preenchido pelo MarkowitzOptimizer após o cálculo.
     *
     * Restrição: soma de todos os pesos = 1.0
     */
    @Column
    private Double weight;

    /**
     * RETORNO ESPERADO ANUALIZADO deste ativo (cópia do asset-service).
     * Salvo para evitar recalcular a cada consulta.
     */
    @Column
    private Double expectedReturn;

    /**
     * RISCO (VOLATILIDADE ANUALIZADA) deste ativo (cópia do asset-service).
     */
    @Column
    private Double risk;

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================

    public PortfolioAsset() {}

    public PortfolioAsset(Long id, Portfolio portfolio, String ticker,
                           Double weight, Double expectedReturn, Double risk) {
        this.id = id;
        this.portfolio = portfolio;
        this.ticker = ticker;
        this.weight = weight;
        this.expectedReturn = expectedReturn;
        this.risk = risk;
    }

    // =========================================================================
    // BUILDER
    // =========================================================================

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Portfolio portfolio;
        private String ticker;
        private Double weight;
        private Double expectedReturn;
        private Double risk;

        public Builder id(Long id)                      { this.id = id; return this; }
        public Builder portfolio(Portfolio p)           { this.portfolio = p; return this; }
        public Builder ticker(String ticker)            { this.ticker = ticker; return this; }
        public Builder weight(Double weight)            { this.weight = weight; return this; }
        public Builder expectedReturn(Double er)        { this.expectedReturn = er; return this; }
        public Builder risk(Double risk)                { this.risk = risk; return this; }

        public PortfolioAsset build() {
            return new PortfolioAsset(id, portfolio, ticker, weight, expectedReturn, risk);
        }
    }

    // =========================================================================
    // GETTERS E SETTERS
    // =========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Portfolio getPortfolio() { return portfolio; }
    public void setPortfolio(Portfolio portfolio) { this.portfolio = portfolio; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public Double getExpectedReturn() { return expectedReturn; }
    public void setExpectedReturn(Double expectedReturn) { this.expectedReturn = expectedReturn; }

    public Double getRisk() { return risk; }
    public void setRisk(Double risk) { this.risk = risk; }
}
