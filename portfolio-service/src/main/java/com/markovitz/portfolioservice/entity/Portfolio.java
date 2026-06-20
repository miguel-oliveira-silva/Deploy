package com.markovitz.portfolioservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * ENTIDADE PORTFOLIO — Carteira de Investimentos de um Usuário
 * ============================================================================
 *
 * Uma carteira representa o conjunto de ativos que um usuário quer investir.
 * Após a otimização, cada ativo recebe um PESO (percentual do capital total).
 *
 * CICLO DE VIDA DE UMA CARTEIRA:
 * ─────────────────────────────────────────────────────────────────────────
 *
 *   1. PENDENTE → usuário cria a carteira com lista de tickers
 *      POST /api/portfolios → status: PENDENTE
 *
 *   2. OTIMIZANDO → usuário solicita a otimização
 *      POST /api/portfolios/{id}/optimize → status: OTIMIZANDO
 *
 *   3. OTIMIZADO → algoritmo de Markowitz calculou os pesos ótimos
 *      → status: OTIMIZADO, pesos dos ativos preenchidos
 *      → evento "portfolio.optimized" publicado no RabbitMQ
 *
 *   4. ERRO → falha no processo de otimização
 *      → status: ERRO, mensagem de erro salva
 *
 * RELAÇÃO COM PORTFOLIOASSET:
 * ─────────────────────────────────────────────────────────────────────────
 * Portfolio (1) ──────── (N) PortfolioAsset
 * PETR4 (30%), VALE3 (25%), ITUB4 (25%), WEGE3 (20%)
 *
 * ============================================================================
 */
@Entity
@Table(name = "portfolios")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID do usuário dono desta carteira.
     * Não fazemos FK para o user-service porque em microsserviços
     * cada serviço é dono dos seus dados.
     * A consistência entre serviços é garantida por eventos, não por FK.
     */
    @Column(nullable = false)
    private Long userId;

    /** Nome da carteira dado pelo usuário. Ex: "Minha aposentadoria" */
    @Column(nullable = false, length = 150)
    private String name;

    /**
     * STATUS atual da carteira.
     *
     * Usamos enum com @Enumerated(STRING) para salvar como texto no banco.
     * Facilita leitura direta no banco e evita erros ao reordenar o enum.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PortfolioStatus status;

    /**
     * OBJETIVO DE OTIMIZAÇÃO escolhido pelo usuário.
     * Determina qual estratégia de Markowitz será usada:
     *   - MIN_VARIANCE: minimizar o risco da carteira
     *   - MAX_SHARPE:   maximizar o índice de Sharpe (retorno/risco)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OptimizationGoal optimizationGoal;

    /**
     * RETORNO ESPERADO da carteira após otimização.
     * μₚ = Σ wᵢ × μᵢ (média ponderada dos retornos)
     * Salvo após a otimização ser concluída.
     */
    @Column
    private Double expectedReturn;

    /**
     * RISCO (VOLATILIDADE) da carteira após otimização.
     * σₚ = √(Σᵢ wᵢ² × σᵢ²) (para ativos não correlacionados)
     * Salvo após a otimização ser concluída.
     */
    @Column
    private Double portfolioRisk;

    /** Índice de Sharpe calculado após otimização */
    @Column
    private Double sharpeRatio;

    /** Mensagem de erro caso a otimização falhe */
    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime optimizedAt;

    /** Lista de ativos desta carteira (com seus pesos) */
    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PortfolioAsset> assets = new ArrayList<>();

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================

    public Portfolio() {}

    public Portfolio(Long id, Long userId, String name, PortfolioStatus status,
                     OptimizationGoal optimizationGoal) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.status = status;
        this.optimizationGoal = optimizationGoal;
    }

    // =========================================================================
    // BUILDER
    // =========================================================================

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long userId;
        private String name;
        private PortfolioStatus status;
        private OptimizationGoal optimizationGoal;

        public Builder id(Long id)                           { this.id = id; return this; }
        public Builder userId(Long userId)                   { this.userId = userId; return this; }
        public Builder name(String name)                     { this.name = name; return this; }
        public Builder status(PortfolioStatus s)             { this.status = s; return this; }
        public Builder optimizationGoal(OptimizationGoal g)  { this.optimizationGoal = g; return this; }

        public Portfolio build() {
            return new Portfolio(id, userId, name, status, optimizationGoal);
        }
    }

    // =========================================================================
    // JPA CALLBACK
    // =========================================================================

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // =========================================================================
    // ENUMS
    // =========================================================================

    /**
     * Status do ciclo de vida da carteira.
     */
    public enum PortfolioStatus {
        /** Carteira criada, aguardando otimização */
        PENDENTE,
        /** Processo de otimização em andamento */
        OTIMIZANDO,
        /** Otimização concluída com sucesso — pesos calculados */
        OTIMIZADO,
        /** Falha durante a otimização */
        ERRO
    }

    /**
     * Objetivo da otimização de Markowitz.
     *
     * MIN_VARIANCE:
     *   Encontra a carteira com o MENOR RISCO POSSÍVEL.
     *   wᵢ = (1/σᵢ²) / Σⱼ(1/σⱼ²)
     *   Recomendado para investidores CONSERVADORES.
     *
     * MAX_SHARPE:
     *   Encontra a carteira com a MELHOR RELAÇÃO RETORNO/RISCO.
     *   wᵢ = ((μᵢ - rf)/σᵢ²) / Σⱼ((μⱼ - rf)/σⱼ²)
     *   Recomendado para investidores MODERADOS e AGRESSIVOS.
     *   (onde rf = taxa livre de risco, ex: Selic)
     */
    public enum OptimizationGoal {
        MIN_VARIANCE,
        MAX_SHARPE
    }

    // =========================================================================
    // GETTERS E SETTERS
    // =========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public PortfolioStatus getStatus() { return status; }
    public void setStatus(PortfolioStatus status) { this.status = status; }

    public OptimizationGoal getOptimizationGoal() { return optimizationGoal; }
    public void setOptimizationGoal(OptimizationGoal optimizationGoal) { this.optimizationGoal = optimizationGoal; }

    public Double getExpectedReturn() { return expectedReturn; }
    public void setExpectedReturn(Double expectedReturn) { this.expectedReturn = expectedReturn; }

    public Double getPortfolioRisk() { return portfolioRisk; }
    public void setPortfolioRisk(Double portfolioRisk) { this.portfolioRisk = portfolioRisk; }

    public Double getSharpeRatio() { return sharpeRatio; }
    public void setSharpeRatio(Double sharpeRatio) { this.sharpeRatio = sharpeRatio; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getOptimizedAt() { return optimizedAt; }
    public void setOptimizedAt(LocalDateTime optimizedAt) { this.optimizedAt = optimizedAt; }

    public List<PortfolioAsset> getAssets() { return assets; }
    public void setAssets(List<PortfolioAsset> assets) { this.assets = assets; }
}
