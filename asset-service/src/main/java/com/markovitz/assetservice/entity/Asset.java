package com.markovitz.assetservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * ENTIDADE ASSET — Representa um Ativo Financeiro (Ação)
 * ============================================================================
 *
 * Um "ativo" no mercado financeiro é qualquer coisa que pode ser comprada
 * e vendida e que tem expectativa de gerar retorno. Aqui, tratamos ações
 * da bolsa de valores brasileira (B3).
 *
 * EXEMPLOS DE ATIVOS:
 *   Ticker   │ Nome                        │ Setor
 *   ─────────┼─────────────────────────────┼──────────────────
 *   PETR4    │ Petrobras PN                │ Energia
 *   VALE3    │ Vale ON                     │ Mineração
 *   ITUB4    │ Itaú Unibanco PN            │ Financeiro
 *   WEGE3    │ WEG ON                      │ Indústria
 *   MGLU3    │ Magazine Luiza ON           │ Varejo
 *
 * RELAÇÃO COM ASSETPRICE:
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Um Asset pode ter MUITOS preços históricos (um por data de pregão).
 * Isso é uma relação 1-para-N (OneToMany):
 *
 *   Asset (1) ──────────────── (N) AssetPrice
 *   PETR4     ←→  36.50 em 01/01, 37.20 em 02/01, 35.80 em 03/01, ...
 *
 * IMPORTÂNCIA PARA MARKOWITZ:
 * ─────────────────────────────────────────────────────────────────────────
 * Os preços históricos são usados para calcular:
 *   - Retorno diário de cada ativo
 *   - Retorno médio (μ) — entrada do modelo
 *   - Volatilidade/Risco (σ) — entrada do modelo
 *   - Covariância entre ativos — entrada do modelo
 *
 * ============================================================================
 */
@Entity
@Table(name = "assets")
public class Asset {

    /**
     * ID gerado automaticamente pelo banco.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * TICKER — Código único do ativo na bolsa.
     *
     * Ex: "PETR4", "VALE3", "ITUB4"
     * Convenção B3: 4 letras + 1 número (3=ON, 4=PN, 11=BDR/FII)
     *
     * unique=true → constraint UNIQUE no banco
     * length=10   → VARCHAR(10) — suficiente para todos os tickers
     */
    @Column(nullable = false, unique = true, length = 10)
    private String ticker;

    /**
     * NOME COMPLETO DO ATIVO
     * Ex: "Petrobras S.A. - Petróleo Brasileiro"
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * SETOR DE ATUAÇÃO
     * Importante para diversificação da carteira — Markowitz recomenda
     * diversificar entre setores com baixa correlação.
     * Ex: "Energia", "Financeiro", "Tecnologia", "Consumo"
     */
    @Column(length = 100)
    private String sector;

    /**
     * DATA DE CADASTRO DO ATIVO NO SISTEMA
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * LISTA DE PREÇOS HISTÓRICOS
     *
     * @OneToMany → Um Asset tem MUITOS AssetPrice
     *
     * mappedBy = "asset" → O campo "asset" na classe AssetPrice é o dono
     *   do relacionamento (tem a FK no banco). Isso é o lado "Many" da relação.
     *
     * cascade = ALL → operações no Asset propagam para os preços:
     *   - Se deletar o Asset → deleta todos os preços associados (CASCADE DELETE)
     *   - Se salvar o Asset  → salva também os preços na lista
     *
     * fetch = LAZY → os preços NÃO são carregados automaticamente do banco
     *   quando você busca um Asset. Só são carregados quando você acessa
     *   a lista explicitamente (via getPrices()).
     *   Isso é mais eficiente: você não carrega milhares de preços quando
     *   só precisa do ticker e nome do ativo.
     *
     * LAZY vs EAGER:
     *   LAZY  → carrega sob demanda (mais eficiente — padrão para @OneToMany)
     *   EAGER → carrega sempre junto (pode ser lento para listas grandes)
     */
    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AssetPrice> prices = new ArrayList<>();
    //                                 ↑ inicializa vazio para evitar NullPointerException

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================

    public Asset() {}

    public Asset(Long id, String ticker, String name, String sector, LocalDateTime createdAt) {
        this.id = id;
        this.ticker = ticker;
        this.name = name;
        this.sector = sector;
        this.createdAt = createdAt;
    }

    // =========================================================================
    // BUILDER
    // =========================================================================

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String ticker;
        private String name;
        private String sector;
        private LocalDateTime createdAt;

        public Builder id(Long id)               { this.id = id; return this; }
        public Builder ticker(String ticker)     { this.ticker = ticker; return this; }
        public Builder name(String name)         { this.name = name; return this; }
        public Builder sector(String sector)     { this.sector = sector; return this; }
        public Builder createdAt(LocalDateTime d){ this.createdAt = d; return this; }

        public Asset build() {
            return new Asset(id, ticker, name, sector, createdAt);
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
    // GETTERS E SETTERS
    // =========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<AssetPrice> getPrices() { return prices; }
    public void setPrices(List<AssetPrice> prices) { this.prices = prices; }

    @Override
    public String toString() {
        return "Asset{id=" + id + ", ticker='" + ticker + "', name='" + name +
                "', sector='" + sector + "'}";
    }
}
