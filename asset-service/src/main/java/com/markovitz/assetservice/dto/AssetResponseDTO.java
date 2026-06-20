package com.markovitz.assetservice.dto;

import com.markovitz.assetservice.entity.Asset;
import java.time.LocalDateTime;

/**
 * DTO de resposta com dados do ativo.
 * Retornado após criar ou buscar um ativo.
 */
public class AssetResponseDTO {

    private Long id;
    private String ticker;
    private String name;
    private String sector;
    private LocalDateTime createdAt;

    /** Quantidade de preços históricos registrados */
    private long priceCount;

    public AssetResponseDTO() {}

    public AssetResponseDTO(Long id, String ticker, String name,
                             String sector, LocalDateTime createdAt, long priceCount) {
        this.id = id;
        this.ticker = ticker;
        this.name = name;
        this.sector = sector;
        this.createdAt = createdAt;
        this.priceCount = priceCount;
    }

    /**
     * Factory method: converte Asset → AssetResponseDTO.
     * priceCount é passado separadamente pois exige query ao banco.
     */
    public static AssetResponseDTO from(Asset asset, long priceCount) {
        return new AssetResponseDTO(
                asset.getId(),
                asset.getTicker(),
                asset.getName(),
                asset.getSector(),
                asset.getCreatedAt(),
                priceCount
        );
    }

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

    public long getPriceCount() { return priceCount; }
    public void setPriceCount(long priceCount) { this.priceCount = priceCount; }
}
