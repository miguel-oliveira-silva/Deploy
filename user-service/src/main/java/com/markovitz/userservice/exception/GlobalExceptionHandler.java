package com.markovitz.userservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * GLOBAL EXCEPTION HANDLER — Tratamento Centralizado de Exceções
 * ============================================================================
 *
 * Sem este handler, uma exceção não tratada retornaria uma stacktrace Java
 * enorme ao cliente — péssima experiência e risco de segurança (expõe
 * detalhes internos da aplicação).
 *
 * Com @RestControllerAdvice, capturamos exceções em qualquer Controller
 * e retornamos respostas JSON padronizadas e amigáveis.
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 *   → Intercepta exceções em qualquer @RestController
 *   → Retorna automaticamente JSON (por causa do @ResponseBody)
 *
 * @ExceptionHandler(MinhaExcecao.class)
 *   → Diz ao Spring: "quando MinhaExcecao for lançada em qualquer Controller,
 *     execute este método"
 *
 * EXEMPLO DE RESPOSTA PADRONIZADA:
 * {
 *   "timestamp": "2024-01-15T14:30:00",
 *   "status": 404,
 *   "error": "Usuário não encontrado",
 *   "path": "/api/users/999"
 * }
 *
 * ============================================================================
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Trata erros de validação dos @RequestBody.
     *
     * Quando uma requisição com @Valid falha nas anotações de validação
     * (@NotBlank, @Email, @Size, etc.), o Spring lança
     * MethodArgumentNotValidException automaticamente.
     *
     * Este método captura essa exceção e retorna um JSON com todos os
     * campos inválidos e suas mensagens de erro.
     *
     * Exemplo de resposta:
     * {
     *   "timestamp": "2024-01-15T14:30:00",
     *   "status": 400,
     *   "errors": {
     *     "email": "O email deve ter um formato válido",
     *     "name": "O nome não pode ser vazio"
     *   }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        // Coleta todos os erros de campo em um Map<campo, mensagem>
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            // FieldError contém o nome do campo que falhou
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        // Monta o body da resposta
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value()); // 400
        response.put("errors", fieldErrors);

        return ResponseEntity.badRequest().body(response);
        //                    ↑ HTTP 400 Bad Request
    }

    /**
     * Trata o caso de usuário não encontrado.
     *
     * Quando UserService lança UserNotFoundException, este método
     * intercepta e retorna HTTP 404 com mensagem amigável.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(
            UserNotFoundException ex) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.NOT_FOUND.value()); // 404
        response.put("error", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Trata tentativa de cadastro com email já existente.
     *
     * HTTP 409 Conflict — indica que a requisição conflita com o
     * estado atual do recurso (email já cadastrado).
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.CONFLICT.value()); // 409
        response.put("error", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handler genérico para qualquer outra exceção não mapeada.
     *
     * É uma rede de segurança: qualquer erro inesperado retorna HTTP 500
     * com uma mensagem genérica (sem expor detalhes internos ao cliente).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value()); // 500
        response.put("error", "Ocorreu um erro interno no servidor");
        // ex.getMessage() NÃO é retornado ao cliente por segurança
        // mas é logado internamente
        System.err.println("[ERROR] Exceção não tratada: " + ex.getMessage());

        return ResponseEntity.internalServerError().body(response);
    }
}
