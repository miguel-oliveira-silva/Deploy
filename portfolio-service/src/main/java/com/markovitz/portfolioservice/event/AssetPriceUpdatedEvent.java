package com.markovitz.portfolioservice.event;

import java.time.LocalDateTime;

/**
 * Evento CONSUMIDO pelo portfolio-service.
 * Publicado pelo asset-service quando um novo preço é adicionado.
 * Deve ter os MESMOS campos do AssetPriceUpdatedEvent do asset-service.
 */
public class AssetPriceUpdatedEvent {

    private String ticker;
    private String assetName;
    private java.math.BigDecimal newPrice;
    private java.time.LocalDate priceDate;
    private LocalDateTime occurredAt;

    public AssetPriceUpdatedEvent() {}

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getAssetName() { return assetName; }
    public void setAssetName(String assetName) { this.assetName = assetName; }

    public java.math.BigDecimal getNewPrice() { return newPrice; }
    public void setNewPrice(java.math.BigDecimal newPrice) { this.newPrice = newPrice; }

    public java.time.LocalDate getPriceDate() { return priceDate; }
    public void setPriceDate(java.time.LocalDate priceDate) { this.priceDate = priceDate; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
}
