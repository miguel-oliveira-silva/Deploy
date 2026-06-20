package com.markovitz.portfolioservice.repository;

import com.markovitz.portfolioservice.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Repositório para operações CRUD em carteiras */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    /**
     * Lista todas as carteiras de um usuário específico.
     * Spring Data JPA gera: SELECT * FROM portfolios WHERE user_id = ?
     */
    List<Portfolio> findByUserId(Long userId);
}
