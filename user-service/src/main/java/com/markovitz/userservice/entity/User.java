package com.markovitz.userservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade User - representa um usuário no banco de dados.
 *
 * Em JPA, uma Entity mapeia para uma tabela no banco.
 * Cada instância = uma linha, cada campo = uma coluna.
 *
 * Anotações principais:
 * @Entity - marca como tabela
 * @Id - chave primária
 * @GeneratedValue - geração automática de ID
 * @Column - configuração da coluna (nullable, unique, length)
 * @Enumerated(STRING) - salva enum como texto no banco
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Email único por usuário */
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** Senha - em produção deve usar hash (BCrypt) */
    @Column(nullable = false)
    private String password;

    /**
     * Perfil de risco do investidor.
     * Influencia qual portfólio é ótimo na Teoria de Markowitz.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskProfile riskProfile;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Construtores
    public User() {}

    public User(Long id, String name, String email, String password,
                RiskProfile riskProfile, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.riskProfile = riskProfile;
        this.createdAt = createdAt;
    }

    // Getters e Setters

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public RiskProfile getRiskProfile() { return riskProfile; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setRiskProfile(RiskProfile riskProfile) { this.riskProfile = riskProfile; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Builder Pattern
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
