package com.markovitz.portfolioservice.markowitz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ============================================================================
 * MARKOWITZ OPTIMIZER — O Algoritmo de Markowitz em Java
 * ============================================================================
 *
 * Esta classe implementa a Teoria Moderna do Portfólio de Harry Markowitz.
 *
 * SIMPLIFICAÇÃO DIDÁTICA:
 * ─────────────────────────────────────────────────────────────────────────
 * Para tornar o código compreensível, assumimos que os ativos NÃO SÃO
 * CORRELACIONADOS entre si (correlação ρᵢⱼ = 0 para i ≠ j).
 *
 * Isso torna a MATRIZ DE COVARIÂNCIA DIAGONAL:
 *
 *        [σ₁²   0    0  ]
 *   Σ =  [ 0   σ₂²   0  ]
 *        [ 0    0   σ₃² ]
 *
 * A INVERSA de uma matriz diagonal é simplesmente:
 *
 *         [1/σ₁²   0     0  ]
 *   Σ⁻¹ = [  0   1/σ₂²   0  ]
 *         [  0     0   1/σ₃²]
 *
 * Isso simplifica MUITO os cálculos (não precisamos de álgebra matricial complexa)
 * e ainda produz resultados válidos — apenas ignora efeitos de correlação.
 *
 * Em um sistema real (produção), você buscaria os preços históricos de
 * todos os ativos, alinharia as datas, calcularia a matriz de correlação
 * completa e aplicaria a inversão de matriz (ex: decomposição de Cholesky).
 *
 * REFERÊNCIA MATEMÁTICA COMPLETA:
 * ─────────────────────────────────────────────────────────────────────────
 *
 * PROBLEMA DE MINIMIZAÇÃO:
 *   Minimizar:  σₚ² = wᵀ Σ w
 *   Sujeito a:  Σ wᵢ = 1 (soma dos pesos = 100%)
 *               wᵢ ≥ 0    (sem venda a descoberto)
 *
 * SOLUÇÃO ANALÍTICA (com Σ diagonal):
 *
 *   1. PORTFÓLIO DE MÍNIMA VARIÂNCIA:
 *      wᵢ* = (1/σᵢ²) / Σⱼ(1/σⱼ²)
 *
 *      → Ativo mais volátil recebe menor peso
 *      → Ativo menos volátil recebe maior peso
 *
 *   2. PORTFÓLIO DE MÁXIMO ÍNDICE DE SHARPE:
 *      wᵢ* = ((μᵢ - rf)/σᵢ²) / Σⱼ((μⱼ - rf)/σⱼ²)
 *
 *      → Ativo com maior retorno ajustado ao risco (Sharpe individual) recebe mais peso
 *      → rf = taxa livre de risco (ex: taxa Selic)
 *
 * MÉTRICAS DO PORTFÓLIO FINAL:
 *   Retorno:  μₚ = Σ wᵢ × μᵢ
 *   Risco:    σₚ = √(Σ wᵢ² × σᵢ²)   [com Σ diagonal]
 *   Sharpe:   Sₚ = (μₚ - rf) / σₚ
 *
 * ============================================================================
 *
 * @Component → registra como bean Spring para injeção de dependência.
 */
@Component
public class MarkowitzOptimizer {

    private static final Logger log = LoggerFactory.getLogger(MarkowitzOptimizer.class);

    /**
     * Taxa livre de risco — lida do application.yml via @Value.
     *
     * @Value("${markowitz.risk-free-rate}") → injeta o valor da propriedade.
     * Se a propriedade não existir, usa 0.1075 como padrão.
     *
     * A taxa livre de risco é usada no cálculo do Índice de Sharpe:
     *   Sharpe = (μ - rf) / σ
     *
     * Usamos a taxa Selic como proxy da taxa livre de risco brasileira.
     */
    @Value("${markowitz.risk-free-rate:0.1075}")
    private double riskFreeRate;

    // =========================================================================
    // ESTRUTURA DE DADOS DE ENTRADA
    // =========================================================================

    /**
     * AssetData — dados de um ativo necessários para a otimização.
     *
     * Esta é uma "inner record" — um tipo de dado imutável introduzido no Java 16.
     * Records são perfeitos para DTOs internos: geram automaticamente
     * construtor, getters, equals, hashCode e toString.
     *
     * @param ticker           identificador do ativo
     * @param annualizedReturn retorno médio anualizado (μ) — vem do asset-service
     * @param annualizedRisk   volatilidade anualizada (σ) — vem do asset-service
     */
    public record AssetData(String ticker, double annualizedReturn, double annualizedRisk) {}

    // =========================================================================
    // RESULTADO DA OTIMIZAÇÃO
    // =========================================================================

    /**
     * OptimizationResult — resultado completo da otimização.
     *
     * Contém os pesos calculados para cada ativo e as métricas do portfólio.
     *
     * @param weights         pesos de cada ativo (na mesma ordem dos inputs)
     * @param portfolioReturn retorno esperado do portfólio (μₚ)
     * @param portfolioRisk   risco (volatilidade) do portfólio (σₚ)
     * @param sharpeRatio     índice de Sharpe do portfólio
     */
    public record OptimizationResult(
            double[] weights,
            double portfolioReturn,
            double portfolioRisk,
            double sharpeRatio
    ) {}

    // =========================================================================
    // MÉTODOS PÚBLICOS DE OTIMIZAÇÃO
    // =========================================================================

    /**
     * Calcula o PORTFÓLIO DE MÍNIMA VARIÂNCIA.
     *
     * Objetivo: encontrar os pesos que minimizam o risco total da carteira.
     *
     * DERIVAÇÃO DA FÓRMULA:
     * ─────────────────────────────────────────────────────────────────────
     * Para uma carteira com n ativos e matriz de covariância Σ diagonal:
     *
     * Minimizar:  σₚ² = Σᵢ wᵢ² × σᵢ²
     * Sujeito a:  Σᵢ wᵢ = 1
     *
     * Usando Multiplicadores de Lagrange:
     * L(w, λ) = Σᵢ wᵢ² × σᵢ² - λ(Σᵢ wᵢ - 1)
     *
     * ∂L/∂wᵢ = 2 × wᵢ × σᵢ² - λ = 0
     * → wᵢ = λ / (2σᵢ²)
     *
     * Usando Σᵢ wᵢ = 1:
     * Σᵢ λ/(2σᵢ²) = 1
     * λ = 2 / Σᵢ(1/σᵢ²)
     *
     * Substituindo:
     * wᵢ* = (1/σᵢ²) / Σⱼ(1/σⱼ²)  ← FÓRMULA FINAL
     *
     * INTERPRETAÇÃO: Ativos mais arriscados (σ² maior) recebem MENOS peso.
     *
     * @param assets lista de ativos com suas estatísticas
     * @return resultado com pesos ótimos e métricas do portfólio
     */
    public OptimizationResult minimizeVariance(List<AssetData> assets) {
        log.info("Calculando portfólio de mínima variância para {} ativos", assets.size());
        validateInputs(assets);

        int n = assets.size();
        double[] weights = new double[n];

        // =====================================================================
        // PASSO 1: Calcular 1/σᵢ² para cada ativo
        // =====================================================================
        double[] inverseVariances = new double[n];
        double sumInverseVariances = 0.0;

        for (int i = 0; i < n; i++) {
            double sigma = assets.get(i).annualizedRisk();
            // Proteção: se σ = 0, usar um valor mínimo para evitar divisão por zero
            double sigma2 = sigma * sigma;
            if (sigma2 < 1e-10) sigma2 = 1e-10;

            inverseVariances[i] = 1.0 / sigma2;          // 1/σᵢ²
            sumInverseVariances += inverseVariances[i];   // Σⱼ(1/σⱼ²)
        }

        // =====================================================================
        // PASSO 2: Calcular os pesos ótimos
        // wᵢ* = (1/σᵢ²) / Σⱼ(1/σⱼ²)
        // =====================================================================
        for (int i = 0; i < n; i++) {
            weights[i] = inverseVariances[i] / sumInverseVariances;
        }

        // =====================================================================
        // PASSO 3: Calcular métricas do portfólio resultante
        // =====================================================================
        return calculatePortfolioMetrics(assets, weights);
    }

    /**
     * Calcula o PORTFÓLIO DE MÁXIMO ÍNDICE DE SHARPE.
     *
     * Objetivo: encontrar os pesos que maximizam o Índice de Sharpe,
     * ou seja, a melhor relação retorno-por-unidade-de-risco.
     *
     * DERIVAÇÃO DA FÓRMULA:
     * ─────────────────────────────────────────────────────────────────────
     * Maximizar: Sₚ = (μₚ - rf) / σₚ
     *
     * Para Σ diagonal, a solução é o chamado "Tangency Portfolio":
     * wᵢ* = ((μᵢ - rf)/σᵢ²) / Σⱼ((μⱼ - rf)/σⱼ²)
     *
     * INTERPRETAÇÃO: Cada ativo é ponderado pelo seu "excesso de retorno
     * por unidade de variância" — quanto mais retorno EXTRA (acima de rf)
     * por unidade de risco, maior o peso.
     *
     * ATENÇÃO — CASO ESPECIAL:
     * Se (μᵢ - rf) ≤ 0 para algum ativo (ativo com retorno menor que
     * a taxa livre de risco), seu peso seria negativo (venda a descoberto).
     * Para simplificar (e porque queremos pesos ≥ 0), descartamos esses
     * ativos ou fixamos seu peso em 0.
     *
     * @param assets lista de ativos com suas estatísticas
     * @return resultado com pesos ótimos e métricas do portfólio
     */
    public OptimizationResult maximizeSharpe(List<AssetData> assets) {
        log.info("Calculando portfólio de máximo Sharpe para {} ativos", assets.size());
        validateInputs(assets);

        int n = assets.size();
        double[] weights = new double[n];

        // =====================================================================
        // PASSO 1: Calcular (μᵢ - rf) / σᵢ² para cada ativo
        // =====================================================================
        double[] sharpeNumerators = new double[n];
        double sumPositive = 0.0;

        for (int i = 0; i < n; i++) {
            double mu    = assets.get(i).annualizedReturn();
            double sigma = assets.get(i).annualizedRisk();
            double sigma2 = sigma * sigma;
            if (sigma2 < 1e-10) sigma2 = 1e-10;

            double excessReturn = mu - riskFreeRate; // (μᵢ - rf)

            // Só consideramos ativos com excesso de retorno positivo
            // (ativos que "batem" a taxa livre de risco)
            if (excessReturn > 0) {
                sharpeNumerators[i] = excessReturn / sigma2; // (μᵢ - rf) / σᵢ²
                sumPositive += sharpeNumerators[i];
            } else {
                // Ativo não supera rf: recebe peso 0
                sharpeNumerators[i] = 0.0;
                log.warn("Ativo '{}' tem retorno ({}) menor ou igual à taxa livre de risco ({}). Peso = 0.",
                        assets.get(i).ticker(), mu, riskFreeRate);
            }
        }

        // =====================================================================
        // PASSO 2: Calcular os pesos
        // =====================================================================
        if (sumPositive <= 0) {
            // CASO ESPECIAL: nenhum ativo supera a taxa livre de risco.
            // Fallback: usamos mínima variância.
            log.warn("Nenhum ativo supera a taxa livre de risco. Usando mínima variância como fallback.");
            return minimizeVariance(assets);
        }

        for (int i = 0; i < n; i++) {
            weights[i] = sharpeNumerators[i] / sumPositive;
        }

        // =====================================================================
        // PASSO 3: Calcular métricas do portfólio resultante
        // =====================================================================
        return calculatePortfolioMetrics(assets, weights);
    }

    // =========================================================================
    // MÉTODOS PRIVADOS
    // =========================================================================

    /**
     * Calcula as métricas do portfólio (μₚ, σₚ, Sharpe) dados os pesos.
     *
     * RETORNO DO PORTFÓLIO:
     *   μₚ = Σᵢ wᵢ × μᵢ
     *   (média ponderada dos retornos individuais)
     *
     * RISCO DO PORTFÓLIO (com Σ diagonal — sem correlação):
     *   σₚ² = Σᵢ wᵢ² × σᵢ²
     *   σₚ  = √(Σᵢ wᵢ² × σᵢ²)
     *
     *   NOTA: Com correlação, seria: σₚ² = Σᵢ Σⱼ wᵢ wⱼ σᵢ σⱼ ρᵢⱼ
     *   Como ρᵢⱼ = 0 para i≠j, os termos cruzados somem.
     *
     * ÍNDICE DE SHARPE:
     *   Sₚ = (μₚ - rf) / σₚ
     */
    private OptimizationResult calculatePortfolioMetrics(List<AssetData> assets,
                                                          double[] weights) {
        double portfolioReturn  = 0.0;
        double portfolioVariance = 0.0;

        for (int i = 0; i < assets.size(); i++) {
            double mu    = assets.get(i).annualizedReturn();
            double sigma = assets.get(i).annualizedRisk();
            double w     = weights[i];

            // Contribuição do ativo i ao retorno: wᵢ × μᵢ
            portfolioReturn += w * mu;

            // Contribuição do ativo i à variância: wᵢ² × σᵢ²
            portfolioVariance += w * w * sigma * sigma;
        }

        double portfolioRisk = Math.sqrt(portfolioVariance);
        double sharpeRatio   = portfolioRisk > 0
                ? (portfolioReturn - riskFreeRate) / portfolioRisk
                : 0.0;

        log.info("Resultado — Retorno: {}%, Risco: {}%, Sharpe: {}",
                String.format("%.2f", portfolioReturn * 100),
                String.format("%.2f", portfolioRisk * 100),
                String.format("%.3f", sharpeRatio));

        return new OptimizationResult(weights, portfolioReturn, portfolioRisk, sharpeRatio);
    }

    /**
     * Valida que há pelo menos 2 ativos e que os dados são válidos.
     * Markowitz com 1 ativo não faz sentido — não há diversificação.
     */
    private void validateInputs(List<AssetData> assets) {
        if (assets == null || assets.isEmpty()) {
            throw new IllegalArgumentException("A lista de ativos não pode ser vazia.");
        }
        if (assets.size() < 2) {
            throw new IllegalArgumentException(
                    "São necessários pelo menos 2 ativos para otimização de Markowitz. " +
                    "Com apenas 1 ativo, a 'carteira' é simplesmente o ativo sozinho."
            );
        }
        for (AssetData a : assets) {
            if (a.annualizedRisk() < 0) {
                throw new IllegalArgumentException(
                        "O risco do ativo '" + a.ticker() + "' não pode ser negativo.");
            }
        }
    }
}
