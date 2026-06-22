package com.markovitz.portfolioservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Portfolio Service - implementa a Teoria Moderna do Portfólio de Markowitz (1952).
 *
 * Responsabilidades:
 * 1. Gerenciar carteiras de ativos para usuários
 * 2. Otimizar carteiras usando a teoria de Markowitz
 *    - Busca estatísticas dos ativos no asset-service
 *    - Calcula a composição ótima (pesos de cada ativo)
 *    - Dois objetivos: Mínima Variância ou Máximo Índice de Sharpe
 * 3. Comunicação assíncrona via RabbitMQ
 *    - Consome: "asset.price.updated" para re-otimizar
 *    - Publica: "portfolio.optimized" para notificar
 *
 * Matemática básica:
 * Para n ativos com retornos μᵢ e riscos σᵢ, o portfólio com pesos wᵢ tem:
 *   Retorno: μₚ = Σ wᵢ × μᵢ
 *   Risco: σₚ² = Σᵢ Σⱼ wᵢ × wⱼ × Cov(i,j)
 *
 * Simplificação: assumimos ativos não correlacionados (ρᵢⱼ = 0 para i ≠ j).
 */
@SpringBootApplication
public class PortfolioServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioServiceApplication.class, args);
    }
}
