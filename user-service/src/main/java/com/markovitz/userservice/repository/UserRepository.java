package com.markovitz.userservice.repository;

import com.markovitz.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ============================================================================
 * REPOSITORY — Camada de Acesso ao Banco de Dados
 * ============================================================================
 *
 * O padrão Repository é responsável por isolar a lógica de acesso a dados
 * do restante da aplicação. O Service não sabe COMO os dados são buscados
 * (SQL, NoSQL, arquivo, API externa) — ele só chama o Repository.
 *
 * SPRING DATA JPA:
 * ─────────────────────────────────────────────────────────────────────────
 * Ao estender JpaRepository<Entidade, TipoDoId>, o Spring gera automaticamente
 * a implementação com os métodos CRUD básicos:
 *
 *   save(entity)           → INSERT ou UPDATE (decide baseado se tem ID)
 *   findById(id)           → SELECT WHERE id = ?  (retorna Optional)
 *   findAll()              → SELECT * FROM users
 *   deleteById(id)         → DELETE WHERE id = ?
 *   count()                → SELECT COUNT(*) FROM users
 *   existsById(id)         → SELECT 1 WHERE id = ? (boolean)
 *
 * QUERY METHODS (Métodos com nome "mágico"):
 * ─────────────────────────────────────────────────────────────────────────
 * O Spring Data JPA interpreta o NOME do método e gera o SQL automaticamente!
 *
 * Convenções:
 *   findBy{Campo}         → WHERE campo = ?
 *   findBy{Campo}And{Campo2} → WHERE campo = ? AND campo2 = ?
 *   existsBy{Campo}       → SELECT EXISTS(WHERE campo = ?)
 *   countBy{Campo}        → SELECT COUNT(*) WHERE campo = ?
 *   findBy{Campo}Containing → WHERE campo LIKE '%?%'
 *   findBy{Campo}OrderBy{Campo2}Asc → WHERE ... ORDER BY ... ASC
 *
 * ============================================================================
 *
 * @Repository → Anotação que marca a interface como componente de acesso a dados.
 * Pode ser omitida quando extendemos JpaRepository (o Spring já a detecta),
 * mas é boa prática incluir para deixar o código mais explícito.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca um usuário pelo email.
     *
     * Spring Data JPA interpreta este método como:
     *   SELECT * FROM users WHERE email = ?
     *
     * Retornamos Optional<User> (e não User diretamente) porque o usuário
     * pode não existir. Optional força o chamador a tratar este caso,
     * evitando NullPointerException.
     *
     * Uso:
     *   Optional<User> user = repository.findByEmail("joao@email.com");
     *   user.ifPresent(u -> System.out.println(u.getName()));
     *   user.orElseThrow(() -> new UserNotFoundException("Email não encontrado"));
     *
     * @param email o email a ser buscado
     * @return Optional contendo o User se encontrado, ou vazio se não existir
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica se já existe um usuário com o email informado.
     *
     * Spring Data JPA gera:
     *   SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)
     *
     * Mais eficiente que findByEmail() quando só precisamos saber SE existe,
     * pois não carrega o objeto inteiro do banco.
     *
     * Usado na validação ao registrar um novo usuário (evitar email duplicado).
     *
     * @param email o email a verificar
     * @return true se já existe um usuário com este email, false caso contrário
     */
    boolean existsByEmail(String email);
}
