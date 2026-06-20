package com.markovitz.portfolioservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * ============================================================================
 * ASSET SERVICE CLIENT — Comunicação Síncrona com o asset-service
 * ============================================================================
 *
 * Este componente encapsula as chamadas HTTP para o asset-service.
 *
 * POR QUE CRIAR UMA CLASSE SEPARADA PARA O CLIENTE?
 * ─────────────────────────────────────────────────────────────────────────
 * Seguindo o princípio de Responsabilidade Única (SRP):
 *   - PortfolioService → lógica de negócio
 *   - AssetServiceClient → comunicação com serviço externo
 *
 * Se a URL do asset-service mudar, ou se quisermos trocar RestTemplate
 * por WebClient, só alteramos aqui — sem tocar na lógica de negócio.
 *
 * COMUNICAÇÃO SÍNCRONA (RestTemplate):
 * ─────────────────────────────────────────────────────────────────────────
 *   - O portfolio-service FAZ A CHAMADA e AGUARDA a resposta
 *   - Thread bloqueada durante a chamada HTTP
 *   - Mais simples de entender e depurar
 *   - Adequado aqui porque PRECISAMOS das estatísticas antes de otimizar
 *
 * Em produção, usaríamos WebClient (não bloqueante) do Spring WebFlux
 * ou OpenFeign (cliente declarativo) para melhor performance.
 *
 * ============================================================================
 */
@Component
public class AssetServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AssetServiceClient.class);

    private final RestTemplate restTemplate;

    /**
     * URL base do asset-service, lida do application.yml.
     * Ex: "http://localhost:8082"
     */
    @Value("${asset-service.base-url}")
    private String assetServiceBaseUrl;

    public AssetServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Busca as estatísticas financeiras de um ativo no asset-service.
     *
     * CHAMADA HTTP: GET {asset-service-url}/api/assets/{ticker}/stats
     *
     * COMO O RestTemplate FUNCIONA:
     * ─────────────────────────────────────────────────────────────────────
     * restTemplate.getForObject(url, ClasseDeRetorno.class)
     *   1. Monta a requisição HTTP GET para a URL
     *   2. Envia a requisição (bloqueante — aguarda resposta)
     *   3. Recebe o JSON de resposta
     *   4. Usa Jackson para converter JSON → objeto Java
     *   5. Retorna o objeto
     *
     * @param ticker código do ativo (ex: "PETR4")
     * @return estatísticas do ativo (μ, σ, valores anualizados)
     * @throws RuntimeException se o ativo não for encontrado ou houver erro de comunicação
     */
    public AssetStatsResponse getAssetStats(String ticker) {
        String url = assetServiceBaseUrl + "/api/assets/" + ticker.toUpperCase() + "/stats";
        log.debug("Chamando asset-service: GET {}", url);

        try {
            // getForObject: faz GET e converte a resposta JSON para AssetStatsResponse
            AssetStatsResponse stats = restTemplate.getForObject(url, AssetStatsResponse.class);

            if (stats == null) {
                throw new RuntimeException("Resposta nula do asset-service para ticker: " + ticker);
            }

            log.debug("Estatísticas recebidas para {}: retorno={}%, risco={}%",
                    ticker,
                    String.format("%.2f", stats.getAnnualizedReturn() * 100),
                    String.format("%.2f", stats.getAnnualizedVolatility() * 100));

            return stats;

        } catch (RestClientException e) {
            // RestClientException cobre: timeout, conexão recusada, 4xx, 5xx
            log.error("Erro ao chamar asset-service para ticker '{}': {}", ticker, e.getMessage());
            throw new RuntimeException(
                    "Não foi possível buscar estatísticas do ativo '" + ticker + "'. " +
                    "Verifique se o asset-service está rodando e se o ativo possui " +
                    "preços históricos suficientes. Detalhe: " + e.getMessage()
            );
        }
    }

    // =========================================================================
    // INNER CLASS — Espelho do AssetStatsDTO do asset-service
    // =========================================================================

    /**
     * Classe que mapeia a resposta JSON do endpoint GET /api/assets/{ticker}/stats
     * do asset-service.
     *
     * Os campos devem ter os MESMOS NOMES dos campos no JSON de resposta
     * para que o Jackson consiga deserializar corretamente.
     *
     * Alternativa: compartilhar um módulo "commons" com DTOs comuns entre serviços.
     * Para fins didáticos, duplicamos a classe aqui — mais simples de entender.
     */
    public static class AssetStatsResponse {
        private String ticker;
        private String name;
        private long priceCount;
        private double averageDailyReturn;
        private double dailyVolatility;
        private double annualizedVolatility;
        private double annualizedReturn;

        public AssetStatsResponse() {}

        public String getTicker()               { return ticker; }
        public void setTicker(String t)         { this.ticker = t; }

        public String getName()                 { return name; }
        public void setName(String n)           { this.name = n; }

        public long getPriceCount()             { return priceCount; }
        public void setPriceCount(long c)       { this.priceCount = c; }

        public double getAverageDailyReturn()   { return averageDailyReturn; }
        public void setAverageDailyReturn(double r) { this.averageDailyReturn = r; }

        public double getDailyVolatility()      { return dailyVolatility; }
        public void setDailyVolatility(double v){ this.dailyVolatility = v; }

        public double getAnnualizedVolatility() { return annualizedVolatility; }
        public void setAnnualizedVolatility(double v) { this.annualizedVolatility = v; }

        public double getAnnualizedReturn()     { return annualizedReturn; }
        public void setAnnualizedReturn(double r) { this.annualizedReturn = r; }
    }
}
