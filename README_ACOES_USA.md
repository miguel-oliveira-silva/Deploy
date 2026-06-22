# 🇺🇸 População de Ações dos Estados Unidos

## 🎯 Objetivo

Popular o banco de dados com as **principais ações dos Estados Unidos** dos últimos **6 meses**, incluindo as empresas mais valiosas e líquidas do mercado americano.

---

## 📊 Ações Incluídas (60+ Empresas)

### 🏆 **FAANG + Magnificent 7** (8)
As gigantes da tecnologia que dominam o mercado:
- **AAPL** - Apple Inc
- **MSFT** - Microsoft Corporation
- **GOOGL/GOOG** - Alphabet (Google)
- **AMZN** - Amazon.com
- **META** - Meta Platforms (Facebook)
- **TSLA** - Tesla
- **NVDA** - NVIDIA

### 💻 **Technology** (8 adicionais)
- **NFLX** - Netflix
- **ORCL** - Oracle
- **CRM** - Salesforce
- **ADBE** - Adobe
- **INTC** - Intel
- **AMD** - Advanced Micro Devices
- **CSCO** - Cisco Systems
- **IBM** - IBM

### 🏦 **Financial Services** (8)
- **JPM** - JPMorgan Chase
- **BAC** - Bank of America
- **WFC** - Wells Fargo
- **GS** - Goldman Sachs
- **MS** - Morgan Stanley
- **V** - Visa
- **MA** - Mastercard
- **AXP** - American Express

### 🏥 **Healthcare & Pharma** (7)
- **JNJ** - Johnson & Johnson
- **UNH** - UnitedHealth Group
- **PFE** - Pfizer
- **ABBV** - AbbVie
- **TMO** - Thermo Fisher Scientific
- **MRK** - Merck
- **LLY** - Eli Lilly

### 🛒 **Consumer Goods & Retail** (9)
- **KO** - Coca-Cola
- **PEP** - PepsiCo
- **PG** - Procter & Gamble
- **WMT** - Walmart
- **COST** - Costco
- **HD** - Home Depot
- **MCD** - McDonald's
- **NKE** - Nike
- **SBUX** - Starbucks

### 🏭 **Industrials** (5)
- **BA** - Boeing
- **CAT** - Caterpillar
- **GE** - General Electric
- **UPS** - United Parcel Service
- **HON** - Honeywell

### ⚡ **Energy** (3)
- **XOM** - Exxon Mobil
- **CVX** - Chevron
- **COP** - ConocoPhillips

### 📡 **Telecommunications** (2)
- **T** - AT&T
- **VZ** - Verizon

### 🎬 **Entertainment & Media** (2)
- **DIS** - Walt Disney
- **CMCSA** - Comcast

### 🔌 **Semiconductors** (3)
- **AVGO** - Broadcom
- **QCOM** - Qualcomm
- **TXN** - Texas Instruments

### 🚗 **Automotive** (2)
- **F** - Ford Motor
- **GM** - General Motors

### 🏢 **Real Estate** (1)
- **AMT** - American Tower

### 💡 **Utilities** (1)
- **NEE** - NextEra Energy

---

## 🚀 Como Usar

### **Pré-requisitos**
```bash
pip install -r requirements.txt
```

### **Executar**
```bash
python populate_acoes_usa.py
```

### **Tempo Estimado**
- Download: 3-8 minutos
- Cadastro: 20-30 segundos
- Envio de preços: 8-12 minutos
- **Total**: ~15-20 minutos

---

## 📈 Saída Esperada

```
======================================================================
  MARKOVITZ — POPULAÇÃO DE AÇÕES DOS ESTADOS UNIDOS (6 MESES)
======================================================================
  🇺🇸 Total de ações: 60
  📅 Período: 2024-06-22 → 2024-12-22
======================================================================

======================================================================
  Baixando Histórico US Stocks
======================================================================

  [1/60] AAPL   (AAPL  ) ✅ 126 dias | $175.43 → $195.71 📈 +11.56%
  [2/60] MSFT   (MSFT  ) ✅ 126 dias | $412.78 → $445.32 📈  +7.89%
  [3/60] GOOGL  (GOOGL ) ✅ 126 dias | $138.21 → $145.67 📈  +5.40%
  [4/60] NVDA   (NVDA  ) ✅ 126 dias | $425.50 → $495.22 📈 +16.38%
  ...

  ──────────────────────────────────────────────────────────────────────
  ✅ Sucesso: 58/60 ações

======================================================================
  Estatísticas dos Dados Carregados (TOP 15)
======================================================================

  Ticker   Dias    Inicial      Final   Variação
  ------- ------ ---------- ---------- -----------
  NVDA       126   $425.50    $495.22 📈 +16.38%
  TSLA       126   $182.45    $207.89 📈 +13.94%
  AAPL       126   $175.43    $195.71 📈 +11.56%
  META       126   $458.23    $510.67 📈 +11.44%
  ...

  ──────────────────────────────────────────────────────────────────────
  ✅ Total: 58 ações US com dados completos
  📅 Período real: 2024-06-22 → 2024-12-22
  📊 Variação média: +8.45%

======================================================================
  ✅ PROCESSO CONCLUÍDO COM SUCESSO!
======================================================================

  🎉 Banco de dados populado com 58 ações dos EUA
  📊 Dados dos últimos 6 meses
  🚀 Agora você pode criar carteiras globais!
```

---

## 🌎 Diversificação Internacional

### **Estratégia Recomendada**

Combine ações de diferentes mercados:

```python
# Carteira Global Diversificada
portfolio = [
    # US Tech (40%)
    "AAPL", "MSFT", "GOOGL", "NVDA",
    
    # US Finance (20%)
    "JPM", "V",
    
    # Brasil Commodities (20%)
    "VALE3", "PETR4",
    
    # Brasil Finance (20%)
    "ITUB4", "BBAS3"
]
```

---

## 📊 Vantagens das Ações Americanas

### ✅ **Liquidez**
- Mercado mais líquido do mundo
- Spreads menores
- Execução rápida

### ✅ **Diversificação Setorial**
- Tecnologia de ponta (FAANG+)
- Healthcare inovador
- Finance global
- Energy tradicional

### ✅ **Blue Chips**
- Empresas consolidadas
- Dividend aristocrats
- Menor volatilidade

### ✅ **Crescimento**
- Tech giants em expansão
- Inovação constante
- Mercado em alta histórica

---

## 🎯 Casos de Uso

### **1. Carteira Growth** 🚀
Foco em crescimento agressivo:
```
NVDA, TSLA, AMD, META, AMZN
```
**Características**: Alta volatilidade, alto retorno potencial

### **2. Carteira Value** 💼
Empresas consolidadas com dividendos:
```
JNJ, PG, KO, XOM, JPM
```
**Características**: Menor risco, dividendos consistentes

### **3. Carteira Tech** 💻
Dominância tecnológica:
```
AAPL, MSFT, GOOGL, NVDA, ORCL
```
**Características**: Liderança de mercado, inovação

### **4. Carteira Defensiva** 🛡️
Proteção em crises:
```
WMT, PG, JNJ, UNH, KO
```
**Características**: Resistente a recessões

### **5. Carteira Global** 🌍
Brasil + EUA balanceado:
```
AAPL, MSFT, VALE3, PETR4, ITUB4, JPM, BBAS3, NVDA
```
**Características**: Diversificação geográfica e cambial

---

## 💡 Dicas de Uso no App

### **No Godot:**

1. **Filtrar por País**
   - Ações brasileiras: terminam com número (PETR4, VALE3)
   - Ações americanas: letras puras (AAPL, MSFT)

2. **Diversificação Setorial**
   - Use a busca para encontrar setores específicos
   - Ex: "Tech", "Finance", "Energy"

3. **Carteiras Mistas**
   - Selecione 50% BR + 50% US
   - Protege contra desvalorização cambial

4. **Comparação de Mercados**
   - Crie 2 carteiras: uma só BR, outra só US
   - Compare retorno e risco

---

## 🔍 Comparação: Brasil vs EUA

| Aspecto | Brasil 🇧🇷 | Estados Unidos 🇺🇸 |
|---------|-----------|-------------------|
| **Ações disponíveis** | 37 | 60 |
| **Setores** | 15 | 11 |
| **Volatilidade** | Alta | Moderada |
| **Liquidez** | Média | Muito Alta |
| **Dividendos** | Altos | Moderados |
| **Crescimento** | Commodities | Tecnologia |
| **Moeda** | BRL (Real) | USD (Dólar) |

---

## ⚠️ Considerações Importantes

### **Câmbio**
- Ações americanas são em **USD**
- Variação cambial BRL/USD afeta retorno
- Diversificação cambial pode proteger patrimônio

### **Horário de Negociação**
- **NYSE/NASDAQ**: 10:30 - 17:00 (horário de Brasília)
- **B3**: 10:00 - 18:00 (horário de Brasília)

### **Impostos**
- **Brasil**: 15% sobre ganho de capital
- **EUA**: 30% sobre dividendos (residentes brasileiros)
- Consulte um contador para detalhes

---

## 📚 Recursos Adicionais

### **Fontes de Dados**
- [Yahoo Finance](https://finance.yahoo.com)
- [Investing.com](https://www.investing.com)
- [MarketWatch](https://www.marketwatch.com)

### **Índices de Referência**
- **S&P 500**: 500 maiores empresas
- **Dow Jones**: 30 blue chips industriais
- **NASDAQ-100**: 100 maiores não-financeiras
- **Russell 2000**: Small caps

### **ETFs Populares**
- **SPY**: S&P 500 tracker
- **QQQ**: NASDAQ-100 tracker
- **DIA**: Dow Jones tracker

---

## 🧪 Testando no App

### **Cenário 1: Tech Giants Portfolio**
```
Selecione: AAPL, MSFT, GOOGL, NVDA, META
Objetivo: MAX_SHARPE
Resultado esperado: Alta concentração em NVDA (melhor Sharpe)
```

### **Cenário 2: Defensive Portfolio**
```
Selecione: JNJ, PG, KO, WMT, UNH
Objetivo: MIN_RISK
Resultado esperado: Risco <15%, retorno moderado
```

### **Cenário 3: Global Diversification**
```
Selecione: AAPL, MSFT, VALE3, PETR4, JPM, ITUB4
Objetivo: MAX_SHARPE
Resultado esperado: Balanço entre crescimento US e valor BR
```

---

## ✅ Checklist de População

- [ ] Executar `populate_acoes_brasileiras.py` (37 ações BR)
- [ ] Executar `populate_acoes_usa.py` (60 ações US)
- [ ] Verificar total: **~97 ações** no banco
- [ ] Testar no app: criar carteira mista
- [ ] Comparar carteiras: BR vs US vs Mista

---

## 🎓 Valor Acadêmico

### **Demonstração Profissional**
- ✅ Mercado global (não só local)
- ✅ Blue chips reconhecidas mundialmente
- ✅ Diversificação internacional
- ✅ Dados reais de mercados maduros

### **Apresentação**
Mostre ao professor:
1. Carteira 100% brasileira
2. Carteira 100% americana
3. Carteira global 50/50
4. Compare resultados (retorno, risco, Sharpe)

**Impacto**: Projeto de nível profissional! 🏆

---

**Pronto para começar?**
```bash
python populate_acoes_usa.py
```

**Última atualização**: 22/12/2024
