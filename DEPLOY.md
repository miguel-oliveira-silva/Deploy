# DEPLOY.md — Documentação de DevOps (Sistema Markovitz)

Este documento descreve a técnica de implantação automatizada usada no projeto,
atendendo ao requisito da disciplina **Ambiente de Desenvolvimento e Operações**.

## 1. Visão geral da técnica escolhida

A técnica utilizada é **Infraestrutura como Código (IaC) com Terraform + Cloud-Init
+ Docker Compose**, implantada na nuvem **Microsoft Azure**.

```
terraform apply
      │
      ▼
┌─────────────────────────────┐
│  Azure Resource Group       │
│  ┌─────────────────────────┐│
│  │ VNet + Subnet + NSG     ││  ← rede e firewall
│  │ IP Público              ││
│  │                         ││
│  │  ┌─────────────────────┐││
│  │  │  VM Ubuntu 22.04    │││
│  │  │  (cloud-init roda    │││
│  │  │   no primeiro boot)  │││
│  │  │                      │││
│  │  │  1. instala Docker   │││
│  │  │  2. clona o repo Git │││
│  │  │  3. gera .env        │││
│  │  │  4. docker compose   │││
│  │  │     up -d --build    │││
│  │  │                      │││
│  │  │  ┌────────────────┐  │││
│  │  │  │ PostgreSQL      │  │││
│  │  │  │ RabbitMQ        │  │││
│  │  │  │ user-service    │  │││
│  │  │  │ asset-service   │  │││
│  │  │  │ portfolio-svc   │  │││
│  │  │  │ notification-svc│  │││
│  │  │  └────────────────┘  │││
│  │  └─────────────────────┘││
│  └─────────────────────────┘│
└─────────────────────────────┘
```

Por que essa combinação?

- **Terraform**: declara a infraestrutura (VM, rede, IPs, regras de firewall) em
  arquivos de texto versionáveis. Rodar `terraform apply` recria a infraestrutura
  inteira do zero, de forma idêntica, em qualquer máquina/conta Azure — sem
  passos manuais no portal.
- **Cloud-Init**: mecanismo padrão de provisionamento que o Azure injeta na VM no
  primeiro boot. Substitui a necessidade de entrar via SSH e digitar comandos —
  a VM "se configura sozinha" assim que nasce.
- **Docker Compose**: orquestra os 4 microsserviços + PostgreSQL + RabbitMQ como
  containers, todos descritos em um único `docker-compose.yml` versionado junto
  do código. Isso garante que o ambiente da VM seja idêntico ao que qualquer
  integrante do grupo roda localmente.

## 2. Como funciona, passo a passo

1. O grupo configura `terraform/terraform.tfvars` com a senha do banco, do
   RabbitMQ e a URL do repositório Git.
2. Ao rodar `terraform apply`, o Terraform:
   - Cria um Resource Group, Virtual Network, Subnet, IP público e um Network
     Security Group (regras de firewall liberando SSH e as portas das APIs).
   - Cria a VM Ubuntu 22.04, anexando um script **cloud-init** como `custom_data`.
3. No primeiro boot, o cloud-init:
   - Instala o Docker Engine e o plugin `docker compose`.
   - Clona o repositório Git do projeto (`git clone --depth 1`).
   - Gera um arquivo `.env` na raiz do projeto com as credenciais vindas das
     variáveis do Terraform (nunca hardcoded no `docker-compose.yml`).
   - Executa `docker compose up -d --build`, que builda as 4 imagens (multi-stage
     Dockerfile: Maven compila o `.jar`, depois uma imagem JRE enxuta roda a
     aplicação) e sobe todos os containers na rede interna `markovitz-net`.
4. O PostgreSQL sobe com um script de inicialização (`init-multi-db.sh`) que cria
   automaticamente os 4 bancos lógicos (`userdb`, `assetdb`, `portfoliodb`,
   `notificationdb`), mantendo o princípio "database per service" que o projeto
   já seguia com H2 — agora com persistência real em disco.
5. Ao final, `terraform output` mostra o IP público da VM e as URLs prontas de
   cada microsserviço.

## 3. Banco de dados: de H2 (memória) para PostgreSQL (persistente)

O código original usava H2 em memória (dados perdidos a cada reinício). Para
atender ao requisito de persistência com banco relacional real, os 4 serviços
foram adaptados:

- `pom.xml`: dependência `com.h2database:h2` movida para escopo `test`;
  adicionada a dependência `org.postgresql:postgresql` em escopo `runtime`.
- `application.yml`: `datasource.url` passou a apontar para PostgreSQL via
  variáveis de ambiente (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`,
  `DB_PASSWORD`), com `ddl-auto: update` em vez de `create-drop` (não apaga
  dados a cada subida).
- Adicionado `spring-boot-starter-actuator` em todos os serviços, expondo
  `/actuator/health`, usado pelo `HEALTHCHECK` do Docker e pelo
  `depends_on: condition: service_healthy` do Compose — isso é o que atende ao
  requisito de **monitoramento dos microsserviços**.

## 4. Como executar

### Pré-requisitos
- Conta Azure ativa (`az login` feito previamente) ou Service Principal configurado.
- Terraform >= 1.5 instalado.
- Chave SSH local em `~/.ssh/id_rsa.pub` (opcional — se não existir, o
  Terraform gera uma automaticamente em `terraform/generated_key.pem`).

### Passos

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
# edite terraform.tfvars: senhas, região, etc.

terraform init
terraform plan
terraform apply
```

Ao final, o Terraform imprime o IP público e as URLs. O provisionamento da
aplicação (clone + build + subida dos containers) leva alguns minutos após a VM
ficar pronta — acompanhe com:

```bash
ssh azureuser@<IP_PUBLICO>
sudo tail -f /var/log/markovitz-bootstrap.log
```

### Destruir tudo

```bash
terraform destroy
```

## 5. Maior dificuldade encontrada

A maior dificuldade foi **garantir a ordem correta de inicialização dos containers**.
No início, os microsserviços tentavam se conectar ao PostgreSQL e ao RabbitMQ
imediatamente ao subir, enquanto esses serviços ainda estavam inicializando — causando
erros de conexão e falhas no startup.

A solução foi combinar duas abordagens:

1. **`healthcheck` no Docker Compose** para PostgreSQL e RabbitMQ, verificando se os
   serviços estavam *realmente prontos* (não apenas com o container rodando).
2. **`depends_on: condition: service_healthy`** nos microsserviços, substituindo o
   `depends_on` simples que só esperava o container *iniciar*, e não *ficar pronto*.

Outra dificuldade foi a **migração de H2 (em memória) para PostgreSQL**: o H2 não
exigia configuração, enquanto o PostgreSQL exige gerenciar credenciais, múltiplos
bancos lógicos e um script de inicialização (`init-multi-db.sh`) para criar os
4 bancos (`userdb`, `assetdb`, `portfoliodb`, `notificationdb`) automaticamente.

## 6. Lições aprendidas

- **Infraestrutura como Código (IaC) é transformador**: com o Terraform, qualquer
  integrante do grupo consegue recriar toda a infraestrutura do zero em minutos,
  sem precisar clicar no portal Azure — essencial para consistência e recuperação
  de desastres.

- **Separar credenciais do código é obrigatório**: usar `.tfvars` e `.env` (ambos
  no `.gitignore`) garante que senhas nunca sejam commitadas no repositório — boa
  prática que vale tanto em projetos acadêmicos quanto em produção.

- **`cloud-init` é poderoso para automação de VMs**: a VM "se configura sozinha" no
  primeiro boot sem nenhuma intervenção manual. Combinado com um serviço `systemd`,
  é possível reexecutar o provisionamento sem destruir e recriar a VM inteira
  (`sudo systemctl restart markovitz-bootstrap`), o que acelerou muito a depuração.

- **Docker Compose como "contrato de ambiente"**: descrever todos os serviços em um
  único `docker-compose.yml` versionado elimina o clássico problema de "funciona na
  minha máquina" — o ambiente da VM é idêntico ao ambiente local de desenvolvimento.

- **Healthchecks são essenciais em sistemas distribuídos**: sem eles, microsserviços
  falham silenciosamente por tentarem se conectar a dependências que ainda não estão
  prontas. Um pequeno investimento em configuração evita horas de depuração.
