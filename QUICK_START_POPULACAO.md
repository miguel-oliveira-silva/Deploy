# 🚀 Quick Start - População de Dados

## ⚡ TL;DR - 3 Comandos

```bash
# 1. Instalar dependências
pip install -r requirements.txt

# 2. Verificar se VM está rodando
curl http://20.195.170.160:8081/actuator/health

# 3. Popular banco com 37 ações brasileiras (6 meses)
python populate_acoes_brasileiras.py
```

**Tempo total**: ~10-15 minutos

---

## 📊 O que será populado?

- **37 ações brasileiras** do Ibovespa
- **Últimos 6 meses** de dados reais
- **~126 pregões** por ação (média)
- **~4.410 preços históricos** no total

---

## ✅ Resultado Esperado

Após executar, você terá:

1. ✅ **35+ ativos cadastrados** no asset-service
2. ✅ **~4.400 preços históricos** no banco PostgreSQL
3. ✅ **App Godot pronto** para criar carteiras
4. ✅ **Algoritmo de Markowitz** funcionando com dados reais

---

## 🎮 Testando no App

1. Abrir Godot (F5)
2. Selecionar 2-10 ativos
3. Clicar "Continuar"
4. Ver carteira otimizada! 🎉

---

## 📈 Setores Incluídos

- 🛢️ Energia (Petrobras, PRIO)
- ⛏️ Mineração (Vale)
- 🏦 Bancos (Itaú, Bradesco, BB, Santander)
- 🛍️ Varejo (Magazine Luiza, Renner)
- 🏭 Indústria (WEG, Embraer)
- 🍺 Alimentos (Ambev, JBS, BRF)
- 📡 Telecom (Vivo, Tim)
- ⚡ Energia Elétrica (Eletrobras, Cemig, CPFL)
- 🏗️ Construção (Cyrela, MRV)
- 🌳 Papel (Suzano)
- 🏢 Siderurgia (CSN, Gerdau)
- 💊 Saúde (Raia Drogasil, Hapvida)
- 💻 Tecnologia (TOTVS)
- 🏬 Imobiliário (Multiplan, BR Malls)
- 🛡️ Seguros (BB Seguridade)

---

## ⚠️ Problemas Comuns

### VM não está rodando
```bash
cd terraform
terraform apply
```

### Dependências faltando
```bash
pip install yfinance pandas requests
```

### Dados já existem
✅ Normal! Script pula automaticamente

---

## 📚 Documentação Completa

Consulte `README_POPULACAO.md` para:
- Detalhes técnicos
- Troubleshooting completo
- Lista completa de ações
- Como atualizar mensalmente

---

**Pronto para começar?** Execute o comando acima! 🚀
