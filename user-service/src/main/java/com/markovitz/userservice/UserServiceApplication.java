package com.markovitz.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================================
 * CLASSE PRINCIPAL DO MICROSSERVIÇO USER-SERVICE
 * ============================================================================
 *
 * Esta é a porta de entrada da aplicação Spring Boot.
 * Quando você executa "mvn spring-boot:run" ou roda o JAR,
 * o Java inicia por aqui — pelo método main().
 *
 * @SpringBootApplication é uma anotação composta que ativa 3 coisas:
 *
 *   1. @SpringBootConfiguration
 *      → Marca esta classe como fonte de configurações Spring.
 *
 *   2. @EnableAutoConfiguration
 *      → O Spring Boot detecta automaticamente as dependências no classpath
 *        e configura beans padrão (ex: detecta H2 → configura DataSource,
 *        detecta RabbitMQ → configura ConnectionFactory, etc.)
 *
 *   3. @ComponentScan
 *      → Varre o pacote atual e subpacotes procurando classes anotadas com
 *        @Component, @Service, @Repository, @Controller, @RestController, etc.
 *        e as registra como beans gerenciados pelo Spring IoC Container.
 *
 * ============================================================================
 */
@SpringBootApplication
public class UserServiceApplication {

    /**
     * Ponto de entrada da JVM (Java Virtual Machine).
     *
     * SpringApplication.run() faz várias coisas:
     *   - Cria o ApplicationContext (container de IoC do Spring)
     *   - Registra todos os beans encontrados pelo @ComponentScan
     *   - Aplica as configurações do application.yml
     *   - Inicia o servidor web embutido (Tomcat)
     *   - Conecta ao RabbitMQ
     *   - Executa migrações/criação do banco H2
     *
     * @param args argumentos de linha de comando (pode passar --server.port=8082, etc.)
     */
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
