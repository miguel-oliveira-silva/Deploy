package com.markovitz.assetservice.dto;

/**
 * ============================================================================
 * AssetStatsDTO — Estatísticas Financeiras de um Ativo
 * ============================================================================
 *
 * Este é o DTO mais importante do asset-service!
 * Ele carrega as métricas que o portfolio-service usa para otimização.
 *
 * EXPLICAÇÃO MATEMÁTICA DAS MÉTRICAS:
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Dado N preços históricos [P₀, P₁, P₂, ..., Pₙ]:
 *
 * 1. RETORNOS DIÁRIOS [r₁, r₂, ..., rₙ]:
 *    rₜ = (Pₜ - Pₜ₋₁) / Pₜ₋₁
 *
 *    Exemplo com PETR4:
 *    P₀ = 36.50, P₁ = 37.20, P₂ = 36.80, P₃ = 37.90
 *    r₁ = (37.20 - 36.50) / 36.50 =  0.01918 (+1.92%)
 *    r₂ = (36.80 - 37.20) / 37.20 = -0.01075 (-1.08%)
 *    r₃ = (37.90 - 36.80) / 36.80 =  0.02989 (+2.99%)
 *
 * 2. RETORNO MÉDIO (μ — "mu"):
 *    μ = (r₁ + r₂ + ... + rₙ) / n
 *
 *    μ = (0.01918 - 0.01075 + 0.02989) / 3 = 0.01277 (+1.28% ao dia)
 *
 *    INTERPRETAÇÃO: Na média, o ativo cresceu 1.28% por dia.
 *    Em Markowitz, μ é o "retorno esperado" do ativo.
 *
 * 3. VOLATILIDADE/RISCO (σ — "sigma") = Desvio Padrão dos Retornos:
 *
 *    σ² (variância) = Σ(rₜ - μ)² / (n-1)
 *    σ  (desvio padrão) = √σ²
 *
 *    Calculando:
 *    (r₁ - μ)² = (0.01918 - 0.01277)² = 0.0000410
 *    (r₂ - μ)² = (-0.01075 - 0.01277)² = 0.0005532
 *    (r₃ - μ)² = (0.02989 - 0.01277)² = 0.0002929
 *
 *    σ² = (0.0000410 + 0.0005532 + 0.0002929) / (3-1) = 0.0004436
 *    σ  = √0.0004436 = 0.02106 = 2.11%
 *
 *    INTERPRETAÇÃO: O retorno diário varia em ±2.11% em média.
 *    Ativo com σ alto = mais volátil = mais arriscado.
 *
 * 4. ÍNDICE DE SHARPE (referência):
 *    Sharpe = (μ - taxa_livre_de_risco) / σ
 *    Mede o retorno por unidade de risco. Quanto maior, melhor.
 *
 * ============================================================================
 */
public class AssetStatsDTO {

    /** Ticker do ativo (ex: "PETR4") */
    private String ticker;

    /** Nome do ativo */
    private String name;

    /** Número de preços históricos usados no cálculo */
    private long priceCount;

    /**
     * Retorno médio diário (μ).
     * Representa: quanto o ativo cresce em média por dia.
     * Ex: 0.0012 = 0.12% ao dia
     */
    private double averageDailyReturn;

    /**
     * Volatilidade = desvio padrão dos retornos diários (σ).
     * Representa: o quanto o retorno varia dia a dia.
     * Ex: 0.021 = 2.1% de variação típica por dia
     *
     * IMPORTANTE: Esta é a medida de RISCO em Markowitz!
     * Maior volatilidade → maior risco → exige maior retorno esperado para compensar.
     */
    private double dailyVolatility;

    /**
     * Volatilidade anualizada (σ × √252).
     *
     * Como se converte volatilidade diária para anual?
     *   σ_anual = σ_diária × √252
     *
     * Por que √252?
     *   - 252 é o número de dias de pregão em um ano (mercado brasileiro B3)
     *   - Os retornos não são aditivos, mas como os quadrados são,
     *     a variância é aditiva: σ²_anual = 252 × σ²_diária
     *   - Logo: σ_anual = σ_diária × √252
     *
     * Usamos o valor anualizado por ser mais intuitivo para comparação.
     * Ex: σ_anual = 30% significa que o preço varia ±30% ao ano — alto risco!
     */
    private double annualizedVolatility;

    /**
     * Retorno médio anualizado (μ × 252).
     * Convertido para facilitar comparação com taxa SELIC, CDI, etc.
     * Ex: 0.30 = 30% ao ano
     */
    private double annualizedReturn;

    public AssetStatsDTO() {}

    public AssetStatsDTO(String ticker, String name, long priceCount,
                          double averageDailyReturn, double dailyVolatility,
                          double annualizedVolatility, double annualizedReturn) {
        this.ticker = ticker;
        this.name = name;
        this.priceCount = priceCount;
        this.averageDailyReturn = averageDailyReturn;
        this.dailyVolatility = dailyVolatility;
        this.annualizedVolatility = annualizedVolatility;
        this.annualizedReturn = annualizedReturn;
    }

    // Getters e Setters
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getPriceCount() { return priceCount; }
    public void setPriceCount(long priceCount) { this.priceCount = priceCount; }

    public double getAverageDailyReturn() { return averageDailyReturn; }
    public void setAverageDailyReturn(double averageDailyReturn) { this.averageDailyReturn = averageDailyReturn; }

    public double getDailyVolatility() { return dailyVolatility; }
    public void setDailyVolatility(double dailyVolatility) { this.dailyVolatility = dailyVolatility; }

    public double getAnnualizedVolatility() { return annualizedVolatility; }
    public void setAnnualizedVolatility(double annualizedVolatility) { this.annualizedVolatility = annualizedVolatility; }

    public double getAnnualizedReturn() { return annualizedReturn; }
    public void setAnnualizedReturn(double annualizedReturn) { this.annualizedReturn = annualizedReturn; }
}
