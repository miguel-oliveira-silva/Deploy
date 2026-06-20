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

*(preencher com a experiência real do grupo — sugestão de pontos a abordar)*

- Garantir que os microsserviços só tentassem conectar ao banco/RabbitMQ depois
  que esses estivessem realmente prontos (resolvido com `healthcheck` +
  `depends_on: condition: service_healthy` no Compose, em vez de apenas
  `depends_on` simples, que só espera o container *iniciar*, não *ficar pronto*).
- Adaptar de H2 em memória (zero configuração) para PostgreSQL persistente,
  que exige gerenciar credenciais, múltiplos bancos lógicos e scripts de
  inicialização.
- Build do Maven dentro da VM (em vez de usar imagens pré-buildadas de um
  registry) torna o primeiro deploy mais lento, mas elimina a necessidade de
  configurar autenticação com um Azure Container Registry — troca consciente
  de simplicidade por tempo de build.

## 6. Lições aprendidas

*(preencher com a experiência real do grupo — sugestão de pontos)*

- Infraestrutura como Código permite recriar o ambiente do zero de forma
  determinística — útil tanto para recuperação de desastres quanto para
  demonstrar o projeto em qualquer máquina/apresentação.
- Separar credenciais (`.tfvars`, `.env`) do código versionado é essencial,
  mesmo em projeto acadêmico.
- `cloud-init` + `systemd` (serviço oneshot) permite reexecutar o
  provisionamento manualmente (`sudo systemctl restart markovitz-bootstrap`)
  sem precisar destruir e recriar a VM inteira — acelera muito a depuração.
