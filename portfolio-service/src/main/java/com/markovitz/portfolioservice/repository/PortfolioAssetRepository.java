package com.markovitz.portfolioservice.repository;

import com.markovitz.portfolioservice.entity.Portfolio;
import com.markovitz.portfolioservice.entity.PortfolioAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Repositório para ativos dentro de uma carteira */
@Repository
public interface PortfolioAssetRepository extends JpaRepository<PortfolioAsset, Long> {

    /**
     * Lista todos os ativos de uma carteira.
     * Gerado pelo Spring: SELECT * FROM portfolio_assets WHERE portfolio_id = ?
     */
    List<PortfolioAsset> findByPortfolio(Portfolio portfolio);

    /**
     * Busca carteiras que contêm um ticker específico.
     * Usado quando um preço de ativo é atualizado para identificar
     * quais carteiras precisam ser re-otimizadas.
     *
     * Spring Data JPA gera:
     * SELECT pa FROM PortfolioAsset pa
     * WHERE pa.ticker = ?
     *   AND pa.portfolio.status = ?
     */
    List<PortfolioAsset> findByTickerAndPortfolio_Status(
            String ticker,
            Portfolio.PortfolioStatus status
    );
}
