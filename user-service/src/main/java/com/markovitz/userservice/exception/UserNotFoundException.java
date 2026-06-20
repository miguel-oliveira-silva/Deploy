package com.markovitz.userservice.exception;

/**
 * ============================================================================
 * UserNotFoundException — Exceção para usuário não encontrado
 * ============================================================================
 *
 * Lançada quando se busca um usuário pelo ID ou email e ele não existe no banco.
 *
 * Estende RuntimeException (unchecked exception):
 *   → Não precisa ser declarada na assinatura dos métodos com "throws"
 *   → Não obriga o chamador a envolver em try/catch
 *   → É a escolha correta para erros de negócio que interrompem o fluxo
 *
 * O GlobalExceptionHandler captura esta exceção e retorna HTTP 404 Not Found.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message); // passa a mensagem para a classe pai (RuntimeException)
    }
}
