package com.markovitz.userservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * ENTIDADE USER — Representa um usuário no banco de dados
 * ============================================================================
 *
 * Em JPA, uma "Entity" é uma classe Java que mapeia para uma tabela no banco.
 * Cada instância da classe = uma linha na tabela.
 * Cada campo da classe = uma coluna na tabela.
 *
 * ANOTAÇÕES DO JPA:
 * ─────────────────────────────────────────────────────────────────────────
 *
 * @Entity        → Marca a classe como uma entidade JPA (tabela no banco)
 *
 * @Table         → Configura o nome da tabela. Se omitido, usa o nome da classe.
 *
 * @Id            → Marca o campo como chave primária da tabela
 *
 * @GeneratedValue → Define a estratégia de geração do ID:
 *   - IDENTITY → banco gera o ID (AUTO_INCREMENT no MySQL, SERIAL no PostgreSQL)
 *   - SEQUENCE → usa uma sequence do banco (padrão no Oracle e PostgreSQL)
 *   - AUTO     → Spring escolhe a estratégia baseado no banco de dados
 *
 * @Column        → Configura a coluna (nome, nullable, unique, length, etc.)
 *
 * @Enumerated    → Define como um enum é salvo no banco:
 *   - EnumType.STRING  → salva o nome ("CONSERVADOR") — recomendado
 *   - EnumType.ORDINAL → salva o número (0, 1, 2) — problemático se mudar ordem
 *
 * NOTA: Neste arquivo escrevemos os getters e setters manualmente (sem Lombok)
 * para que o código seja 100% explícito e fácil de estudar.
 * Em projetos reais usa-se @Data do Lombok para gerar tudo automaticamente.
 *
 * ============================================================================
 */
@Entity                 // JPA: esta classe é uma tabela no banco
@Table(name = "users")  // JPA: nome da tabela no banco (evitamos "user" pois é palavra reservada em SQL)
public class User {

    /**
     * CHAVE PRIMÁRIA — Identificador único do usuário.
     *
     * @Id      → este campo é a chave primária
     * @GeneratedValue → o banco gera o valor automaticamente
     * GenerationType.IDENTITY → usa AUTO_INCREMENT (perfeito para H2 e MySQL)
     *
     * Long vs Integer: Long vai até ~9,2 quintilhões — mais seguro para sistemas grandes.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * NOME DO USUÁRIO
     *
     * @Column configura:
     *   nullable = false → NOT NULL no banco (campo obrigatório)
     *   length = 100     → VARCHAR(100) no banco
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * EMAIL — Único por usuário (não pode haver dois usuários com o mesmo email)
     *
     * unique = true → cria uma UNIQUE CONSTRAINT no banco.
     * Se tentar inserir email duplicado, o banco lança uma exceção.
     */
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /**
     * SENHA — Em produção SEMPRE deve ser armazenada com hash (BCrypt, Argon2).
     * Para este trabalho escolar, simplificamos armazenando o texto direto.
     * NUNCA faça isso em produção!
     */
    @Column(nullable = false)
    private String password;

    /**
     * PERFIL DE RISCO DO INVESTIDOR
     *
     * Na Teoria de Markowitz, o perfil de risco do investidor influencia
     * quais portfólios são "ótimos" para ele:
     *
     * - CONSERVADOR → prefere menor risco, aceita menor retorno
     * - MODERADO    → equilíbrio entre risco e retorno
     * - AGRESSIVO   → aceita maior risco em busca de maior retorno
     *
     * @Enumerated(STRING) → salva "CONSERVADOR" na coluna, não o número 0.
     * Isso é mais seguro: se você reordenar o enum, os dados não corrompem.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskProfile riskProfile;

    /**
     * DATA/HORA DE CADASTRO
     *
     * LocalDateTime → data e hora sem timezone (suficiente para este projeto)
     * Em produção com usuários internacionais, considere usar ZonedDateTime ou Instant.
     *
     * updatable = false → uma vez definido, este campo nunca é atualizado pelo JPA.
     * Isso garante que a data de cadastro nunca mude.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================

    /** Construtor padrão — OBRIGATÓRIO para o JPA funcionar! */
    public User() {}

    /** Construtor completo — usado pelo Builder pattern */
    public User(Long id, String name, String email, String password,
                RiskProfile riskProfile, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.riskProfile = riskProfile;
        this.createdAt = createdAt;
    }

    // =========================================================================
    // GETTERS — Métodos para LER os valores dos campos
    // =========================================================================
    // O JPA, Jackson e outros frameworks usam getters para acessar os dados.

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public RiskProfile getRiskProfile() { return riskProfile; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // =========================================================================
    // SETTERS — Métodos para DEFINIR os valores dos campos
    // =========================================================================
    // O JPA usa setters ao reconstruir objetos do banco.

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setRiskProfile(RiskProfile riskProfile) { this.riskProfile = riskProfile; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // =========================================================================
    // BUILDER PATTERN — Para construção fluente de objetos
    // =========================================================================
    // O padrão Builder permite criar objetos de forma legível:
    //   User user = User.builder()
    //                   .name("João")
    //                   .email("joao@email.com")
    //                   .build();
    //
    // É especialmente útil quando um objeto tem muitos campos opcionais.

    /** Ponto de entrada do Builder */
    public static Builder builder() { return new Builder(); }

    /** Classe Builder interna — acumula valores e cria o User no final */
    public static class Builder {
        private Long id;
        private String name;
        private String email;
        private String password;
        private RiskProfile riskProfile;
        private LocalDateTime createdAt;

        // Cada método "seta" um campo e retorna o próprio Builder (encadeamento)
        public Builder id(Long id)                   { this.id = id; return this; }
        public Builder name(String name)             { this.name = name; return this; }
        public Builder email(String email)           { this.email = email; return this; }
        public Builder password(String password)     { this.password = password; return this; }
        public Builder riskProfile(RiskProfile rp)   { this.riskProfile = rp; return this; }
        public Builder createdAt(LocalDateTime dt)   { this.createdAt = dt; return this; }

        /** Cria e retorna o objeto User com os valores acumulados */
        public User build() {
            return new User(id, name, email, password, riskProfile, createdAt);
        }
    }

    // =========================================================================
    // CALLBACK JPA
    // =========================================================================

    /**
     * @PrePersist → Executado automaticamente pelo JPA ANTES de inserir no banco.
     *
     * Isso garante que createdAt seja sempre preenchido automaticamente,
     * sem precisar definir manualmente ao criar um User.
     *
     * Outros callbacks JPA úteis:
     *   @PreUpdate  → antes de atualizar
     *   @PostLoad   → depois de carregar do banco
     *   @PreRemove  → antes de deletar
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // =========================================================================
    // toString — Para exibir o objeto em logs de forma legível
    // =========================================================================

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                // password INTENCIONALMENTE omitido nos logs por segurança
                ", riskProfile=" + riskProfile +
                ", createdAt=" + createdAt +
                '}';
    }

    // =========================================================================
    // ENUM DE PERFIL DE RISCO
    // =========================================================================

    /**
     * Enum é uma classe especial com um conjunto fixo de constantes.
     * Usar enum ao invés de String evita valores inválidos
     * (ex: "ARROJADO" digitado errado).
     *
     * Esta é uma "nested enum" (enum dentro da classe User) — boa prática quando
     * o enum pertence conceitualmente à entidade.
     */
    public enum RiskProfile {

        /**
         * Investidor conservador:
         * Prioriza preservação de capital. Aceita retornos menores em troca
         * de menor volatilidade da carteira. Ex: aposentados, avessos ao risco.
         */
        CONSERVADOR,

        /**
         * Investidor moderado:
         * Equilíbrio entre risco e retorno. Carteira com diversificação
         * entre ativos defensivos e de crescimento.
         */
        MODERADO,

        /**
         * Investidor agressivo:
         * Tolera alta volatilidade em busca de maiores retornos.
         * Ex: jovens com longo horizonte de investimento.
         */
        AGRESSIVO
    }
}
