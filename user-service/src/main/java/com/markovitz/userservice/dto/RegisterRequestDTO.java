package com.markovitz.userservice.dto;

import com.markovitz.userservice.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * ============================================================================
 * DTO — Data Transfer Object (Objeto de Transferência de Dados)
 * ============================================================================
 *
 * Por que usar DTOs ao invés de passar a Entidade diretamente?
 *
 * PROBLEMA sem DTO:
 *   Se você expõe a entidade User diretamente na API:
 *   1. O campo "password" seria retornado no JSON (GRAVE falha de segurança!)
 *   2. O cliente poderia tentar definir campos internos como "id" ou "createdAt"
 *   3. Mudanças internas na entidade quebrariam o contrato da API
 *
 * SOLUÇÃO com DTOs:
 *   - RequestDTO  → dados que o CLIENTE envia para o servidor (entrada)
 *   - ResponseDTO → dados que o SERVIDOR retorna para o cliente (saída)
 *   - Você controla exatamente o que entra e o que sai da API
 *
 * ANOTAÇÕES DE VALIDAÇÃO (Bean Validation / Jakarta Validation):
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Quando o Controller recebe uma requisição com @Valid, o Spring valida
 * automaticamente os campos antes de chamar o método do Controller.
 * Se a validação falhar, retorna HTTP 400 Bad Request com os erros.
 *
 * @NotBlank  → string não pode ser null, vazia ou só espaços
 * @NotNull   → valor não pode ser null (funciona para qualquer tipo)
 * @Email     → deve ser um email válido (ex: "usuario@dominio.com")
 * @Size      → define tamanho mínimo e/ou máximo
 *
 * ============================================================================
 */
public class RegisterRequestDTO {

    /**
     * Nome completo do usuário.
     *
     * @NotBlank → impede strings vazias ou com apenas espaços.
     *   message = "..." → mensagem de erro retornada quando a validação falha.
     *
     * @Size → define o tamanho máximo do nome para evitar dados muito grandes.
     */
    @NotBlank(message = "O nome não pode ser vazio")
    @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
    private String name;

    /**
     * Email do usuário — também serve como login no sistema.
     *
     * @Email → valida o formato do email (deve ter "@" e domínio válido)
     * Exemplos válidos: "joao@gmail.com", "ana.lima@empresa.com.br"
     * Exemplos inválidos: "joao", "joao@", "@gmail.com"
     */
    @NotBlank(message = "O email não pode ser vazio")
    @Email(message = "O email deve ter um formato válido")
    private String email;

    /**
     * Senha do usuário.
     *
     * @Size(min=6) → exige pelo menos 6 caracteres.
     */
    @NotBlank(message = "A senha não pode ser vazia")
    @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
    private String password;

    /**
     * Perfil de risco do investidor.
     *
     * @NotNull → o perfil de risco é obrigatório (define a estratégia de otimização).
     *
     * O cliente deve enviar um dos valores do enum:
     *   "CONSERVADOR", "MODERADO" ou "AGRESSIVO"
     *
     * Jackson converte automaticamente o String do JSON para o enum correspondente.
     */
    @NotNull(message = "O perfil de risco não pode ser nulo")
    private User.RiskProfile riskProfile;

    // =========================================================================
    // Getters e Setters (gerados manualmente — sem Lombok)
    // =========================================================================
    // Jackson (serialização/desserialização JSON) e o Spring precisam de getters/setters.

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public User.RiskProfile getRiskProfile() { return riskProfile; }
    public void setRiskProfile(User.RiskProfile riskProfile) { this.riskProfile = riskProfile; }
}
