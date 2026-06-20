package com.markovitz.userservice.exception;

/**
 * ============================================================================
 * EmailAlreadyExistsException — Exceção para email duplicado
 * ============================================================================
 *
 * Lançada quando se tenta registrar um usuário com um email que já existe
 * no banco de dados.
 *
 * O GlobalExceptionHandler captura esta exceção e retorna HTTP 409 Conflict.
 *
 * HTTP 409 Conflict é semanticamente correto aqui porque:
 *   → A requisição em si é válida (dados corretos)
 *   → Mas conflita com o estado atual do servidor (email já cadastrado)
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
