package com.markovitz.portfolioservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================================
 * PORTFOLIO SERVICE — O Coração do Sistema Markovitz
 * ============================================================================
 *
 * Este é o microsserviço mais sofisticado do sistema. Ele implementa
 * a Teoria Moderna do Portfólio de Harry Markowitz (1952).
 *
 * RESPONSABILIDADES:
 * ─────────────────────────────────────────────────────────────────────────
 *
 *   1. GERENCIAR CARTEIRAS
 *      - Criar carteiras de ativos para usuários
 *      - Cada carteira contém um conjunto de tickers escolhidos pelo usuário
 *
 *   2. OTIMIZAÇÃO DE MARKOWITZ
 *      - Buscar estatísticas dos ativos no asset-service (via REST)
 *      - Calcular a composição ótima da carteira (pesos de cada ativo)
 *      - Dois objetivos disponíveis:
 *          a) Mínima Variância → menor risco possível
 *          b) Máximo Índice de Sharpe → melhor relação retorno/risco
 *
 *   3. COMUNICAÇÃO ASSÍNCRONA
 *      - CONSOME: "asset.price.updated" → ao receber novos preços, re-otimiza
 *      - PUBLICA: "portfolio.optimized" → notifica o notification-service
 *
 * A MATEMÁTICA DE MARKOWITZ (resumo):
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Dado n ativos com retornos μ₁, μ₂, ..., μₙ e riscos σ₁, σ₂, ..., σₙ:
 *
 * Para um portfólio com pesos w₁, w₂, ..., wₙ (onde Σwᵢ = 1):
 *
 *   Retorno do portfólio: μₚ = Σ wᵢ × μᵢ
 *
 *   Risco do portfólio:   σₚ² = Σᵢ Σⱼ wᵢ × wⱼ × Cov(i, j)
 *
 *   Onde Cov(i, j) = ρᵢⱼ × σᵢ × σⱼ
 *   e ρᵢⱼ é a correlação entre os ativos i e j.
 *
 * O insight de Markowitz: ao combinar ativos COM BAIXA CORRELAÇÃO,
 * o risco da carteira é MENOR que a média ponderada dos riscos individuais!
 * Isso é a DIVERSIFICAÇÃO.
 *
 * Neste projeto, usamos uma simplificação didática:
 * Assumimos ativos não correlacionados (ρᵢⱼ = 0 para i ≠ j),
 * o que torna a matriz de covariância diagonal (mais fácil de inverter).
 * Veja MarkowitzOptimizer.java para os detalhes do cálculo.
 *
 * ============================================================================
 */
@SpringBootApplication
public class PortfolioServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioServiceApplication.class, args);
    }
}
