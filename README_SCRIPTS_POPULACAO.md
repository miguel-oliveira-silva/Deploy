# 📊 Scripts de População - Guia Completo

## 🎯 Visão Geral

Este projeto oferece **3 scripts** para popular o banco de dados com ações reais:

| Script | Mercado | Ações | Tempo |
|--------|---------|-------|-------|
| `populate_acoes_brasileiras.py` | 🇧🇷 Brasil | 37 | ~10-15 min |
| `populate_acoes_usa.py` | 🇺🇸 EUA | 60 | ~15-20 min |
| `populate_all.py` | 🌎 Global | 97 | ~25-35 min |

---

## 🚀 Quick Start

### **Opção 1: Popular Tudo (Recomendado)** ⭐
```bash
# 1. Instalar dependências
pip install -r requirements.txt

# 2. Popular Brasil + EUA automaticamente
python populate_all.py
```
**Resultado**: 97 ações (37 BR + 60 US) em ~30 minutos

---

### **Opção 2: Popular Apenas Brasil** 🇧🇷
```bash
python populate_acoes_brasileiras.py
```
**Resultado**: 37 ações brasileiras em ~15 minutos

---

### **Opção 3: Popular Apenas EUA** 🇺🇸
```bash
python populate_acoes_usa.py
```
**Resultado**: 60 ações americanas em ~20 minutos

---

## 📁 Arquivos do Projeto

```
markovitz-devops/
│
├── populate_all.py                    # ⭐ Master script (Brasil + EUA)
├── populate_acoes_brasileiras.py      # 🇧🇷 37 ações BR
├── populate_acoes_usa.py              # 🇺🇸 60 ações US
├── populate_and_optimize.py           # 📊 Original com gráfico
│
├── requirements.txt                    # Dependências Python
│
├── README_SCRIPTS_POPULACAO.md        # 📚 Este arquivo
├── README_POPULACAO.md                # Docs Brasil
├── README_ACOES_USA.md                # Docs EUA
└── QUICK_START_POPULACAO.md           # Quick start BR
```

---

## 📊 Comparação dos Scripts

### **1. populate_all.py** 🌎 **MASTER**
- **O que faz**: Executa os 2 scripts (BR + US) sequencialmente
- **Vantagens**:
  - ✅ População completa automática
  - ✅ Diversificação máxima (97 ações)
  - ✅ Mercados globais
  - ✅ Um único comando
- **Quando usar**: Projeto completo para apresentação

---

### **2. populate_acoes_brasileiras.py** 🇧🇷
- **O que faz**: 37 ações do Ibovespa (últimos 6 meses)
- **Vantagens**:
  - ✅ Foco no mercado local
  - ✅ Ativos familiares (Petrobras, Vale, Itaú)
  - ✅ Mais rápido (~15 min)
  - ✅ Ideal para começar
- **Setores**: Energia, Mineração, Bancos, Varejo, Indústria, etc.
- **Quando usar**: Foco em mercado brasileiro

---

### **3. populate_acoes_usa.py** 🇺🇸
- **O que faz**: 60 ações americanas (S&P 500, Dow Jones, NASDAQ)
- **Vantagens**:
  - ✅ Blue chips globais
  - ✅ Tech giants (FAANG+)
  - ✅ Maior liquidez
  - ✅ Mercado mais maduro
- **Setores**: Technology, Finance, Healthcare, Consumer, Energy, etc.
- **Quando usar**: Exposição ao mercado americano

---

### **4. populate_and_optimize.py** 📊 **ORIGINAL**
- **O que faz**: 5 ações BR + otimização + gráfico da Fronteira Eficiente
- **Vantagens**:
  - ✅ Gera gráfico visual bonito
  - ✅ Demonstração didática
  - ✅ Rápido (~5 min)
- **Desvantagens**:
  - ❌ Poucas ações (apenas 5)
  - ❌ Requer matplotlib + scipy
- **Quando usar**: Demonstração rápida com visualização

---

## 🎯 Recomendações por Cenário

### **Cenário 1: Apresentação do Trabalho** 🎓
```bash
python populate_all.py
```
**Por quê?**
- Diversificação máxima
- Demonstra conhecimento global
- Impressiona professores
- Permite comparações BR vs US

---

### **Cenário 2: Desenvolvimento & Testes** 🧪
```bash
# Começar com Brasil (mais rápido)
python populate_acoes_brasileiras.py

# Testar no app Godot
# Se tudo OK, adicionar EUA
python populate_acoes_usa.py
```
**Por quê?**
- Iteração mais rápida
- Teste incremental
- Menos dados para debugar

---

### **Cenário 3: Foco Acadêmico Local** 🇧🇷
```bash
python populate_acoes_brasileiras.py
```
**Por quê?**
- Requisito mínimo atendido
- Ações conhecidas
- Mais fácil explicar

---

### **Cenário 4: Demo Rápida com Gráfico** 📊
```bash
python populate_and_optimize.py
```
**Por quê?**
- Visual impactante
- Rápido (~5 min)
- Fronteira Eficiente plotada

---

## 📈 Dados Carregados

### **Brasil (37 ações)** 🇧🇷

| Setor | Quantidade | Exemplos |
|-------|-----------|----------|
| Energia | 3 | PETR3, PETR4, PRIO3 |
| Mineração | 1 | VALE3 |
| Bancos | 4 | ITUB4, BBDC4, BBAS3, SANB11 |
| Varejo | 4 | MGLU3, LREN3, AMER3, PCAR3 |
| Indústria | 3 | WEGE3, EMBR3, RAIL3 |
| **Total** | **37** | 15 setores |

**Período**: Últimos 6 meses (~126 pregões)  
**Total de preços**: ~4.600

---

### **Estados Unidos (60 ações)** 🇺🇸

| Setor | Quantidade | Exemplos |
|-------|-----------|----------|
| Technology | 16 | AAPL, MSFT, GOOGL, NVDA, META |
| Financial | 8 | JPM, BAC, V, MA, GS |
| Healthcare | 7 | JNJ, UNH, PFE, ABBV, LLY |
| Consumer | 9 | KO, PEP, WMT, COST, NKE |
| Industrials | 5 | BA, CAT, GE, UPS, HON |
| Energy | 3 | XOM, CVX, COP |
| **Total** | **60** | 11 setores |

**Período**: Últimos 6 meses (~126 dias)  
**Total de preços**: ~7.500

---

### **Global (97 ações)** 🌎

**Total combinado**:
- Ações: 97
- Setores: 20+ (únicos)
- Preços históricos: ~12.000
- Período: 6 meses
- Diversificação: ⭐⭐⭐⭐⭐

---

## 🛠️ Troubleshooting

### **Problema 1: "ModuleNotFoundError: No module named 'yfinance'"**
```bash
pip install yfinance pandas requests
```

### **Problema 2: "Cannot connect to host"**
**Solução**: VM Azure não está rodando
```bash
cd terraform
terraform apply
```

### **Problema 3: Rate limiting (muitos erros 429)**
**Solução**: Yahoo Finance bloqueou temporariamente
- Aguardar 10-15 minutos
- Executar scripts separadamente (BR primeiro, depois US)

### **Problema 4: "HTTP 409 - Ticker já cadastrado"**
**Solução**: Isso é NORMAL! O script é idempotente
- Ativos já existentes são pulados automaticamente
- Apenas novos preços são adicionados

### **Problema 5: Script trava/demora muito**
**Solução**: Conexão lenta ou Yahoo Finance instável
- Verificar internet
- Aguardar e tentar novamente
- Usar `populate_all.py` (tem delays entre mercados)

---

## 💡 Dicas Avançadas

### **1. Atualização Mensal**
```bash
# Re-executar mensalmente para manter dados atualizados
python populate_all.py

# O script é idempotente:
# - Ativos já cadastrados: pula
# - Preços duplicados: ignora
# - Novos preços: adiciona
```

### **2. Verificar Dados Carregados**
```bash
# Via API
curl http://20.195.170.160:8082/api/assets

# Quantidade de ativos
curl http://20.195.170.160:8082/api/assets | jq 'length'

# Ver preços de um ativo específico
curl http://20.195.170.160:8082/api/assets/AAPL
```

### **3. Limpar Banco (Reset)**
```bash
# SSH na VM
ssh azureuser@20.195.170.160

# Reiniciar containers (apaga dados)
docker compose down -v
docker compose up -d

# Aguardar serviços ficarem UP (~2 min)
# Depois re-popular
python populate_all.py
```

### **4. Popular Apenas Ativos Específicos**
Edite o script Python e comente/descomente ações:

```python
# Em populate_acoes_usa.py
ACOES_USA = [
    {"ticker": "AAPL", ...},  # ✅ Manter
    {"ticker": "MSFT", ...},  # ✅ Manter
    # {"ticker": "IBM", ...},  # ❌ Comentar para pular
]
```

---

## 🎓 Valor Acadêmico

### **Antes (sem scripts)**
- ❌ Dados mockados/inventados
- ❌ Poucas ações
- ❌ Sem diversificação

### **Depois (com scripts)**
- ✅ **97 ações reais** de 2 mercados
- ✅ **~12.000 preços históricos**
- ✅ **6 meses** de dados atualizados
- ✅ **20+ setores** diversos
- ✅ Demonstração **profissional**

### **Impacto na Nota**
- **Desenvolvimento Móvel**: Dados reais impressionam
- **APIs & Microsserviços**: Sistema funcional completo
- **DevOps**: Deploy real com dados reais

**Resultado**: Projeto de nível profissional! 🏆

---

## 🎮 Testando no App Godot

### **Após popular, você pode:**

1. **Ver todas as ações** organizadas por setor
2. **Criar carteira 100% brasileira**
   - Ex: PETR4, VALE3, ITUB4, WEGE3, BBAS3
3. **Criar carteira 100% americana**
   - Ex: AAPL, MSFT, GOOGL, NVDA, JPM
4. **Criar carteira global mista**
   - Ex: AAPL, MSFT, VALE3, PETR4, ITUB4, JPM
5. **Comparar resultados**
   - Retorno esperado
   - Risco
   - Índice de Sharpe

---

## 📊 Exemplos de Carteiras

### **1. Conservative Global** 🛡️
```
JNJ, PG, KO, VALE3, ITUB4, BBAS3
```
- Risco: Baixo
- Retorno: Moderado
- Diversificação: Alta

### **2. Aggressive Tech** 🚀
```
NVDA, TSLA, AMD, META, MSFT
```
- Risco: Alto
- Retorno: Alto potencial
- Volatilidade: Alta

### **3. Balanced Global** ⚖️
```
AAPL, MSFT, VALE3, PETR4, JPM, ITUB4, WEGE3, BA
```
- Risco: Moderado
- Retorno: Moderado
- Diversificação: Excelente

### **4. Dividend Hunters** 💰
```
KO, PEP, JNJ, VALE3, ITUB4, XOM
```
- Risco: Baixo
- Retorno: Dividendos altos
- Estabilidade: Alta

---

## 📞 Suporte

### **Problemas Comuns**
- **Timeout**: Aguardar e tentar novamente
- **409 Conflict**: Normal, ativo já existe
- **Rate limit**: Aguardar 10-15 min

### **Logs Úteis**
```bash
# Na VM Azure
ssh azureuser@20.195.170.160
docker compose logs asset-service -f
```

### **Documentação Adicional**
- `README_POPULACAO.md` - Detalhes Brasil
- `README_ACOES_USA.md` - Detalhes EUA
- `QUICK_START_POPULACAO.md` - Quick start

---

## ✅ Checklist Final

Antes da apresentação:

- [ ] Executar `python populate_all.py`
- [ ] Verificar: ~97 ações cadastradas
- [ ] Testar no Godot: criar 3 carteiras diferentes
- [ ] Preparar demonstração: BR vs US vs Global
- [ ] Anotar resultados (retorno, risco, Sharpe)
- [ ] Preparar explicação sobre algoritmo de Markowitz

---

## 🎉 Resultado Esperado

```
======================================================================
  🎉 POPULAÇÃO COMPLETA — SUCESSO TOTAL!
======================================================================

  ✅ Banco de dados populado com ~97 ações (Brasil + EUA)
  ✅ Dados dos últimos 6 meses disponíveis
  ✅ Sistema pronto para otimização de carteiras globais!

  ℹ️  Próximos passos:
     1. Abra o app Godot (F5)
     2. Explore as ações brasileiras e americanas
     3. Crie carteiras mistas para diversificação internacional
     4. Compare resultados: BR vs US vs Global
```

---

**Pronto para começar?**

```bash
python populate_all.py
```

**Tempo total**: ~30 minutos  
**Resultado**: Sistema 100% funcional com dados reais! 🚀

---

**Última atualização**: 22/12/2024  
**Versão**: 1.0  
**Ações totais**: 97 (37 BR + 60 US)
