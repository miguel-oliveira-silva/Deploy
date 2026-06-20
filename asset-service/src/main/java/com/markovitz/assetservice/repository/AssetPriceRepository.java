package com.markovitz.assetservice.repository;

import com.markovitz.assetservice.entity.Asset;
import com.markovitz.assetservice.entity.AssetPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ============================================================================
 * ASSETPRICE REPOSITORY — Acesso ao Banco para Preços Históricos
 * ============================================================================
 *
 * Além dos métodos CRUD padrão, precisamos de queries especiais para
 * buscar os preços em ordem cronológica — essencial para calcular retornos.
 *
 * INTRODUÇÃO AO JPQL (@Query):
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Quando os Query Methods automáticos do Spring Data não são suficientes,
 * podemos escrever queries manualmente com @Query usando JPQL.
 *
 * JPQL (Java Persistence Query Language) é parecido com SQL, mas
 * referencia CLASSES e CAMPOS Java (não tabelas e colunas do banco).
 *
 * Comparação:
 *   SQL:  SELECT * FROM asset_prices WHERE asset_id = ? ORDER BY price_date ASC
 *   JPQL: SELECT p FROM AssetPrice p WHERE p.asset = :asset ORDER BY p.priceDate ASC
 *
 * Repare:
 *   - "AssetPrice" é o nome da CLASSE Java, não da tabela
 *   - "p.priceDate" é o nome do CAMPO Java, não da coluna
 *   - ":asset" é um parâmetro nomeado (mais legível que "?")
 *
 * ============================================================================
 */
@Repository
public interface AssetPriceRepository extends JpaRepository<AssetPrice, Long> {

    /**
     * Busca todos os preços de um ativo em ORDER cronológica crescente.
     *
     * A ordem cronológica é ESSENCIAL para calcular retornos:
     * Precisamos que P(t) venha depois de P(t-1) para fazer:
     *   retorno(t) = [P(t) - P(t-1)] / P(t-1)
     *
     * Sem ORDER BY, a ordem dos resultados é imprevisível.
     *
     * @Query define a query JPQL manualmente.
     * @Param("asset") liga o parâmetro :asset ao argumento do método.
     *
     * @param asset o ativo cujos preços queremos
     * @return lista de preços em ordem crescente por data
     */
    @Query("SELECT p FROM AssetPrice p WHERE p.asset = :asset ORDER BY p.priceDate ASC")
    List<AssetPrice> findByAssetOrderByPriceDateAsc(@Param("asset") Asset asset);

    /**
     * Conta quantos preços existem para um ativo.
     * Usado para validar se há dados históricos suficientes para calcular
     * estatísticas confiáveis.
     *
     * A Teoria de Markowitz requer pelo menos alguns meses de dados
     * para que as estimativas de retorno e risco sejam significativas.
     * Em produção, exigiríamos no mínimo 252 dias (1 ano de pregões).
     *
     * Spring Data JPA gera automaticamente pelo nome do método:
     *   SELECT COUNT(*) FROM asset_prices WHERE asset_id = ?
     */
    long countByAsset(Asset asset);
}
