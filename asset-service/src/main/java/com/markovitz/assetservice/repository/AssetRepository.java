package com.markovitz.assetservice.repository;

import com.markovitz.assetservice.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ============================================================================
 * ASSET REPOSITORY — Acesso ao Banco para Ativos
 * ============================================================================
 *
 * Seguindo o mesmo padrão do UserRepository:
 * Estende JpaRepository<Asset, Long> para ter CRUD gratuito.
 *
 * Query Methods customizados adicionados conforme necessidade do Service.
 */
@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    /**
     * Busca ativo pelo ticker.
     *
     * Spring Data JPA gera: SELECT * FROM assets WHERE ticker = ?
     *
     * Exemplos de uso:
     *   assetRepository.findByTicker("PETR4") → Optional com Petrobras
     *   assetRepository.findByTicker("XXXX9") → Optional vazio
     *
     * @param ticker código do ativo (ex: "PETR4")
     * @return Optional com o ativo, ou vazio se não encontrado
     */
    Optional<Asset> findByTicker(String ticker);

    /**
     * Verifica se já existe um ativo com o ticker informado.
     * Usado para evitar duplicatas ao cadastrar novo ativo.
     *
     * Spring Data JPA gera: SELECT EXISTS(SELECT 1 FROM assets WHERE ticker = ?)
     */
    boolean existsByTicker(String ticker);
}
