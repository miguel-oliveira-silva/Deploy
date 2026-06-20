# 🚀 Guia de Deploy — Azure for Students
### Sistema Markovitz | Questão 2 — DevOps

> **Tempo estimado:** 30–40 minutos (a maior parte é esperar a VM subir)  
> **Custo:** ~$1–2 do crédito Azure for Students (VM B2s por poucas horas)

---

## Pré-requisitos — O que instalar antes

### 1. Azure CLI
```powershell
winget install Microsoft.AzureCLI
```
Feche e abra o terminal depois de instalar. Verifique:
```powershell
az --version
```

### 2. Terraform
```powershell
winget install HashiCorp.Terraform
```
Feche e abra o terminal. Verifique:
```powershell
terraform --version
```

### 3. Git (provavelmente já tem)
```powershell
git --version
```

---

## ETAPA 1 — Fazer login na Azure

```powershell
az login
```

Isso abre o navegador. Entre com a sua conta de estudante (`@edu.br` ou equivalente).

Depois confirme que está na subscription correta:
```powershell
az account show
```

Procure o campo `"name"` — deve ser algo como `"Azure for Students"`.

> ⚠️ **Se aparecer mais de uma subscription**, selecione a correta:
> ```powershell
> az account set --subscription "Azure for Students"
> ```

---

## ETAPA 2 — Criar chave SSH (se ainda não tiver)

A chave SSH é usada para você entrar na VM depois do deploy.

```powershell
# Verifica se já existe
Test-Path ~/.ssh/id_rsa.pub
```

Se retornar `False`, crie agora:
```powershell
ssh-keygen -t rsa -b 4096 -C "markovitz-deploy" -f ~/.ssh/id_rsa -N ""
```

---

## ETAPA 3 — Fazer push do projeto para o GitHub

O cloud-init clona o repositório do GitHub durante o boot da VM.  
O repositório que será clonado é o **`markovitz-devops`** (a pasta que tem o `docker-compose.yml`).

### 3.1 — Confirme o conteúdo do `markovitz-devops`
```
markovitz-devops/
├── docker-compose.yml       ← orquestra os 4 serviços
├── user-service/            ← código adaptado para PostgreSQL
├── asset-service/
├── portfolio-service/
├── notification-service/
├── init-multi-db.sh         ← cria os 4 bancos no PostgreSQL
└── terraform/               ← infraestrutura
```

### 3.2 — Crie um repositório no GitHub

1. Acesse https://github.com/new
2. Nome: `markovitz-devops` (pode ser **privado**)
3. Clique em **Create repository** (sem inicializar com README)

### 3.3 — Faça o push

```powershell
cd c:\Users\migue\GIT\Markovitz\markovitz-devops

git init                          # só se ainda não for um repo git
git add .
git commit -m "deploy inicial"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/markovitz-devops.git
git push -u origin main
```

> 💡 Substitua `SEU_USUARIO` pelo seu usuário do GitHub.

---

## ETAPA 4 — Configurar o Terraform

### 4.1 — Copiar o arquivo de variáveis

```powershell
cd c:\Users\migue\GIT\Markovitz\markovitz-devops\terraform
Copy-Item terraform.tfvars.example terraform.tfvars
```

### 4.2 — Editar o `terraform.tfvars`

Abra o arquivo `terraform.tfvars` e preencha:

```hcl
location     = "Brazil South"
project_name = "markovitz"
environment  = "prod"
vm_size      = "Standard_B2s"

admin_username      = "azureuser"
ssh_public_key_path = "~/.ssh/id_rsa.pub"

# URL do repositório que você acabou de criar no GitHub
git_repo_url    = "https://github.com/SEU_USUARIO/markovitz-devops.git"
git_repo_branch = "main"

# Senha do PostgreSQL — escolha uma senha forte
db_user     = "markovitz"
db_password = "MinhaS3nhaF0rte!"

# Senha do RabbitMQ
rabbitmq_user     = "admin"
rabbitmq_password = "RabbitS3nha!"

# Libera SSH de qualquer IP (ok para projeto acadêmico)
allowed_ssh_cidr = "0.0.0.0/0"
```

> ⚠️ **NUNCA** faça commit do `terraform.tfvars` — ele já está no `.gitignore`.

---

## ETAPA 5 — Rodar o Terraform

```powershell
cd c:\Users\migue\GIT\Markovitz\markovitz-devops\terraform

# Inicializa (baixa os providers Azure)
terraform init

# Mostra o que vai ser criado (sem criar nada)
terraform plan

# Cria a infraestrutura na Azure (vai pedir confirmação: "yes")
terraform apply
```

O `terraform apply` vai:
1. Criar um **Resource Group** `markovitz-prod-rg`
2. Criar uma **VNet** com subnet e IP público
3. Criar o **Network Security Group** (firewall) liberando as portas 8081–8084, SSH e RabbitMQ
4. Criar a **VM Ubuntu 22.04** e injetar o script de bootstrap

⏱️ **Aguarde 3–5 minutos** até o Terraform terminar.

### Saída esperada ao final

```
Outputs:

vm_public_ip            = "4.201.XX.XX"
ssh_command             = "ssh azureuser@4.201.XX.XX"
user_service_url        = "http://4.201.XX.XX:8081"
asset_service_url       = "http://4.201.XX.XX:8082"
portfolio_service_url   = "http://4.201.XX.XX:8083"
notification_service_url = "http://4.201.XX.XX:8084"
rabbitmq_management_url = "http://4.201.XX.XX:15672"
```

> 📋 **Anote o IP!** Você vai precisar dele.

---

## ETAPA 6 — Aguardar o bootstrap da aplicação

A VM acabou de nascer. O **cloud-init** está rodando em background:
instalando Docker, clonando o repositório e buildando as 4 imagens.

**Este processo leva 10–20 minutos** (o build Maven é pesado).

### 6.1 — Entrar na VM via SSH para acompanhar

```powershell
ssh azureuser@4.201.XX.XX
```
(substitua pelo IP real que o Terraform mostrou)

> Se o Terraform gerou a chave automaticamente (não havia `~/.ssh/id_rsa.pub`):
> ```powershell
> ssh -i terraform/generated_key.pem azureuser@4.201.XX.XX
> ```

### 6.2 — Acompanhar o log de bootstrap

```bash
sudo tail -f /var/log/markovitz-bootstrap.log
```

Você verá as etapas sendo executadas em tempo real. Quando aparecer:

```
[markovitz] Bootstrap concluído em ...
NAME                    STATUS
user-service            Up
asset-service           Up
portfolio-service       Up
notification-service    Up
postgres                Up
rabbitmq                Up
```

A aplicação está pronta! Use `Ctrl+C` para sair do log e `exit` para sair da VM.

---

## ETAPA 7 — Verificar se tudo está funcionando

Nos links abaixo, substitua `IP` pelo IP real da sua VM.

### Teste rápido via curl (de dentro ou fora da VM)

```bash
# Verifica se o user-service está respondendo
curl http://IP:8081/api/users

# Verifica se o asset-service está respondendo
curl http://IP:8082/api/assets
```

### Swagger UI (abrir no navegador)

| Serviço | URL |
|---------|-----|
| user-service | `http://IP:8081/swagger-ui.html` |
| asset-service | `http://IP:8082/swagger-ui.html` |
| portfolio-service | `http://IP:8083/swagger-ui.html` |
| notification-service | `http://IP:8084/swagger-ui.html` |

### Painel do RabbitMQ

```
http://IP:15672
Usuário: admin
Senha: (a que você definiu no terraform.tfvars)
```

### Actuator / Health Check

```
http://IP:8081/actuator/health
http://IP:8082/actuator/health
http://IP:8083/actuator/health
http://IP:8084/actuator/health
```
Todos devem retornar `{"status":"UP"}`.

---

## ETAPA 8 — Testar o fluxo completo (para a apresentação)

Execute os passos abaixo com o Swagger ou com curl. Substitua `IP` pelo IP real.

### Passo 1 — Cadastrar um usuário
```http
POST http://IP:8081/api/users/register
Content-Type: application/json

{
  "name": "João Silva",
  "email": "joao@email.com",
  "password": "senha123",
  "riskProfile": "MODERADO"
}
```
✅ Verificar: notificação de boas-vindas criada em `GET http://IP:8084/api/notifications/user/1`

### Passo 2 — Cadastrar ativos
```http
POST http://IP:8082/api/assets
Content-Type: application/json
{ "ticker": "PETR4", "name": "Petrobras PN", "sector": "Energia" }

POST http://IP:8082/api/assets
Content-Type: application/json
{ "ticker": "VALE3", "name": "Vale ON", "sector": "Mineração" }
```

### Passo 3 — Adicionar preços históricos (mínimo 2 por ativo)
```http
POST http://IP:8082/api/assets/PETR4/prices
Content-Type: application/json
{ "price": 36.50, "priceDate": "2024-01-02" }

POST http://IP:8082/api/assets/PETR4/prices
Content-Type: application/json
{ "price": 37.80, "priceDate": "2024-01-03" }

POST http://IP:8082/api/assets/VALE3/prices
Content-Type: application/json
{ "price": 68.20, "priceDate": "2024-01-02" }

POST http://IP:8082/api/assets/VALE3/prices
Content-Type: application/json
{ "price": 69.50, "priceDate": "2024-01-03" }
```

### Passo 4 — Verificar estatísticas (μ e σ)
```http
GET http://IP:8082/api/assets/PETR4/stats
GET http://IP:8082/api/assets/VALE3/stats
```

### Passo 5 — Criar carteira
```http
POST http://IP:8083/api/portfolios
Content-Type: application/json

{
  "userId": 1,
  "name": "Carteira de Apresentação",
  "tickers": ["PETR4", "VALE3"],
  "optimizationGoal": "MAX_SHARPE"
}
```

### Passo 6 — Executar otimização de Markowitz! 🎯
```http
POST http://IP:8083/api/portfolios/1/optimize
```

### Passo 7 — Ver resultado e notificação
```http
GET http://IP:8083/api/portfolios/1
GET http://IP:8084/api/notifications/user/1
```

---

## ETAPA 9 — Destruir a VM após a apresentação

> ⚠️ **Importante:** destrua a VM logo após a apresentação para não gastar o crédito!

```powershell
cd c:\Users\migue\GIT\Markovitz\markovitz-devops\terraform
terraform destroy
```

Confirme com `yes`. Todos os recursos criados serão deletados da Azure.

---

## Solução de Problemas

### Os serviços ainda não respondem após 20 minutos
Entre na VM e verifique o log:
```bash
ssh azureuser@IP
sudo cat /var/log/markovitz-bootstrap.log | tail -50
```

### Erro de build no Docker (Out of Memory)
A B2s tem 4GB RAM. Se o build de 4 serviços simultâneos estourar:
```bash
# Dentro da VM
cd /opt/markovitz/app
sudo docker compose up -d --build user-service asset-service
# Aguardar...
sudo docker compose up -d --build portfolio-service notification-service
```

### Ver containers rodando
```bash
sudo docker compose ps
sudo docker compose logs user-service --tail=50
```

### Reiniciar o bootstrap manualmente
```bash
sudo systemctl restart markovitz-bootstrap
sudo tail -f /var/log/markovitz-bootstrap.log
```

### Porta não acessível (timeout no navegador)
Verifique o NSG (firewall) no portal Azure:
- Portal → Resource Groups → markovitz-prod-rg → Network Security Group
- Confirme que as regras de entrada para as portas 8081–8084 existem

---

## Checklist para a Apresentação

- [ ] VM rodando na Azure (portal mostra `Running`)
- [ ] `http://IP:8081/swagger-ui.html` abre no navegador
- [ ] `http://IP:8082/swagger-ui.html` abre no navegador
- [ ] `http://IP:8083/swagger-ui.html` abre no navegador
- [ ] `http://IP:8084/swagger-ui.html` abre no navegador
- [ ] `/actuator/health` retorna `UP` nos 4 serviços
- [ ] Fluxo completo funciona: cadastro → ativos → otimização → notificação
- [ ] Screenshot do portal Azure mostrando a VM
- [ ] Screenshot do RabbitMQ Management (`http://IP:15672`)

---

*Documento gerado para o Projeto Markovitz — Entrega 23/06/2026*
