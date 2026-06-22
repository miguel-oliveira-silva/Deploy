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
 * Service - camada de lógica de negócio.
 *
 * Responsabilidades:
 * - Implementar regras de negócio
 * - Coordenar Repository e RabbitMQ
 * - Publicar eventos assíncronos
 *
 * Arquitetura: Controller -> Service -> Repository -> Banco
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    public UserService(UserRepository userRepository, RabbitTemplate rabbitTemplate) {
        this.userRepository = userRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    // Métodos de negócio

    /**
     * Registra um novo usuário no sistema.
     *
     * Fluxo:
     * 1. Verifica se o email já está cadastrado
     * 2. Salva no banco de dados
     * 3. Publica evento "user.registered" no RabbitMQ
     * 4. Retorna o DTO de resposta
     */
    @Transactional
    public UserResponseDTO register(RegisterRequestDTO requestDTO) {

        log.info("Iniciando cadastro de novo usuário com email: {}", requestDTO.getEmail());

        // Verifica se email já existe
        if (userRepository.existsByEmail(requestDTO.getEmail())) {
            log.warn("Tentativa de cadastro com email já existente: {}", requestDTO.getEmail());
            throw new EmailAlreadyExistsException(
                    "O email '" + requestDTO.getEmail() + "' já está cadastrado no sistema"
            );
        }

        // Constrói e salva a entidade
        User user = User.builder()
                .name(requestDTO.getName())
                .email(requestDTO.getEmail())
                .password(requestDTO.getPassword()) // Em produção: hash com BCrypt!
                .riskProfile(requestDTO.getRiskProfile())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Usuário salvo com sucesso. ID gerado: {}", savedUser.getId());

        // Publica evento no RabbitMQ
        publishUserRegisteredEvent(savedUser);

        return UserResponseDTO.from(savedUser);
    }

    /**
     * Busca um usuário pelo ID.
     */
    @Transactional(readOnly = true)
    public UserResponseDTO findById(Long id) {

        log.debug("Buscando usuário por ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado com ID: {}", id);
                    return new UserNotFoundException("Usuário com ID " + id + " não encontrado");
                });

        return UserResponseDTO.from(user);
    }

    /**
     * Lista todos os usuários cadastrados.
     */
    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {

        log.debug("Listando todos os usuários");

        return userRepository.findAll()
                .stream()
                .map(UserResponseDTO::from)
                .toList();
    }

    /**
     * Atualiza o perfil de risco de um usuário.
     */
    @Transactional
    public UserResponseDTO updateRiskProfile(Long id, User.RiskProfile riskProfile) {

        log.info("Atualizando perfil de risco do usuário {} para {}", id, riskProfile);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(
                        "Usuário com ID " + id + " não encontrado"));

        user.setRiskProfile(riskProfile);
        User updatedUser = userRepository.save(user);

        log.info("Perfil de risco atualizado com sucesso para usuário {}", id);
        return UserResponseDTO.from(updatedUser);
    }

    // Métodos privados

    /**
     * Publica o evento de usuário registrado no RabbitMQ.
     * Comunicação assíncrona - "fire and forget".
     */
    private void publishUserRegisteredEvent(User user) {
        try {
            UserRegisteredEvent event = UserRegisteredEvent.from(user);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.USER_REGISTERED_ROUTING_KEY,
                    event
            );

            log.info("✉ Evento 'user.registered' publicado no RabbitMQ para o usuário ID: {}",
                    user.getId());

        } catch (Exception e) {
            // Se o RabbitMQ estiver offline, não deixamos o cadastro falhar
            log.error("Falha ao publicar evento no RabbitMQ para usuário {}: {}",
                    user.getId(), e.getMessage());
        }
    }
}
