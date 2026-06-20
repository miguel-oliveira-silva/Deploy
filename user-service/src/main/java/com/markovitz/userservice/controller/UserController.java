package com.markovitz.userservice.controller;

import com.markovitz.userservice.dto.RegisterRequestDTO;
import com.markovitz.userservice.dto.UserResponseDTO;
import com.markovitz.userservice.entity.User;
import com.markovitz.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================================
 * CONTROLLER — Camada de API REST
 * ============================================================================
 *
 * O Controller é a camada mais externa da aplicação — é quem recebe as
 * requisições HTTP do mundo externo e as direciona para o Service.
 *
 * RESPONSABILIDADES DO CONTROLLER:
 *   1. Mapear URLs para métodos Java (@GetMapping, @PostMapping, etc.)
 *   2. Extrair dados da requisição (@RequestBody, @PathVariable, @RequestParam)
 *   3. Validar a entrada (@Valid)
 *   4. Chamar o Service para processar
 *   5. Retornar a resposta com o status HTTP correto (ResponseEntity)
 *
 * O Controller NÃO deve conter lógica de negócio — isso é responsabilidade
 * do Service. O Controller só "traduz" HTTP para Java e Java para HTTP.
 *
 * ANOTAÇÕES PRINCIPAIS:
 * ─────────────────────────────────────────────────────────────────────────
 *
 * @RestController
 *   = @Controller + @ResponseBody
 *   → @Controller: registra como bean controlador
 *   → @ResponseBody: os métodos retornam dados (JSON) e não nomes de views (Thymeleaf/JSP)
 *
 * @RequestMapping("/api/users")
 *   → Define o prefixo de URL para todos os endpoints desta classe.
 *   → Todos os endpoints começarão com "/api/users"
 *
 * CONVENÇÃO DE URLs REST:
 *   Recurso: /api/users
 *   GET    /api/users              → listar todos
 *   POST   /api/users/register     → criar novo
 *   GET    /api/users/{id}         → buscar por ID
 *   PUT    /api/users/{id}/...     → atualizar parcialmente
 *
 * CÓDIGOS HTTP COMUNS:
 *   200 OK          → sucesso geral (GET, PUT)
 *   201 Created     → recurso criado com sucesso (POST)
 *   400 Bad Request → dados inválidos (validação falhou)
 *   404 Not Found   → recurso não encontrado
 *   409 Conflict    → conflito (email duplicado)
 *   500 Server Error→ erro interno
 *
 * ============================================================================
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuários", description = "Cadastro e gestão de investidores")
public class UserController {

    /** Logger para registrar informações das requisições */
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    /**
     * Injeção do Service via construtor.
     * O Controller delega TODA a lógica de negócio para o Service.
     */
    private final UserService userService;

    /**
     * Construtor para injeção de dependência.
     * O Spring injeta automaticamente a implementação do UserService.
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // =========================================================================
    // ENDPOINT 1: Registrar novo usuário
    // =========================================================================

    /**
     * POST /api/users/register
     *
     * Cria um novo usuário no sistema.
     *
     * EXEMPLO DE REQUISIÇÃO:
     * POST http://localhost:8081/api/users/register
     * Content-Type: application/json
     * {
     *   "name": "João Silva",
     *   "email": "joao@email.com",
     *   "password": "senha123",
     *   "riskProfile": "MODERADO"
     * }
     *
     * EXEMPLO DE RESPOSTA (HTTP 201):
     * {
     *   "id": 1,
     *   "name": "João Silva",
     *   "email": "joao@email.com",
     *   "riskProfile": "MODERADO",
     *   "createdAt": "2024-01-15T14:30:00"
     * }
     *
     * @PostMapping → mapeia requisições HTTP POST para este método
     *
     * @RequestBody → extrai o corpo JSON da requisição e converte para RegisterRequestDTO
     *   Jackson (biblioteca JSON do Spring) faz a conversão automaticamente.
     *
     * @Valid → dispara a validação das anotações do DTO (@NotBlank, @Email, etc.)
     *   Se falhar, lança MethodArgumentNotValidException (tratada pelo GlobalExceptionHandler)
     *
     * ResponseEntity<T> → permite controlar o status HTTP e o body da resposta.
     *   .status(HttpStatus.CREATED) → HTTP 201
     *   .body(dto)                  → serializa o DTO em JSON
     */
    @Operation(summary = "Registrar novo usuário", description = "Cria um novo investidor e publica evento 'user.registered' no RabbitMQ")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos (validação falhou)"),
        @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO requestDTO) {

        log.info("Requisição de registro recebida para: {}", requestDTO.getEmail());

        UserResponseDTO response = userService.register(requestDTO);

        // HTTP 201 Created → indica que um novo recurso foi criado com sucesso
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // ENDPOINT 2: Buscar usuário por ID
    // =========================================================================

    /**
     * GET /api/users/{id}
     *
     * Busca um usuário pelo ID.
     *
     * EXEMPLO DE REQUISIÇÃO:
     * GET http://localhost:8081/api/users/1
     *
     * @GetMapping("/{id}") → mapeia GET /api/users/{id}
     *   O {id} é uma variável de caminho (path variable)
     *
     * @PathVariable Long id → extrai o valor {id} da URL e injeta no parâmetro
     *   Ex: GET /api/users/42 → id = 42L
     */
    @Operation(summary = "Buscar usuário por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(
            @Parameter(description = "ID do usuário") @PathVariable Long id) {

        log.debug("Requisição para buscar usuário ID: {}", id);

        UserResponseDTO response = userService.findById(id);

        // ResponseEntity.ok() é um shortcut para .status(HttpStatus.OK).body(response)
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // ENDPOINT 3: Listar todos os usuários
    // =========================================================================

    /**
     * GET /api/users
     *
     * Lista todos os usuários cadastrados.
     *
     * EXEMPLO DE REQUISIÇÃO:
     * GET http://localhost:8081/api/users
     *
     * EXEMPLO DE RESPOSTA (HTTP 200):
     * [
     *   { "id": 1, "name": "João", ... },
     *   { "id": 2, "name": "Ana", ... }
     * ]
     *
     * Em produção: adicionar paginação com @RequestParam int page, int size
     * e retornar Page<UserResponseDTO> ao invés de List<>.
     */
    @Operation(summary = "Listar todos os usuários")
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> findAll() {

        log.debug("Requisição para listar todos os usuários");

        List<UserResponseDTO> users = userService.findAll();

        return ResponseEntity.ok(users);
    }

    // =========================================================================
    // ENDPOINT 4: Atualizar perfil de risco
    // =========================================================================

    /**
     * PUT /api/users/{id}/risk-profile
     *
     * Atualiza o perfil de risco do investidor.
     * Este endpoint é importante pois o perfil de risco influencia
     * diretamente a otimização da carteira pelo portfolio-service.
     *
     * EXEMPLO DE REQUISIÇÃO:
     * PUT http://localhost:8081/api/users/1/risk-profile
     * Content-Type: application/json
     * "AGRESSIVO"
     *
     * @PutMapping → mapeia requisições HTTP PUT (atualização de recurso)
     *
     * @RequestBody User.RiskProfile → extrai o enum do body JSON.
     *   O Jackson converte a string "AGRESSIVO" para User.RiskProfile.AGRESSIVO.
     */
    @Operation(summary = "Atualizar perfil de risco", description = "Valores aceitos: CONSERVADOR, MODERADO, AGRESSIVO")
    @ApiResponse(responseCode = "200", description = "Perfil atualizado")
    @PutMapping("/{id}/risk-profile")
    public ResponseEntity<UserResponseDTO> updateRiskProfile(
            @Parameter(description = "ID do usuário") @PathVariable Long id,
            @RequestBody User.RiskProfile riskProfile) {

        log.info("Requisição para atualizar perfil de risco do usuário {} para {}", id, riskProfile);

        UserResponseDTO response = userService.updateRiskProfile(id, riskProfile);

        return ResponseEntity.ok(response);
    }
}
