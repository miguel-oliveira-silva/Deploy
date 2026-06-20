package com.markovitz.userservice.service;

import com.markovitz.userservice.config.RabbitMQConfig;
import com.markovitz.userservice.dto.RegisterRequestDTO;
import com.markovitz.userservice.dto.UserResponseDTO;
import com.markovitz.userservice.entity.User;
import com.markovitz.userservice.event.UserRegisteredEvent;
import com.markovitz.userservice.exception.EmailAlreadyExistsException;
import com.markovitz.userservice.exception.UserNotFoundException;
import com.markovitz.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ============================================================================
 * SERVICE — Camada de Lógica de Negócio
 * ============================================================================
 *
 * O Service é o coração da aplicação. Ele:
 *   - Implementa as regras de negócio
 *   - Coordena Repository (banco de dados) e outros componentes
 *   - Publica eventos assíncronos no RabbitMQ
 *   - É chamado pelo Controller
 *
 * ARQUITETURA EM CAMADAS (Layered Architecture):
 * ─────────────────────────────────────────────────────────────────────────
 *
 *   Controller  →  Service  →  Repository  →  Banco de Dados
 *       ↑              ↑            ↑
 *   HTTP/REST    Regras de      JPA/Hibernate
 *               Negócio
 *
 * Cada camada só conhece a camada imediatamente abaixo.
 * Controller não chama Repository diretamente — passa pelo Service.
 *
 * INJEÇÃO DE DEPENDÊNCIA (Dependency Injection — DI):
 * ─────────────────────────────────────────────────────────────────────────
 *
 * O Spring injeta as dependências automaticamente via construtor.
 * Isso é chamado de Constructor Injection — a melhor prática:
 *   1. Imutabilidade: campos final não podem ser reatribuídos
 *   2. Facilita testes unitários (não precisa do Spring no teste)
 *   3. Detecta dependências circulares em tempo de inicialização
 *
 * LOGGING — SLF4J:
 * ─────────────────────────────────────────────────────────────────────────
 * SLF4J (Simple Logging Facade for Java) é uma fachada de logging.
 * O Spring Boot usa Logback como implementação por padrão.
 *
 * Níveis de log (do mais ao menos verboso):
 *   TRACE → muito detalhado (quase nunca usado)
 *   DEBUG → depuração (detalhes de desenvolvimento)
 *   INFO  → informações importantes do fluxo normal
 *   WARN  → situações inesperadas mas não críticas
 *   ERROR → erros que precisam de atenção
 *
 * @Service → marca esta classe como componente de serviço.
 * O @ComponentScan do Spring encontra e registra como bean no contexto.
 *
 * ============================================================================
 */
@Service // Spring: registra como bean de serviço
public class UserService {

    /**
     * LOGGER — para registrar eventos e informações da aplicação
     *
     * LoggerFactory.getLogger(UserService.class) cria um logger
     * identificado pelo nome da classe, que aparece nos logs.
     *
     * Uso:
     *   log.info("mensagem")        → para informações normais
     *   log.debug("detalhe: {}", valor) → {} é substituído pelo valor (lazy)
     *   log.error("erro: {}", ex.getMessage()) → para erros
     */
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /**
     * Repositório para operações no banco de dados de usuários.
     * Injetado pelo Spring via construtor.
     */
    private final UserRepository userRepository;

    /**
     * Template do RabbitMQ para publicar mensagens nas filas.
     * Injetado pelo Spring — configurado em RabbitMQConfig.
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * Construtor para injeção de dependências.
     *
     * O Spring detecta este construtor e injeta automaticamente:
     *   - UserRepository → implementação gerada pelo Spring Data JPA
     *   - RabbitTemplate → configurado em RabbitMQConfig
     *
     * Quando a classe tem apenas UM construtor, o @Autowired é opcional.
     */
    public UserService(UserRepository userRepository, RabbitTemplate rabbitTemplate) {
        this.userRepository = userRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    // =========================================================================
    // MÉTODOS DE NEGÓCIO
    // =========================================================================

    /**
     * Registra um novo usuário no sistema.
     *
     * FLUXO:
     * 1. Verifica se o email já está cadastrado
     * 2. Constrói a entidade User a partir do DTO
     * 3. Salva no banco de dados
     * 4. Publica evento "user.registered" no RabbitMQ (assíncrono!)
     * 5. Retorna o DTO de resposta
     *
     * @Transactional garante que as operações de banco são atômicas:
     *   - Se o save() funcionar mas algo falhar depois → banco faz ROLLBACK
     *   - Garante consistência dos dados
     *
     * NOTA: A publicação no RabbitMQ acontece FORA da transação (depois do commit).
     * Em produção usaríamos "Outbox Pattern" para garantir que o evento seja
     * publicado mesmo em caso de falha do RabbitMQ.
     *
     * @param requestDTO dados do novo usuário vindos da requisição
     * @return DTO com os dados do usuário criado (sem a senha)
     * @throws EmailAlreadyExistsException se o email já estiver cadastrado
     */
    @Transactional // Garante atomicidade — ou tudo funciona, ou nada é salvo
    public UserResponseDTO register(RegisterRequestDTO requestDTO) {

        log.info("Iniciando cadastro de novo usuário com email: {}", requestDTO.getEmail());

        // =====================================================================
        // REGRA DE NEGÓCIO: Email deve ser único
        // =====================================================================
        // Verificamos ANTES de tentar inserir para dar uma mensagem clara.
        // Se não verificássemos, o banco lançaria uma exceção genérica de
        // violação de constraint UNIQUE — difícil de tratar.
        if (userRepository.existsByEmail(requestDTO.getEmail())) {
            log.warn("Tentativa de cadastro com email já existente: {}", requestDTO.getEmail());
            throw new EmailAlreadyExistsException(
                    "O email '" + requestDTO.getEmail() + "' já está cadastrado no sistema"
            );
        }

        // =====================================================================
        // CONSTRUÇÃO DA ENTIDADE — Converte DTO → Entity usando Builder
        // =====================================================================
        // Usamos o Builder pattern para construir o objeto de forma legível.
        // O Builder é definido manualmente dentro da classe User.
        User user = User.builder()
                .name(requestDTO.getName())
                .email(requestDTO.getEmail())
                .password(requestDTO.getPassword()) // Em produção: hash com BCrypt!
                .riskProfile(requestDTO.getRiskProfile())
                // createdAt é preenchido automaticamente pelo @PrePersist
                .build();

        // =====================================================================
        // PERSISTÊNCIA — Salva no banco via JPA
        // =====================================================================
        // save() → faz INSERT no banco e retorna a entidade com o ID gerado
        // O ID vem do AUTO_INCREMENT do H2 e é preenchido na entidade retornada.
        User savedUser = userRepository.save(user);
        log.info("Usuário salvo com sucesso. ID gerado: {}", savedUser.getId());

        // =====================================================================
        // COMUNICAÇÃO ASSÍNCRONA — Publica evento no RabbitMQ
        // =====================================================================
        publishUserRegisteredEvent(savedUser);

        // =====================================================================
        // RESPOSTA — Converte Entity → DTO de Resposta
        // =====================================================================
        // Usamos o método estático "from()" do DTO para fazer a conversão.
        // A senha é OMITIDA nesta conversão (ver UserResponseDTO.from()).
        return UserResponseDTO.from(savedUser);
    }

    /**
     * Busca um usuário pelo ID.
     *
     * @param id o ID do usuário
     * @return DTO com os dados do usuário
     * @throws UserNotFoundException se o usuário não for encontrado
     */
    @Transactional(readOnly = true)
    // readOnly=true → otimização: JPA não precisa monitorar mudanças (dirty checking)
    // Isso melhora performance em operações que só leem dados.
    public UserResponseDTO findById(Long id) {

        log.debug("Buscando usuário por ID: {}", id);

        // findById() retorna Optional<User>
        // orElseThrow() → se o Optional estiver vazio, lança a exceção
        // Isso é mais elegante que verificar if(user == null)
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado com ID: {}", id);
                    return new UserNotFoundException("Usuário com ID " + id + " não encontrado");
                });

        return UserResponseDTO.from(user);
    }

    /**
     * Lista todos os usuários cadastrados.
     *
     * Em produção, você implementaria paginação (Pageable) para não
     * carregar milhões de registros de uma vez.
     *
     * @return lista de DTOs de todos os usuários
     */
    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {

        log.debug("Listando todos os usuários");

        // findAll() retorna List<User>
        // .stream() converte para Stream para usar operações funcionais
        // .map(UserResponseDTO::from) → aplica o método from() em cada User
        //   "UserResponseDTO::from" é uma method reference → equivale a: user -> UserResponseDTO.from(user)
        // .toList() → coleta o resultado em uma List imutável
        return userRepository.findAll()
                .stream()
                .map(UserResponseDTO::from)
                .toList();
    }

    /**
     * Atualiza o perfil de risco de um usuário.
     *
     * O perfil de risco é crucial na Teoria de Markowitz:
     * define qual ponto da fronteira eficiente é o "ótimo" para o investidor.
     *
     * @param id          ID do usuário
     * @param riskProfile novo perfil de risco
     * @return DTO com os dados atualizados
     */
    @Transactional
    public UserResponseDTO updateRiskProfile(Long id, User.RiskProfile riskProfile) {

        log.info("Atualizando perfil de risco do usuário {} para {}", id, riskProfile);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(
                        "Usuário com ID " + id + " não encontrado"));

        user.setRiskProfile(riskProfile);

        // save() → faz UPDATE (o objeto já tem ID, então JPA sabe que é uma atualização)
        // Dentro de @Transactional, JPA faz "dirty checking" e poderia atualizar
        // automaticamente sem chamar save(), mas chamamos explicitamente por clareza.
        User updatedUser = userRepository.save(user);

        log.info("Perfil de risco atualizado com sucesso para usuário {}", id);
        return UserResponseDTO.from(updatedUser);
    }

    // =========================================================================
    // MÉTODOS PRIVADOS — Lógica auxiliar
    // =========================================================================

    /**
     * Publica o evento de usuário registrado no RabbitMQ.
     *
     * Este método é privado — é um detalhe de implementação do Service.
     * O Controller não precisa saber COMO o evento é publicado.
     *
     * COMUNICAÇÃO ASSÍNCRONA:
     * convertAndSend() é uma operação de "fire and forget":
     *   - Serializa o evento em JSON
     *   - Envia para o exchange do RabbitMQ
     *   - NÃO aguarda confirmação de que o notification-service processou
     *   - O método retorna imediatamente
     *
     * Isso é a essência da comunicação ASSÍNCRONA!
     * O user-service não depende do notification-service estar online.
     *
     * @param user o usuário recém-cadastrado
     */
    private void publishUserRegisteredEvent(User user) {
        try {
            // Cria o evento com os dados do usuário
            UserRegisteredEvent event = UserRegisteredEvent.from(user);

            // Publica no RabbitMQ:
            //   Argumento 1: exchange   → "markovitz.exchange" (definido em RabbitMQConfig)
            //   Argumento 2: routingKey → "user.registered" (direciona para a fila certa)
            //   Argumento 3: message    → o objeto event (serializado em JSON pelo conversor)
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,               // para qual exchange enviar
                    RabbitMQConfig.USER_REGISTERED_ROUTING_KEY, // com qual routing key
                    event                                        // o evento serializado em JSON
            );

            log.info("✉ Evento 'user.registered' publicado no RabbitMQ para o usuário ID: {}",
                    user.getId());

        } catch (Exception e) {
            // IMPORTANTE: Se o RabbitMQ estiver offline, NÃO deixamos o cadastro falhar!
            // Logamos o erro mas o usuário foi cadastrado com sucesso no banco.
            // Em produção, usaríamos o "Outbox Pattern" para garantir entrega.
            log.error("Falha ao publicar evento no RabbitMQ para usuário {}: {}",
                    user.getId(), e.getMessage());
        }
    }
}
