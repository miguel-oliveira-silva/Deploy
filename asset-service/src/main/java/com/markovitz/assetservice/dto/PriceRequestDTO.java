package com.markovitz.assetservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para registrar o preço de fechamento de um ativo em uma data.
 *
 * EXEMPLO DE REQUISIÇÃO:
 * POST /api/assets/PETR4/prices
 * {
 *   "price": 36.50,
 *   "priceDate": "2024-01-15"
 * }
 */
public class PriceRequestDTO {

    /**
     * Preço de fechamento.
     *
     * @Positive → preço deve ser maior que zero (não existe ação com preço negativo
     *   no mercado convencional, apenas em derivativos especiais)
     */
    @NotNull(message = "O preço não pode ser nulo")
    @Positive(message = "O preço deve ser positivo")
    private BigDecimal price;

    /**
     * Data do pregão.
     * O cliente deve enviar no formato ISO: "2024-01-15"
     * Jackson converte automaticamente String → LocalDate.
     */
    @NotNull(message = "A data não pode ser nula")
    private LocalDate priceDate;

    public PriceRequestDTO() {}

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public LocalDate getPriceDate() { return priceDate; }
    public void setPriceDate(LocalDate priceDate) { this.priceDate = priceDate; }
}
