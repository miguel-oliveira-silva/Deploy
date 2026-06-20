package com.markovitz.userservice.dto;

import com.markovitz.userservice.entity.User;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * UserResponseDTO — Dados do Usuário Retornados pela API
 * ============================================================================
 *
 * Este DTO representa o que o servidor RETORNA ao cliente.
 *
 * Repare que NÃO incluímos o campo "password" aqui!
 * A senha nunca deve ser enviada de volta para o cliente — isso é uma
 * prática fundamental de segurança.
 *
 * DIFERENÇA ENTRE REQUEST E RESPONSE DTO:
 * ─────────────────────────────────────────────────────────────────────────
 *
 * RegisterRequestDTO  →  cliente envia  →  servidor processa
 * UserResponseDTO     ←  servidor cria  ←  retorna ao cliente
 *
 * O fluxo é:
 *   1. Cliente POST /api/users/register com RegisterRequestDTO no body
 *   2. Controller chama UserService.register(requestDTO)
 *   3. Service cria e salva a entidade User no banco
 *   4. Service converte User → UserResponseDTO
 *   5. Controller retorna UserResponseDTO com HTTP 201 Created
 *
 * ============================================================================
 */
public class UserResponseDTO {

    /** ID único gerado pelo banco — útil para o cliente referenciar o usuário */
    private Long id;

    /** Nome do usuário */
    private String name;

    /** Email do usuário — senha NUNCA é retornada! */
    private String email;

    /** Perfil de risco — será serializado como String no JSON */
    private User.RiskProfile riskProfile;

    /** Data/hora de cadastro — informação útil para exibir no perfil */
    private LocalDateTime createdAt;

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================

    /** Construtor padrão — necessário para o Jackson desserializar */
    public UserResponseDTO() {}

    /** Construtor completo */
    public UserResponseDTO(Long id, String name, String email,
                           User.RiskProfile riskProfile, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.riskProfile = riskProfile;
        this.createdAt = createdAt;
    }

    // =========================================================================
    // MÉTODO DE FÁBRICA (FACTORY METHOD)
    // =========================================================================

    /**
     * Método de fábrica estático para converter uma entidade User em UserResponseDTO.
     *
     * Por que um método estático "from()" ao invés de fazer a conversão no Service?
     * → Single Responsibility: cada classe sabe como se converter.
     * → Reutilização: qualquer lugar pode chamar UserResponseDTO.from(user).
     * → Facilidade de manutenção: se a entidade mudar, só altera aqui.
     *
     * @param user a entidade User vinda do banco de dados
     * @return UserResponseDTO pronto para ser serializado em JSON pelo Jackson
     */
    public static UserResponseDTO from(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRiskProfile(),
                user.getCreatedAt()
                // password é INTENCIONALMENTE omitido aqui!
        );
    }

    // =========================================================================
    // Getters e Setters
    // =========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public User.RiskProfile getRiskProfile() { return riskProfile; }
    public void setRiskProfile(User.RiskProfile riskProfile) { this.riskProfile = riskProfile; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
