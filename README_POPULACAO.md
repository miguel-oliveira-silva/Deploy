# 📊 Guia de População do Banco de Dados - Ações Brasileiras

## 🎯 Objetivo

Este guia explica como popular o banco de dados do sistema Markovitz com **dados reais** dos últimos **6 meses** de todas as principais **ações brasileiras** do Ibovespa.

---

## 📁 Scripts Disponíveis

### 1. `populate_acoes_brasileiras.py` ⭐ **RECOMENDADO**
População completa com **37 ações brasileiras** dos últimos 6 meses.

### 2. `populate_and_optimize.py`
Script original com apenas 5 ações + geração de gráfico da Fronteira Eficiente.

---

## 🚀 Passo a Passo - População Completa

### **Pré-requisitos**

1. **VM Azure rodando** com os microsserviços
2. **Python 3.8+** instalado
3. **Conexão com internet** (para baixar dados do Yahoo Finance)

### **Passo 1: Instalar Dependências**

```bash
# Na raiz do projeto markovitz-devops
pip install requests yfinance pandas
```

Ou usando requirements:
```bash
pip install -r requirements.txt
```

### **Passo 2: Verificar se a VM está Ativa**

```bash
# Testar conectividade
curl http://20.195.170.160:8081/actuator/health

# Deve retornar: {"status":"UP"}
```

Se não estiver UP, inicie a VM Azure:
```bash
cd terraform
terraform apply
```

### **Passo 3: Executar o Script de População**

```bash
python populate_acoes_brasileiras.py
```

---

## 📊 O que o Script Faz?

### **Etapa 1: Health Check**
Verifica se os 4 microsserviços estão UP:
- ✅ user-service (porta 8081)
- ✅ asset-service (porta 8082)  
- ✅ portfolio-service (porta 8083)
- ✅ notification-service (porta 8084)

### **Etapa 2: Download de Dados**
Baixa dados históricos de **37 ações brasileiras** via Yahoo Finance:
- **Período**: Últimos 6 meses (180 dias)
- **Fonte**: Yahoo Finance (`yfinance` library)
- **Formato**: Preços de fechamento ajustados

### **Etapa 3: Cadastro de Ativos**
Envia cada ação para `POST /api/assets`:
```json
{
  "ticker": "PETR4",
  "name": "Petrobras PN",
  "sector": "Energia"
}
```

### **Etapa 4: Envio de Preços**
Para cada ativo, envia histórico para `POST /api/assets/{ticker}/prices`:
```json
{
  "price": 36.50,
  "priceDate": "2024-01-15"
}
```

### **Etapa 5: Estatísticas**
Exibe resumo dos dados carregados com variações percentuais.

---

## 📈 Ações Incluídas (37 Total)

### **Energia & Petróleo (3)**
- PETR3, PETR4 (Petrobras)
- PRIO3 (PRIO)

### **Mineração (1)**
- VALE3 (Vale)

### **Bancos (4)**
- ITUB4 (Itaú), BBDC4 (Bradesco)
- BBAS3 (Banco do Brasil), SANB11 (Santander)

### **Varejo (4)**
- MGLU3 (Magazine Luiza), AMER3 (Americanas)
- LREN3 (Renner), PCAR3 (Pão de Açúcar)

### **Indústria & Logística (3)**
- WEGE3 (WEG), EMBR3 (Embraer)
- RAIL3 (Rumo)

### **Alimentos & Bebidas (3)**
- ABEV3 (Ambev), JBSS3 (JBS)
- BRFS3 (BRF)

### **Telecomunicações (2)**
- VIVT3 (Vivo), TIMS3 (Tim)

### **Energia Elétrica & Saneamento (5)**
- ELET3, ELET6 (Eletrobras)
- CMIG4 (Cemig), CPFE3 (CPFL)
- SBSP3 (Sabesp)

### **Construção (2)**
- CYRE3 (Cyrela), MRVE3 (MRV)

### **Papel & Celulose (1)**
- SUZB3 (Suzano)

### **Siderurgia (3)**
- CSNA3 (CSN), GGBR4 (Gerdau)
- GOAU4 (Gerdau Metalúrgica)

### **Saúde (2)**
- RADL3 (Raia Drogasil), HAPV3 (Hapvida)

### **Tecnologia (1)**
- TOTS3 (TOTVS)

### **Imobiliário (2)**
- MULT3 (Multiplan), BRML3 (BR Malls)

### **Seguros (1)**
- BBSE3 (BB Seguridade)

---

## ⏱️ Tempo de Execução

- **Download dos dados**: 2-5 minutos (depende da internet)
- **Cadastro de ativos**: 10-20 segundos
- **Envio de preços**: 5-10 minutos (rate limiting do Yahoo)

**Total estimado**: ~10-15 minutos

---

## 📊 Saída Esperada

```
======================================================================
  MARKOVITZ — POPULAÇÃO DE AÇÕES BRASILEIRAS (6 MESES)
======================================================================
  📊 Total de ações: 37
  📅 Período: 2024-06-22 → 2024-12-22
======================================================================

======================================================================
  Verificando Saúde dos Microsserviços
======================================================================
  ✅ user-service: UP
  ✅ asset-service: UP
  ✅ portfolio-service: UP

======================================================================
  Baixando Histórico (Últimos 6 Meses: 2024-06-22 → 2024-12-22)
======================================================================
  ℹ️  Total de ações: 37

  [1/37] PETR3    (PETR3.SA    ) ✅ 126 pregões | R$ 38.45 → R$ 42.30 📈 +10.01%
  [2/37] PETR4    (PETR4.SA    ) ✅ 126 pregões | R$ 34.20 → R$ 37.85 📈 +10.67%
  [3/37] VALE3    (VALE3.SA    ) ✅ 126 pregões | R$ 61.25 → R$ 58.40 📉  -4.65%
  ...

  ──────────────────────────────────────────────────────────────────────
  ✅ Sucesso: 35/37 ações
  ⚠️  Falhas: 2 ações sem dados suficientes

======================================================================
  Cadastrando Ativos no Asset-Service
======================================================================
  ✅ PETR3    cadastrado (ID=1)
  ✅ PETR4    cadastrado (ID=2)
  ✅ VALE3    cadastrado (ID=3)
  ...

  ──────────────────────────────────────────────────────────────────────
  ✅ Novos: 35 | Já existiam: 0 | Erros: 0

======================================================================
  Enviando Histórico de Preços
======================================================================
  ℹ️  Processando 35 ativos...

  PETR3    (126 preços) ✅ 126 enviados
  PETR4    (126 preços) ✅ 126 enviados
  VALE3    (126 preços) ✅ 126 enviados
  ...

  ──────────────────────────────────────────────────────────────────────
  ✅ Total enviado: 4410 preços

======================================================================
  Estatísticas dos Dados Carregados
======================================================================

  Ticker   Pregões   Primeiro     Último  Variação
  -------- -------- ---------- ---------- ----------
  PETR3         126    R$ 38.45   R$ 42.30 📈  +10.01%
  PETR4         126    R$ 34.20   R$ 37.85 📈  +10.67%
  VALE3         126    R$ 61.25   R$ 58.40 📉   -4.65%
  ...

  ──────────────────────────────────────────────────────────────────────
  ✅ Total: 35 ações com dados completos
  📅 Período real: 2024-06-22 → 2024-12-22

======================================================================
  ✅ PROCESSO CONCLUÍDO COM SUCESSO!
======================================================================

  🎉 Banco de dados populado com 35 ações brasileiras
  📊 Dados dos últimos 6 meses (2024-06-22 → 2024-12-22)
  🚀 Agora você pode criar e otimizar carteiras no aplicativo Godot!

  💡 Dica: No app, selecione de 2 a 10 ativos e clique em 'Continuar'
     para ver a carteira otimizada pelo algoritmo de Markowitz.
```

---

## 🧪 Testar no App Godot

### **Passo 1: Abrir o App**
```
Godot 4.6 → Executar (F5)
```

### **Passo 2: Ver Ativos Disponíveis**
Na tela de seleção, você verá **todas as 35+ ações** organizadas por setor:
- Energia
- Mineração
- Financeiro
- Varejo
- Indústria
- etc.

### **Passo 3: Criar Carteira**
1. Selecione de 2 a 10 ativos
2. Clique em "Continuar"
3. Aguarde a otimização (~5 segundos)
4. Veja o resultado:
   - Pesos ótimos de cada ativo
   - Retorno esperado
   - Risco da carteira
   - Índice de Sharpe

---

## 🔧 Troubleshooting

### **Problema 1: "Cannot connect to host"**
**Causa**: VM Azure não está rodando ou IP mudou  
**Solução**:
```bash
cd terraform
terraform output public_ip
# Atualizar IP em populate_acoes_brasileiras.py se necessário
```

### **Problema 2: "Nenhum dado retornado"**
**Causa**: Yahoo Finance bloqueou temporariamente  
**Solução**: Aguardar 5 minutos e tentar novamente

### **Problema 3: "HTTP 409 - Ticker já cadastrado"**
**Causa**: Ativo já existe no banco  
**Solução**: Isso é normal! O script pula automaticamente

### **Problema 4: Rate limiting (muitos erros)**
**Causa**: Yahoo Finance limitou as requisições  
**Solução**: O script já tem delays. Se persistir, execute em 2 etapas:
1. Primeiras 20 ações
2. Aguardar 10 minutos
3. Últimas 17 ações

---

## 📚 Dados Técnicos

### **Formato dos Preços**
- **Tipo**: Close (fechamento ajustado)
- **Frequência**: Diária
- **Ajustes**: Auto-ajustado (dividendos e splits)

### **Período**
- **Início**: Hoje - 180 dias
- **Fim**: Hoje
- **Dias úteis**: ~126 pregões (média)

### **Validação**
- Mínimo de 20 pregões por ação
- Remoção de NaN (dados ausentes)
- Preços arredondados para 4 casas decimais

---

## 🎓 Uso Acadêmico

Este script atende ao requisito do trabalho escolar:
- ✅ **Dados reais** (não mockados)
- ✅ **Período relevante** (6 meses)
- ✅ **Diversificação** (37 ações, 15 setores)
- ✅ **Pronto para otimização** (mínimo 2 preços por ativo)

---

## 🔄 Atualizar Dados (Mensal)

Para manter os dados atualizados:

```bash
# A cada mês, re-executar
python populate_acoes_brasileiras.py

# O script é idempotente:
# - Ativos já cadastrados: pula
# - Preços duplicados: ignora
# - Novos preços: adiciona
```

---

## 📞 Suporte

**Problemas comuns:**
- `yfinance` não encontrado → `pip install yfinance`
- Timeout → Verificar se VM está rodando
- 409 Conflict → Normal, ativo já existe

**Logs úteis:**
```bash
# Ver logs dos microsserviços
ssh azureuser@20.195.170.160
docker compose logs asset-service -f
```

---

**Última atualização**: 22/12/2024  
**Versão do Script**: 1.0  
**Python**: 3.8+
