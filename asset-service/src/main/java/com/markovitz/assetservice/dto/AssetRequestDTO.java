package com.markovitz.assetservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para criar um novo ativo.
 *
 * O ticker é convertido para maiúsculas no Service antes de salvar,
 * então o cliente pode enviar "petr4" ou "PETR4".
 */
public class AssetRequestDTO {

    @NotBlank(message = "O ticker não pode ser vazio")
    @Size(min = 4, max = 10, message = "O ticker deve ter entre 4 e 10 caracteres")
    private String ticker;

    @NotBlank(message = "O nome do ativo não pode ser vazio")
    @Size(max = 200, message = "O nome deve ter no máximo 200 caracteres")
    private String name;

    // Setor é opcional — nem sempre sabemos o setor na hora do cadastro
    @Size(max = 100, message = "O setor deve ter no máximo 100 caracteres")
    private String sector;

    public AssetRequestDTO() {}

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
}
