package com.markovitz.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal do microsserviço user-service.
 *
 * @SpringBootApplication configura automaticamente:
 * - Component scan nos subpacotes
 * - Auto-configuração de beans (DataSource, RabbitMQ, etc)
 * - Servidor web embutido (Tomcat)
 */
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
