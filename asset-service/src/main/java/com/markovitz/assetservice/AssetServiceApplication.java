package com.markovitz.assetservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================================
 * ASSET SERVICE — Microsserviço de Gestão de Ativos
 * ============================================================================
 *
 * Responsabilidades deste serviço:
 *
 *   1. CADASTRO DE ATIVOS
 *      - Registrar ações disponíveis para investimento (ex: PETR4, VALE3, ITUB4)
 *      - Cada ativo tem: ticker, nome, setor
 *
 *   2. HISTÓRICO DE PREÇOS
 *      - Armazenar preços históricos de cada ativo por data
 *      - Ex: PETR4 → R$36,50 em 01/01/2024, R$37,20 em 02/01/2024, etc.
 *
 *   3. CÁLCULO DE ESTATÍSTICAS FINANCEIRAS
 *      - Retorno médio diário: quanto o ativo cresceu em média por dia
 *      - Volatilidade: o quanto o retorno varia (desvio padrão dos retornos)
 *      - Estas métricas são os INPUTS do algoritmo de Markowitz!
 *
 *   4. PUBLICAÇÃO DE EVENTOS
 *      - Ao adicionar um novo preço, publica "asset.price.updated" no RabbitMQ
 *      - O portfolio-service consome este evento para re-otimizar carteiras
 *
 * RELAÇÃO COM A TEORIA DE MARKOWITZ:
 * ─────────────────────────────────────────────────────────────────────────
 * Markowitz definiu que uma carteira é caracterizada por dois números:
 *
 *   μ (mu)    = Retorno Esperado   → calculado aqui como média dos retornos diários
 *   σ (sigma) = Risco/Volatilidade → calculado aqui como desvio padrão dos retornos
 *
 * O objetivo é maximizar μ e minimizar σ — essa é a fronteira eficiente!
 *
 * ============================================================================
 */
@SpringBootApplication
public class AssetServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssetServiceApplication.class, args);
    }
}
