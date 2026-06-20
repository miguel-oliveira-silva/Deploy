package com.markovitz.assetservice.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * AssetPriceUpdatedEvent — Evento publicado quando um novo preço é adicionado
 * ============================================================================
 *
 * QUEM PUBLICA: asset-service (no AssetService.addPrice())
 * QUEM CONSOME: portfolio-service (para re-otimizar carteiras afetadas)
 *
 * POR QUE NOTIFICAR QUANDO UM PREÇO ATUALIZA?
 * ─────────────────────────────────────────────────────────────────────────
 * Quando novos preços são adicionados, as estatísticas do ativo mudam:
 *   - O retorno médio é recalculado com um novo ponto de dados
 *   - A volatilidade é recalculada
 *   - A covariância com outros ativos muda
 *
 * Consequentemente, a composição ótima das carteiras que contêm este ativo
 * também muda! O portfolio-service deve ser notificado para re-otimizar.
 *
 * FLUXO ASSÍNCRONO:
 *   1. Cliente POST /api/assets/PETR4/prices com preço do dia
 *   2. AssetService salva o preço no banco
 *   3. AssetService publica AssetPriceUpdatedEvent no RabbitMQ
 *   4. asset-service retorna resposta HTTP imediatamente (não espera!)
 *   5. portfolio-service consome o evento (pode ser milissegundos depois)
 *   6. portfolio-service re-otimiza todas as carteiras com PETR4
 *   7. portfolio-service publica PortfolioOptimizedEvent
 *   8. notification-service notifica os usuários afetados
 *
 * ============================================================================
 */
public class AssetPriceUpdatedEvent {

    /** Ticker do ativo cujo preço foi atualizado */
    private String ticker;

    /** Nome do ativo */
    private String assetName;

    /** O novo preço adicionado */
    private BigDecimal newPrice;

    /** Data do pregão do novo preço */
    private LocalDate priceDate;

    /** Momento em que o evento foi gerado */
    private LocalDateTime occurredAt;

    public AssetPriceUpdatedEvent() {}

    public AssetPriceUpdatedEvent(String ticker, String assetName,
                                   BigDecimal newPrice, LocalDate priceDate,
                                   LocalDateTime occurredAt) {
        this.ticker = ticker;
        this.assetName = assetName;
        this.newPrice = newPrice;
        this.priceDate = priceDate;
        this.occurredAt = occurredAt;
    }

    // Getters e Setters
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getAssetName() { return assetName; }
    public void setAssetName(String assetName) { this.assetName = assetName; }

    public BigDecimal getNewPrice() { return newPrice; }
    public void setNewPrice(BigDecimal newPrice) { this.newPrice = newPrice; }

    public LocalDate getPriceDate() { return priceDate; }
    public void setPriceDate(LocalDate priceDate) { this.priceDate = priceDate; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    @Override
    public String toString() {
        return "AssetPriceUpdatedEvent{ticker='" + ticker + "', newPrice=" + newPrice +
                ", priceDate=" + priceDate + ", occurredAt=" + occurredAt + "}";
    }
}
