# 📊 Markovitz — Documentação da API

> Sistema de otimização de carteiras de investimento baseado no **Algoritmo de Markowitz**.
> Stack: 4 microsserviços Spring Boot + PostgreSQL + RabbitMQ, rodando em Azure VM.

**IP Público:** `20.195.170.160`

---

## 🔗 URLs de Acesso Rápido

| Serviço              | Base URL                              | Swagger UI                                              |
|----------------------|---------------------------------------|---------------------------------------------------------|
| **user-service**     | `http://20.195.170.160:8081`          | http://20.195.170.160:8081/swagger-ui.html              |
| **asset-service**    | `http://20.195.170.160:8082`          | http://20.195.170.160:8082/swagger-ui.html              |
| **portfolio-service**| `http://20.195.170.160:8083`          | http://20.195.170.160:8083/swagger-ui.html              |
| **notification-service** | `http://20.195.170.160:8084`      | http://20.195.170.160:8084/swagger-ui.html              |
| **RabbitMQ UI**      | `http://20.195.170.160:15672`         | login: `admin` / senha definida no tfvars               |

---

## 🔄 Fluxo de Uso Recomendado

```
1. Criar usuário       →  user-service
2. Cadastrar ativos    →  asset-service
3. Adicionar preços    →  asset-service  (mín. 2 por ativo)
4. Criar carteira      →  portfolio-service
5. Otimizar carteira   →  portfolio-service  ← executa Markowitz!
6. Ver notificações    →  notification-service
```

---

## 👤 1. User Service — Porta 8081

> Gerencia investidores: cadastro, busca e atualização de perfil de risco.

**Base URL:** `http://20.195.170.160:8081`

---

### `POST /api/users/register` — Registrar novo usuário

**Entrada (`application/json`):**

```json
{
  "name": "João Silva",
  "email": "joao@email.com",
  "password": "senha123",
  "riskProfile": "MODERADO"
}
```

| Campo         | Tipo     | Obrigatório | Regras                                      |
|---------------|----------|-------------|---------------------------------------------|
| `name`        | `string` | ✅           | 2 a 100 caracteres                          |
| `email`       | `string` | ✅           | Formato válido (`usuario@dominio.com`)      |
| `password`    | `string` | ✅           | Mínimo 6 caracteres                         |
| `riskProfile` | `enum`   | ✅           | `CONSERVADOR` \| `MODERADO` \| `AGRESSIVO`  |

**Saída (`201 Created`):**

```json
{
  "id": 1,
  "name": "João Silva",
  "email": "joao@email.com",
  "riskProfile": "MODERADO",
  "createdAt": "2026-06-21T14:30:00"
}
```

> ⚠️ A senha **nunca** é retornada na resposta.
> 📨 Ao registrar, um evento `user.registered` é publicado no RabbitMQ — o notification-service gera automaticamente uma notificação de boas-vindas.

**Erros:**
| Código | Motivo                       |
|--------|------------------------------|
| `400`  | Dados inválidos (validação)  |
| `409`  | E-mail já cadastrado         |

---

### `GET /api/users/{id}` — Buscar usuário por ID

```
GET http://20.195.170.160:8081/api/users/1
```

**Saída (`200 OK`):**

```json
{
  "id": 1,
  "name": "João Silva",
  "email": "joao@email.com",
  "riskProfile": "MODERADO",
  "createdAt": "2026-06-21T14:30:00"
}
```

**Erros:**
| Código | Motivo               |
|--------|----------------------|
| `404`  | Usuário não encontrado |

---

### `GET /api/users` — Listar todos os usuários

```
GET http://20.195.170.160:8081/api/users
```

**Saída (`200 OK`):**

```json
[
  {
    "id": 1,
    "name": "João Silva",
    "email": "joao@email.com",
    "riskProfile": "MODERADO",
    "createdAt": "2026-06-21T14:30:00"
  },
  {
    "id": 2,
    "name": "Ana Lima",
    "email": "ana@email.com",
    "riskProfile": "AGRESSIVO",
    "createdAt": "2026-06-21T14:35:00"
  }
]
```

---

### `PUT /api/users/{id}/risk-profile` — Atualizar perfil de risco

```
PUT http://20.195.170.160:8081/api/users/1/risk-profile
Content-Type: application/json
```

**Entrada (body):**

```json
"AGRESSIVO"
```

> Valores aceitos: `"CONSERVADOR"`, `"MODERADO"`, `"AGRESSIVO"`

**Saída (`200 OK`):** objeto `UserResponseDTO` atualizado (mesmo formato do `GET /api/users/{id}`).

---

## 📈 2. Asset Service — Porta 8082

> Gerencia ativos financeiros (ações, FIIs) e seus históricos de preço.
> Fornece as **estatísticas financeiras (μ e σ)** usadas pelo algoritmo de Markowitz.

**Base URL:** `http://20.195.170.160:8082`

---

### `POST /api/assets` — Cadastrar novo ativo

**Entrada (`application/json`):**

```json
{
  "ticker": "PETR4",
  "name": "Petrobras S.A. PN",
  "sector": "Energia"
}
```

| Campo    | Tipo     | Obrigatório | Regras                        |
|----------|----------|-------------|-------------------------------|
| `ticker` | `string` | ✅           | 4 a 10 caracteres (ex: PETR4) |
| `name`   | `string` | ✅           | Máximo 200 caracteres         |
| `sector` | `string` | ❌           | Máximo 100 caracteres         |

> O ticker é convertido automaticamente para maiúsculas.

**Saída (`201 Created`):**

```json
{
  "id": 1,
  "ticker": "PETR4",
  "name": "Petrobras S.A. PN",
  "sector": "Energia",
  "priceCount": 0,
  "createdAt": "2026-06-21T14:30:00"
}
```

**Erros:**
| Código | Motivo              |
|--------|---------------------|
| `409`  | Ticker já cadastrado |

---

### `GET /api/assets` — Listar todos os ativos

```
GET http://20.195.170.160:8082/api/assets
```

**Saída (`200 OK`):**

```json
[
  { "id": 1, "ticker": "PETR4", "name": "Petrobras S.A. PN", "sector": "Energia", "priceCount": 252 },
  { "id": 2, "ticker": "VALE3", "name": "Vale S.A. ON",      "sector": "Mineração","priceCount": 252 }
]
```

---

### `GET /api/assets/{ticker}` — Buscar ativo por ticker

```
GET http://20.195.170.160:8082/api/assets/PETR4
```

**Saída (`200 OK`):** mesmo formato do cadastro, com `priceCount` atualizado.

---

### `POST /api/assets/{ticker}/prices` — Adicionar preço histórico

```
POST http://20.195.170.160:8082/api/assets/PETR4/prices
```

**Entrada (`application/json`):**

```json
{
  "price": 36.50,
  "priceDate": "2024-01-15"
}
```

| Campo       | Tipo     | Obrigatório | Regras                   |
|-------------|----------|-------------|--------------------------|
| `price`     | `number` | ✅           | Preço de fechamento      |
| `priceDate` | `string` | ✅           | Formato `YYYY-MM-DD`     |

> 📨 Após salvar, publica evento `asset.price.updated` no RabbitMQ.
> ⚠️ É necessário pelo menos **2 preços** antes de calcular estatísticas.

**Saída (`201 Created`):** objeto `AssetResponseDTO` com `priceCount` incrementado.

---

### `GET /api/assets/{ticker}/stats` — Calcular estatísticas financeiras ⭐

> Este é o endpoint mais importante para o algoritmo de Markowitz.

```
GET http://20.195.170.160:8082/api/assets/PETR4/stats
```

**Saída (`200 OK`):**

```json
{
  "ticker": "PETR4",
  "name": "Petrobras S.A. PN",
  "priceCount": 252,
  "averageDailyReturn": 0.00127,
  "dailyVolatility": 0.02106,
  "annualizedReturn": 0.32004,
  "annualizedVolatility": 0.33434
}
```

| Campo                  | Descrição                                    |
|------------------------|----------------------------------------------|
| `averageDailyReturn`   | Retorno médio diário (μ)                     |
| `dailyVolatility`      | Desvio padrão diário (σ)                     |
| `annualizedReturn`     | Retorno anualizado (μ × 252) — ex: 32% a.a. |
| `annualizedVolatility` | Risco anualizado (σ × √252) — ex: 33.4%     |

**Erros:**
| Código | Motivo                            |
|--------|-----------------------------------|
| `400`  | Menos de 2 preços cadastrados     |
| `404`  | Ticker não encontrado             |

---

## 💼 3. Portfolio Service — Porta 8083

> Cria carteiras de investimento e executa o **Algoritmo de Markowitz** para encontrar os pesos ótimos que maximizam o Índice de Sharpe.

**Base URL:** `http://20.195.170.160:8083`

**Pré-requisito:** cada ativo da carteira deve ter pelo menos **2 preços históricos** cadastrados no asset-service.

---

### `POST /api/portfolios` — Criar carteira

**Entrada (`application/json`):**

```json
{
  "userId": 1,
  "name": "Minha aposentadoria",
  "tickers": ["PETR4", "VALE3", "ITUB4", "WEGE3"],
  "optimizationGoal": "MAX_SHARPE"
}
```

| Campo              | Tipo           | Obrigatório | Regras                                          |
|--------------------|----------------|-------------|--------------------------------------------------|
| `userId`           | `number`       | ✅           | ID de um usuário existente                       |
| `name`             | `string`       | ✅           | Nome da carteira                                 |
| `tickers`          | `string[]`     | ✅           | Lista de tickers cadastrados no asset-service    |
| `optimizationGoal` | `enum`         | ✅           | `MAX_SHARPE` \| `MIN_RISK` \| `MAX_RETURN`       |

**Saída (`201 Created`):**

```json
{
  "id": 1,
  "userId": 1,
  "name": "Minha aposentadoria",
  "status": "PENDENTE",
  "optimizationGoal": "MAX_SHARPE",
  "expectedReturn": null,
  "portfolioRisk": null,
  "sharpeRatio": null,
  "assets": [],
  "createdAt": "2026-06-21T14:30:00",
  "optimizedAt": null
}
```

---

### `GET /api/portfolios/{id}` — Buscar carteira por ID

```
GET http://20.195.170.160:8083/api/portfolios/1
```

**Saída (`200 OK`):** mesmo formato da criação. Após otimização, todos os campos de resultado estarão preenchidos.

---

### `GET /api/portfolios/user/{userId}` — Listar carteiras de um usuário

```
GET http://20.195.170.160:8083/api/portfolios/user/1
```

**Saída (`200 OK`):** array de `PortfolioResponseDTO`.

---

### `POST /api/portfolios/{id}/optimize` — Executar Markowitz ⭐

> Endpoint principal do sistema. Consulta o asset-service, executa o algoritmo e retorna os pesos ótimos.

```
POST http://20.195.170.160:8083/api/portfolios/1/optimize
```

> Sem body — apenas o ID da carteira na URL.

**Saída (`200 OK`):**

```json
{
  "id": 1,
  "userId": 1,
  "name": "Minha aposentadoria",
  "status": "OTIMIZADO",
  "optimizationGoal": "MAX_SHARPE",
  "expectedReturn": 0.29,
  "portfolioRisk": 0.224,
  "sharpeRatio": 0.848,
  "assets": [
    {
      "ticker": "PETR4",
      "weight": 0.30,
      "expectedReturn": 0.32,
      "risk": 0.334
    },
    {
      "ticker": "VALE3",
      "weight": 0.25,
      "expectedReturn": 0.28,
      "risk": 0.298
    },
    {
      "ticker": "ITUB4",
      "weight": 0.28,
      "expectedReturn": 0.26,
      "risk": 0.267
    },
    {
      "ticker": "WEGE3",
      "weight": 0.17,
      "expectedReturn": 0.31,
      "risk": 0.312
    }
  ],
  "createdAt": "2026-06-21T14:30:00",
  "optimizedAt": "2026-06-21T14:30:05"
}
```

| Campo            | Descrição                                           |
|------------------|-----------------------------------------------------|
| `status`         | `PENDENTE` → `OTIMIZADO`                            |
| `expectedReturn` | Retorno esperado anual da carteira (ex: 29% a.a.)   |
| `portfolioRisk`  | Risco anual da carteira (ex: 22.4%)                 |
| `sharpeRatio`    | (Retorno - SELIC) / Risco — quanto maior, melhor    |
| `assets[].weight`| Percentual alocado em cada ativo (soma = 1.0)       |

> 📨 Após otimizar, publica evento `portfolio.optimized` no RabbitMQ — o notification-service gera automaticamente uma notificação para o usuário.

**Erros:**
| Código | Motivo                                              |
|--------|-----------------------------------------------------|
| `404`  | Carteira não encontrada                             |
| `500`  | asset-service indisponível ou dados insuficientes   |

---

## 🔔 4. Notification Service — Porta 8084

> Recebe eventos do RabbitMQ e armazena notificações para os usuários.
> É um serviço **passivo** — não precisa de chamadas para gerar notificações, elas são geradas automaticamente pelos outros serviços.

**Base URL:** `http://20.195.170.160:8084`

**Eventos que geram notificações automaticamente:**

| Evento RabbitMQ        | Quando ocorre                         | Tipo de notificação     |
|------------------------|---------------------------------------|-------------------------|
| `user.registered`      | Novo usuário cadastrado               | `BOAS_VINDAS`           |
| `portfolio.optimized`  | Carteira otimizada com sucesso        | `CARTEIRA_OTIMIZADA`    |

---

### `GET /api/notifications/user/{userId}` — Listar notificações de um usuário

```
GET http://20.195.170.160:8084/api/notifications/user/1
```

**Saída (`200 OK`):**

```json
[
  {
    "id": 2,
    "userId": 1,
    "type": "CARTEIRA_OTIMIZADA",
    "title": "✅ Carteira 'Minha aposentadoria' otimizada!",
    "message": "Sua carteira foi otimizada com sucesso.\nRetorno esperado: 29.00% a.a.\nRisco: 22.40%\nÍndice de Sharpe: 0.848",
    "status": "ENVIADA",
    "sourceEvent": "portfolio.optimized",
    "sourceId": 1,
    "createdAt": "2026-06-21T14:30:05"
  },
  {
    "id": 1,
    "userId": 1,
    "type": "BOAS_VINDAS",
    "title": "🎉 Bem-vindo ao Markovitz, João!",
    "message": "Sua conta foi criada com sucesso. Comece cadastrando ativos e criando sua carteira!",
    "status": "ENVIADA",
    "sourceEvent": "user.registered",
    "sourceId": 1,
    "createdAt": "2026-06-21T14:28:00"
  }
]
```

| Campo         | Descrição                                              |
|---------------|--------------------------------------------------------|
| `type`        | `BOAS_VINDAS` \| `CARTEIRA_OTIMIZADA`                  |
| `status`      | `ENVIADA` (sempre, por ora)                            |
| `sourceEvent` | Evento RabbitMQ que gerou a notificação                |
| `sourceId`    | ID do recurso que gerou o evento (usuário ou carteira) |

---

## 🩺 Health Checks

Todos os serviços expõem `/actuator/health`:

```bash
curl http://20.195.170.160:8081/actuator/health  # user-service
curl http://20.195.170.160:8082/actuator/health  # asset-service
curl http://20.195.170.160:8083/actuator/health  # portfolio-service
curl http://20.195.170.160:8084/actuator/health  # notification-service
```

**Resposta esperada:**

```json
{
  "status": "UP",
  "components": {
    "db":     { "status": "UP" },
    "rabbit": { "status": "UP" },
    "ping":   { "status": "UP" }
  }
}
```

---

## 🧪 Exemplo de Uso Completo (curl)

```bash
IP="20.195.170.160"

# 1. Criar usuário
curl -s -X POST http://$IP:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"name":"João Silva","email":"joao@email.com","password":"senha123","riskProfile":"MODERADO"}' | jq

# 2. Cadastrar ativos
curl -s -X POST http://$IP:8082/api/assets \
  -H "Content-Type: application/json" \
  -d '{"ticker":"PETR4","name":"Petrobras S.A. PN","sector":"Energia"}' | jq

curl -s -X POST http://$IP:8082/api/assets \
  -H "Content-Type: application/json" \
  -d '{"ticker":"VALE3","name":"Vale S.A. ON","sector":"Mineração"}' | jq

# 3. Adicionar preços históricos (mínimo 2 por ativo)
curl -s -X POST http://$IP:8082/api/assets/PETR4/prices \
  -H "Content-Type: application/json" \
  -d '{"price":34.20,"priceDate":"2024-01-02"}' | jq

curl -s -X POST http://$IP:8082/api/assets/PETR4/prices \
  -H "Content-Type: application/json" \
  -d '{"price":36.50,"priceDate":"2024-01-03"}' | jq

curl -s -X POST http://$IP:8082/api/assets/VALE3/prices \
  -H "Content-Type: application/json" \
  -d '{"price":71.80,"priceDate":"2024-01-02"}' | jq

curl -s -X POST http://$IP:8082/api/assets/VALE3/prices \
  -H "Content-Type: application/json" \
  -d '{"price":73.40,"priceDate":"2024-01-03"}' | jq

# 4. Criar carteira
curl -s -X POST http://$IP:8083/api/portfolios \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"name":"Minha aposentadoria","tickers":["PETR4","VALE3"],"optimizationGoal":"MAX_SHARPE"}' | jq

# 5. Otimizar carteira (ID=1)
curl -s -X POST http://$IP:8083/api/portfolios/1/optimize | jq

# 6. Ver notificações geradas
curl -s http://$IP:8084/api/notifications/user/1 | jq
```
