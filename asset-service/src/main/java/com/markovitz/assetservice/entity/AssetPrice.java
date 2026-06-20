package com.markovitz.assetservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ============================================================================
 * ENTIDADE ASSETPRICE — Preço Histórico de um Ativo
 * ============================================================================
 *
 * Esta entidade armazena o preço de fechamento de um ativo em cada dia.
 * "Preço de fechamento" = o último preço negociado no fim do pregão.
 *
 * POR QUE PREÇOS HISTÓRICOS SÃO TÃO IMPORTANTES?
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Na Teoria de Markowitz, calculamos o retorno esperado e o risco de cada
 * ativo com base no seu histórico de preços. Quanto mais dados históricos,
 * mais preciso o modelo.
 *
 * CÁLCULO DO RETORNO DIÁRIO:
 *   Dado dois preços consecutivos P(t) e P(t-1):
 *
 *   Retorno(t) = [ P(t) - P(t-1) ] / P(t-1)
 *
 *   Exemplo:
 *     P(02/01) = 37.20
 *     P(01/01) = 36.50
 *     Retorno = (37.20 - 36.50) / 36.50 = 0.01917 = 1.917% no dia
 *
 * Com uma série de retornos diários, calculamos:
 *   μ (retorno médio) = média aritmética dos retornos
 *   σ (volatilidade)  = desvio padrão dos retornos
 *
 * RELAÇÃO COM ASSET:
 * ─────────────────────────────────────────────────────────────────────────
 * Esta é a classe "filha" do relacionamento OneToMany.
 * Ela contém a CHAVE ESTRANGEIRA (FK) que referencia o Asset.
 *
 * No banco de dados, a tabela asset_prices terá uma coluna:
 *   asset_id BIGINT REFERENCES assets(id)
 *
 * ============================================================================
 */
@Entity
@Table(
    name = "asset_prices",
    // Constraint de unicidade: não pode ter dois preços do mesmo ativo na mesma data
    // Isso previne duplicatas acidentais.
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_asset_date",
            columnNames = {"asset_id", "price_date"}
        )
    }
)
public class AssetPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * RELACIONAMENTO ManyToOne — MUITOS preços para UM ativo
     *
     * @ManyToOne → Esta entidade (AssetPrice) é o lado "Many".
     *   Cada preço pertence a um único ativo.
     *
     * @JoinColumn → Configura a coluna de FK no banco:
     *   name = "asset_id" → nome da coluna FK na tabela asset_prices
     *   nullable = false  → todo preço DEVE ter um ativo associado
     *
     * fetch = EAGER → ao carregar um AssetPrice, carrega o Asset junto.
     *   Faz sentido aqui porque sempre precisamos saber de qual ativo é o preço.
     *   Para @ManyToOne, EAGER é o padrão — não precisaríamos declarar,
     *   mas deixamos explícito por clareza didática.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    /**
     * PREÇO DE FECHAMENTO
     *
     * BigDecimal vs Double para valores monetários:
     * ─────────────────────────────────────────────
     * NUNCA use double/float para valores monetários!
     * Problemas de ponto flutuante podem gerar erros centavos/milhares.
     *
     * Exemplo do problema com double:
     *   double a = 0.1 + 0.2;
     *   // a = 0.30000000000000004 (errado!)
     *
     * BigDecimal representa números decimais com precisão exata.
     *
     * precision=18 → até 18 dígitos no total
     * scale=4      → até 4 casas decimais (ex: 36.5042)
     */
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal price;

    /**
     * DATA DO PREGÃO
     *
     * LocalDate → data sem horário (apenas dia/mês/ano)
     * Perfeito para preços de fechamento que são registrados por dia.
     *
     * Diferença:
     *   LocalDate     → 2024-01-15
     *   LocalDateTime → 2024-01-15T14:30:00
     *   LocalTime     → 14:30:00
     */
    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================

    public AssetPrice() {}

    public AssetPrice(Long id, Asset asset, BigDecimal price, LocalDate priceDate) {
        this.id = id;
        this.asset = asset;
        this.price = price;
        this.priceDate = priceDate;
    }

    // =========================================================================
    // BUILDER
    // =========================================================================

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Asset asset;
        private BigDecimal price;
        private LocalDate priceDate;

        public Builder id(Long id)               { this.id = id; return this; }
        public Builder asset(Asset asset)        { this.asset = asset; return this; }
        public Builder price(BigDecimal price)   { this.price = price; return this; }
        public Builder priceDate(LocalDate d)    { this.priceDate = d; return this; }

        public AssetPrice build() {
            return new AssetPrice(id, asset, price, priceDate);
        }
    }

    // =========================================================================
    // GETTERS E SETTERS
    // =========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public LocalDate getPriceDate() { return priceDate; }
    public void setPriceDate(LocalDate priceDate) { this.priceDate = priceDate; }

    @Override
    public String toString() {
        String ticker = asset != null ? asset.getTicker() : "null";
        return "AssetPrice{id=" + id + ", ticker='" + ticker +
                "', price=" + price + ", date=" + priceDate + "}";
    }
}
