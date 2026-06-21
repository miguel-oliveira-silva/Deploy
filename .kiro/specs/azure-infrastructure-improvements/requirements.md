# Requirements Document

## Introduction

Este documento especifica os requisitos para melhorias na infraestrutura Azure do projeto Markovitz, que atualmente utiliza Terraform para provisionar 4 microsserviços Spring Boot em uma VM Ubuntu 22.04 (Standard_B2s). As melhorias visam tornar o processo de deploy mais resiliente, observável e confiável, sem quebrar a funcionalidade existente e respeitando as limitações de recursos da Azure for Students.

## Glossary

- **Bootstrap_System**: O mecanismo de cloud-init responsável por provisionar a aplicação na VM durante o primeiro boot (instalação de Docker, clone do repositório, build das imagens, inicialização dos containers)
- **Terraform_Orchestrator**: O conjunto de arquivos Terraform (main.tf, vm.tf, network.tf, etc.) que provisionam a infraestrutura Azure
- **Service_Container**: Qualquer um dos 4 microsserviços Spring Boot (user-service, asset-service, portfolio-service, notification-service) executando em container Docker
- **Infrastructure_Container**: PostgreSQL ou RabbitMQ executando em container Docker
- **Health_Endpoint**: Endpoint HTTP que retorna o status de saúde de um serviço (ex: /actuator/health)
- **Verification_Script**: Script automatizado que valida o estado pós-deploy da infraestrutura e aplicação
- **Bootstrap_Log**: Arquivo /var/log/markovitz-bootstrap.log contendo registro timestamped de todas operações do bootstrap
- **Maintenance_Script**: Scripts auxiliares para operações comuns de manutenção (restart, logs, update)
- **Pre_Deploy_Validator**: Validador que verifica pré-condições antes de executar terraform apply
- **Resource_Monitor**: Sistema de monitoramento básico que rastreia uso de CPU, memória e disco durante o bootstrap
- **Rollback_Agent**: Mecanismo que destrói recursos automaticamente caso o bootstrap falhe
- **Deployment_Report**: Documento gerado automaticamente com IP, endpoints, credenciais e status dos serviços

## Requirements

### Requirement 1: Cloud-Init com Retry Logic

**User Story:** Como operador de infraestrutura, eu quero que o bootstrap seja resiliente a falhas temporárias, para que problemas de rede ou timeout não causem falha completa do deploy.

#### Acceptance Criteria

1. WHEN a clone do repositório Git falhar com timeout, THE Bootstrap_System SHALL tentar novamente até 3 vezes com intervalo de 10 segundos entre tentativas
2. WHEN o build de uma imagem Docker falhar com erro de rede ou timeout, THE Bootstrap_System SHALL tentar novamente até 2 vezes para aquela imagem específica
3. WHEN o apt-get update ou apt-get install falhar, THE Bootstrap_System SHALL tentar novamente até 3 vezes com apt-get clean antes de cada retry
4. WHEN um retry for bem-sucedido, THE Bootstrap_System SHALL registrar no Bootstrap_Log o número de tentativas necessárias
5. WHEN todas as tentativas de retry falharem para operação Git clone, Docker build ou apt-get install, THE Bootstrap_System SHALL registrar erro fatal no Bootstrap_Log e interromper o processo

### Requirement 2: Build Sequencial Otimizado

**User Story:** Como operador de infraestrutura, eu quero que o build das imagens Docker seja otimizado para a VM B2s, para que o processo não falhe por Out of Memory.

#### Acceptance Criteria

1. THE Bootstrap_System SHALL construir as imagens Docker em grupos sequenciais para evitar consumo simultâneo de memória superior a 3GB
2. THE Bootstrap_System SHALL construir primeiro Infrastructure_Container (postgres, rabbitmq) antes de qualquer Service_Container
3. THE Bootstrap_System SHALL construir Service_Container em pares máximos de 2 simultaneamente (user-service + asset-service, depois portfolio-service + notification-service)
4. WHEN o uso de memória exceder 3.5GB durante o build, THE Bootstrap_System SHALL tentar aguardar 30 segundos antes de iniciar o próximo build
5. WHEN o mecanismo de aguardo de 30 segundos falhar ao ser acionado, THE Bootstrap_System SHALL continuar construindo sem aguardar
6. THE Bootstrap_System SHALL registrar no Bootstrap_Log o tempo de build e uso de memória de cada imagem

### Requirement 3: Health Checks Automáticos

**User Story:** Como operador de infraestrutura, eu quero que o bootstrap valide automaticamente que todos os serviços estão funcionando, para que eu possa confiar que o deploy foi bem-sucedido.

#### Acceptance Criteria

1. WHEN todos os containers estiverem rodando, THE Bootstrap_System SHALL aguardar até 120 segundos para que os Health_Endpoint respondam
2. THE Bootstrap_System SHALL verificar o Health_Endpoint de cada Service_Container a cada 10 segundos
3. WHEN um Health_Endpoint retornar status HTTP 200 com body contendo "UP", THE Bootstrap_System SHALL marcar aquele serviço como saudável no Bootstrap_Log
4. WHEN exatamente 4 Service_Container estiverem saudáveis, THE Bootstrap_System SHALL registrar "Bootstrap concluído com sucesso" no Bootstrap_Log
5. WHEN algum Health_Endpoint não responder após 120 segundos, THE Bootstrap_System SHALL registrar erro específico identificando qual serviço falhou

### Requirement 4: Logs Estruturados com Timestamps

**User Story:** Como operador de infraestrutura, eu quero logs estruturados e com timestamps, para que eu possa diagnosticar problemas e medir performance facilmente.

#### Acceptance Criteria

1. THE Bootstrap_System SHALL registrar todas operações no Bootstrap_Log no formato "[TIMESTAMP] [NÍVEL] [COMPONENTE] mensagem"
2. THE Bootstrap_System SHALL usar timestamps no formato ISO-8601 (YYYY-MM-DDTHH:MM:SS+00:00)
3. THE Bootstrap_System SHALL usar níveis: INFO para operações normais, WARN para retries, ERROR para falhas
4. THE Bootstrap_System SHALL identificar componentes: GIT, DOCKER, APT, HEALTHCHECK, SYSTEM
5. WHEN o bootstrap iniciar, THE Bootstrap_System SHALL tentar registrar linha de separação visual e timestamp de início
6. WHEN o registro de separador visual no início falhar, THE Bootstrap_System SHALL continuar o processo de bootstrap
7. WHEN o bootstrap terminar (sucesso ou falha), THE Bootstrap_System SHALL tentar registrar linha de separação visual, timestamp de término e duração total em segundos
8. WHEN o registro de separador visual no término falhar, THE Bootstrap_System SHALL continuar completando o processo

### Requirement 5: Script de Verificação Pós-Deploy

**User Story:** Como operador de infraestrutura, eu quero um script que valide automaticamente o deploy completo, para que eu possa confirmar rapidamente que tudo está funcionando.

#### Acceptance Criteria

1. THE Terraform_Orchestrator SHALL gerar um Verification_Script executável após terraform apply
2. THE Verification_Script SHALL validar que o IP público da VM responde a ping
3. THE Verification_Script SHALL validar que a porta SSH (22) está acessível
4. THE Verification_Script SHALL validar que todas as 4 portas de Service_Container (8081-8084) estão acessíveis
5. THE Verification_Script SHALL fazer requisição HTTP GET para cada Health_Endpoint e validar resposta "UP"
6. THE Verification_Script SHALL validar que Swagger UI está acessível em cada Service_Container (HTTP 200 em /swagger-ui.html)
7. THE Verification_Script SHALL validar que RabbitMQ Management UI está acessível (HTTP 200 em :15672)
8. WHEN todas as validações passarem, THE Verification_Script SHALL exibir mensagem "Deploy verificado com sucesso"
9. WHEN a exibição da mensagem de sucesso falhar, THE Verification_Script SHALL terminar com exit code 1
10. WHEN a mensagem de sucesso for exibida corretamente, THE Verification_Script SHALL terminar com exit code 0
11. WHEN alguma validação falhar, THE Verification_Script SHALL exibir erro específico, sugestão de diagnóstico e exit code 1

### Requirement 6: Outputs Terraform Melhorados

**User Story:** Como operador de infraestrutura, eu quero outputs Terraform mais informativos, para que eu possa acessar rapidamente logs, endpoints e comandos de diagnóstico.

#### Acceptance Criteria

1. THE Terraform_Orchestrator SHALL incluir output com comando direto para acessar Bootstrap_Log via SSH
2. THE Terraform_Orchestrator SHALL incluir output com links clicáveis para todos os Health_Endpoint
3. THE Terraform_Orchestrator SHALL incluir output com links clicáveis para todas as URLs Swagger UI
4. THE Terraform_Orchestrator SHALL incluir output com comando para verificar status dos containers Docker via SSH
5. THE Terraform_Orchestrator SHALL incluir output com tempo estimado até aplicação pronta (15-20 minutos)
6. THE Terraform_Orchestrator SHALL incluir output com caminho para o Verification_Script gerado

### Requirement 7: Validações Pré-Deploy

**User Story:** Como operador de infraestrutura, eu quero validações antes do terraform apply, para que eu identifique problemas de configuração antes de gastar créditos Azure.

#### Acceptance Criteria

1. THE Pre_Deploy_Validator SHALL verificar que terraform.tfvars existe antes de permitir terraform apply
2. THE Pre_Deploy_Validator SHALL verificar que todas as variáveis obrigatórias (db_password, rabbitmq_password, git_repo_url) têm valores não vazios
3. THE Pre_Deploy_Validator SHALL verificar que o arquivo de chave SSH especificado em ssh_public_key_path existe ou pode ser gerado
4. THE Pre_Deploy_Validator SHALL fazer teste de conectividade com git_repo_url (HTTP HEAD request) para confirmar que repositório existe
5. WHEN alguma validação pré-deploy falhar, THE Pre_Deploy_Validator SHALL exibir erro específico, ação corretiva e exit code 1
6. WHEN todas as validações pré-deploy passarem, THE Pre_Deploy_Validator SHALL exibir "Pré-condições validadas com sucesso" e permitir terraform apply

### Requirement 8: Scripts de Manutenção

**User Story:** Como operador de infraestrutura, eu quero scripts de manutenção prontos, para que eu possa realizar operações comuns sem precisar lembrar comandos complexos.

#### Acceptance Criteria

1. THE Terraform_Orchestrator SHALL gerar Maintenance_Script para reiniciar todos Service_Container sem rebuild
2. THE Terraform_Orchestrator SHALL gerar Maintenance_Script para visualizar logs de um Service_Container específico (mínimo 50 linhas, permitindo mais quando benéfico)
3. THE Terraform_Orchestrator SHALL gerar Maintenance_Script para atualizar aplicação (git pull + rebuild + restart)
4. THE Terraform_Orchestrator SHALL gerar Maintenance_Script para verificar uso de recursos (CPU, memória, disco) da VM
5. THE Maintenance_Script SHALL ser executável via SSH sem requerer conhecimento de Docker Compose
6. WHEN um Maintenance_Script for executado, THE Maintenance_Script SHALL registrar operação no Bootstrap_Log com timestamp

### Requirement 9: Monitoramento Básico Durante Bootstrap

**User Story:** Como operador de infraestrutura, eu quero visibilidade do uso de recursos durante o bootstrap, para que eu possa identificar gargalos e otimizar o processo.

#### Acceptance Criteria

1. THE Resource_Monitor SHALL registrar uso de CPU a cada 30 segundos durante o bootstrap no Bootstrap_Log
2. THE Resource_Monitor SHALL registrar uso de memória (total, usada, disponível) a cada 30 segundos durante o bootstrap no Bootstrap_Log
3. THE Resource_Monitor SHALL registrar uso de disco (/) a cada 60 segundos durante o bootstrap no Bootstrap_Log
4. WHILE bootstrap estiver em execução AND uso de memória for maior ou igual a 90%, THE Resource_Monitor SHALL registrar WARNING no Bootstrap_Log
5. WHEN o uso de disco exceder 80%, THE Resource_Monitor SHALL registrar WARNING no Bootstrap_Log
6. THE Resource_Monitor SHALL calcular e registrar pico de uso de CPU e memória ao final do bootstrap

### Requirement 10: Rollback Automático em Caso de Falha

**User Story:** Como operador de infraestrutura, eu quero rollback automático se o bootstrap falhar, para que eu não gaste créditos Azure com VM não funcional.

#### Acceptance Criteria

1. WHEN o bootstrap não completar com sucesso após 30 minutos, THE Rollback_Agent SHALL marcar o deploy como falho
2. WHEN o deploy for marcado como falho AND enable_auto_rollback estiver habilitado, THE Rollback_Agent SHALL aguardar 5 minutos antes de executar terraform destroy
3. WHEN o deploy for marcado como falho por rollback de falha de deployment AND a espera de 5 minutos completar, THE Rollback_Agent SHALL executar terraform destroy automaticamente
4. THE Rollback_Agent SHALL registrar no log local (workstation) o motivo da falha e timestamp do rollback
5. WHERE rollback automático estiver habilitado via variável terraform enable_auto_rollback=true, THE Rollback_Agent SHALL executar o destroy
6. WHEN rollback automático estiver desabilitado (enable_auto_rollback=false), THE Rollback_Agent SHALL apenas notificar sobre a falha sem executar destroy

### Requirement 11: Documentação Gerada Automaticamente

**User Story:** Como operador de infraestrutura, eu quero documentação gerada automaticamente após deploy, para que eu tenha referência rápida de todos os endpoints e credenciais.

#### Acceptance Criteria

1. THE Terraform_Orchestrator SHALL gerar Deployment_Report em formato Markdown após terraform apply bem-sucedido
2. THE Deployment_Report SHALL conter IP público da VM e comando SSH para acesso
3. THE Deployment_Report SHALL conter tabela com todos os endpoints (user-service, asset-service, portfolio-service, notification-service, RabbitMQ Management)
4. THE Deployment_Report SHALL conter links diretos para Health_Endpoint de cada Service_Container
5. THE Deployment_Report SHALL conter links diretos para Swagger UI de cada Service_Container
6. THE Deployment_Report SHALL conter credenciais do RabbitMQ (usuário e senha)
7. THE Deployment_Report SHALL conter seção de troubleshooting com comandos comuns de diagnóstico
8. THE Deployment_Report SHALL conter timestamp de criação e hash do commit Git do repositório deployado
9. THE Deployment_Report SHALL ser salvo em terraform/DEPLOYMENT-{timestamp}.md

### Requirement 12: Compatibilidade com Deploy Existente

**User Story:** Como operador de infraestrutura, eu quero que as melhorias sejam compatíveis com o deploy atual, para que eu não quebre funcionalidade existente.

#### Acceptance Criteria

1. THE Bootstrap_System SHALL manter compatibilidade com arquivo docker-compose.yml existente sem modificações
2. THE Terraform_Orchestrator SHALL manter estrutura de arquivos separados (main.tf, vm.tf, network.tf, ssh-key.tf, providers.tf, variables.tf, outputs.tf)
3. THE Terraform_Orchestrator SHALL manter todas as variáveis existentes em variables.tf com mesmos nomes e defaults
4. THE Bootstrap_System SHALL manter criação do arquivo .env com mesmas variáveis existentes
5. THE Terraform_Orchestrator SHALL manter configuração de rede existente (VNet, subnet, NSG, portas abertas)
6. THE Terraform_Orchestrator SHALL usar Standard_B2s como tipo de VM padrão
7. WHEN todas as novas features estiverem desabilitadas via flags, THE Bootstrap_System SHALL se comportar exatamente como versão anterior

### Requirement 13: Parser e Pretty-Printer para Logs Estruturados

**User Story:** Como operador de infraestrutura, eu quero ferramentas para processar logs estruturados, para que eu possa filtrar e analisar eventos específicos facilmente.

#### Acceptance Criteria

1. THE Terraform_Orchestrator SHALL gerar script Log_Parser que lê Bootstrap_Log e extrai eventos estruturados
2. THE Log_Parser SHALL parsear linhas no formato "[TIMESTAMP] [NÍVEL] [COMPONENTE] mensagem" em objeto estruturado
3. WHEN uma linha do Bootstrap_Log não seguir o formato esperado, THE Log_Parser SHALL retornar erro descritivo indicando linha inválida
4. THE Log_Parser SHALL suportar filtro por nível (INFO, WARN, ERROR)
5. THE Log_Parser SHALL suportar filtro por componente (GIT, DOCKER, APT, HEALTHCHECK, SYSTEM)
6. THE Log_Parser SHALL suportar filtro por intervalo de tempo (timestamp inicial e final)
7. THE Terraform_Orchestrator SHALL gerar script Log_Pretty_Printer que formata logs estruturados em saída legível
8. FOR ALL objetos estruturados válidos parseados, THE Log_Pretty_Printer SHALL formatar de volta para o formato original sem perda de informação (round-trip property)
9. THE Log_Pretty_Printer SHALL suportar opção de colorização por nível (INFO=verde, WARN=amarelo, ERROR=vermelho) quando output for terminal

### Requirement 14: Testes de Validação Incremental

**User Story:** Como desenvolvedor da infraestrutura, eu quero que cada melhoria possa ser testada isoladamente, para que eu possa validar progressivamente sem riscos.

#### Acceptance Criteria

1. THE Terraform_Orchestrator SHALL suportar variável feature_flags para habilitar/desabilitar melhorias individualmente
2. THE Bootstrap_System SHALL verificar feature_flags.retry_logic antes de executar retry logic
3. THE Bootstrap_System SHALL verificar feature_flags.sequential_build em todos os momentos antes de executar builds sequenciais
4. THE Bootstrap_System SHALL verificar feature_flags.health_checks antes de executar validação de health endpoints
5. THE Bootstrap_System SHALL verificar feature_flags.resource_monitoring antes de ativar Resource_Monitor
6. THE Bootstrap_System SHALL verificar feature_flags.auto_rollback antes de ativar Rollback_Agent
7. WHEN uma feature estiver desabilitada via feature_flags, THE Bootstrap_System SHALL registrar no Bootstrap_Log que aquela feature foi pulada
8. WHEN todas as features estiverem desabilitadas, THE Bootstrap_System SHALL funcionar exatamente como implementação original

