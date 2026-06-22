#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
populate_acoes_usa.py

Popula o banco de dados Markovitz com dados REAIS dos últimos 6 meses
das principais ações dos Estados Unidos (S&P 500, Dow Jones, NASDAQ).

Dependências:
    pip install requests yfinance pandas

Uso:
    python populate_acoes_usa.py
"""

import sys
import time
import requests
from datetime import datetime, timedelta
from typing import Dict, List
import warnings
warnings.filterwarnings("ignore")

# Fix Windows cp1252 encoding
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
if hasattr(sys.stderr, 'reconfigure'):
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

try:
    import yfinance as yf
    import pandas as pd
except ImportError:
    print("❌ Dependências não encontradas. Execute:")
    print("   pip install yfinance pandas requests")
    sys.exit(1)

# =============================================================================
# CONFIGURAÇÃO
# =============================================================================

BASE_URL = {
    "user":      "http://20.195.170.160:8081",
    "asset":     "http://20.195.170.160:8082",
    "portfolio": "http://20.195.170.160:8083",
}

# Principais ações dos EUA (Blue Chips + Tech Giants + Growth Stocks)
ACOES_USA = [
    # ===== FAANG + MAGNIFICENT 7 =====
    {"ticker": "AAPL",  "yahoo": "AAPL",  "name": "Apple Inc",                    "sector": "Technology"},
    {"ticker": "MSFT",  "yahoo": "MSFT",  "name": "Microsoft Corporation",        "sector": "Technology"},
    {"ticker": "GOOGL", "yahoo": "GOOGL", "name": "Alphabet Inc (Google) Class A","sector": "Technology"},
    {"ticker": "GOOG",  "yahoo": "GOOG",  "name": "Alphabet Inc (Google) Class C","sector": "Technology"},
    {"ticker": "AMZN",  "yahoo": "AMZN",  "name": "Amazon.com Inc",               "sector": "Consumer Cyclical"},
    {"ticker": "META",  "yahoo": "META",  "name": "Meta Platforms (Facebook)",    "sector": "Technology"},
    {"ticker": "TSLA",  "yahoo": "TSLA",  "name": "Tesla Inc",                    "sector": "Automotive"},
    {"ticker": "NVDA",  "yahoo": "NVDA",  "name": "NVIDIA Corporation",           "sector": "Technology"},
    
    # ===== TECHNOLOGY (Além das 7 Magníficas) =====
    {"ticker": "NFLX",  "yahoo": "NFLX",  "name": "Netflix Inc",                  "sector": "Technology"},
    {"ticker": "ORCL",  "yahoo": "ORCL",  "name": "Oracle Corporation",           "sector": "Technology"},
    {"ticker": "CRM",   "yahoo": "CRM",   "name": "Salesforce Inc",               "sector": "Technology"},
    {"ticker": "ADBE",  "yahoo": "ADBE",  "name": "Adobe Inc",                    "sector": "Technology"},
    {"ticker": "INTC",  "yahoo": "INTC",  "name": "Intel Corporation",            "sector": "Technology"},
    {"ticker": "AMD",   "yahoo": "AMD",   "name": "Advanced Micro Devices",       "sector": "Technology"},
    {"ticker": "CSCO",  "yahoo": "CSCO",  "name": "Cisco Systems Inc",            "sector": "Technology"},
    {"ticker": "IBM",   "yahoo": "IBM",   "name": "IBM Corporation",              "sector": "Technology"},
    
    # ===== FINANCE =====
    {"ticker": "JPM",   "yahoo": "JPM",   "name": "JPMorgan Chase & Co",          "sector": "Financial"},
    {"ticker": "BAC",   "yahoo": "BAC",   "name": "Bank of America Corp",         "sector": "Financial"},
    {"ticker": "WFC",   "yahoo": "WFC",   "name": "Wells Fargo & Company",        "sector": "Financial"},
    {"ticker": "GS",    "yahoo": "GS",    "name": "Goldman Sachs Group",          "sector": "Financial"},
    {"ticker": "MS",    "yahoo": "MS",    "name": "Morgan Stanley",               "sector": "Financial"},
    {"ticker": "V",     "yahoo": "V",     "name": "Visa Inc",                     "sector": "Financial"},
    {"ticker": "MA",    "yahoo": "MA",    "name": "Mastercard Inc",               "sector": "Financial"},
    {"ticker": "AXP",   "yahoo": "AXP",   "name": "American Express",             "sector": "Financial"},
    
    # ===== HEALTHCARE =====
    {"ticker": "JNJ",   "yahoo": "JNJ",   "name": "Johnson & Johnson",            "sector": "Healthcare"},
    {"ticker": "UNH",   "yahoo": "UNH",   "name": "UnitedHealth Group",           "sector": "Healthcare"},
    {"ticker": "PFE",   "yahoo": "PFE",   "name": "Pfizer Inc",                   "sector": "Healthcare"},
    {"ticker": "ABBV",  "yahoo": "ABBV",  "name": "AbbVie Inc",                   "sector": "Healthcare"},
    {"ticker": "TMO",   "yahoo": "TMO",   "name": "Thermo Fisher Scientific",     "sector": "Healthcare"},
    {"ticker": "MRK",   "yahoo": "MRK",   "name": "Merck & Co Inc",               "sector": "Healthcare"},
    {"ticker": "LLY",   "yahoo": "LLY",   "name": "Eli Lilly and Company",        "sector": "Healthcare"},
    
    # ===== CONSUMER GOODS =====
    {"ticker": "KO",    "yahoo": "KO",    "name": "Coca-Cola Company",            "sector": "Consumer Defensive"},
    {"ticker": "PEP",   "yahoo": "PEP",   "name": "PepsiCo Inc",                  "sector": "Consumer Defensive"},
    {"ticker": "PG",    "yahoo": "PG",    "name": "Procter & Gamble",             "sector": "Consumer Defensive"},
    {"ticker": "WMT",   "yahoo": "WMT",   "name": "Walmart Inc",                  "sector": "Consumer Defensive"},
    {"ticker": "COST",  "yahoo": "COST",  "name": "Costco Wholesale",             "sector": "Consumer Defensive"},
    {"ticker": "HD",    "yahoo": "HD",    "name": "Home Depot Inc",               "sector": "Consumer Cyclical"},
    {"ticker": "MCD",   "yahoo": "MCD",   "name": "McDonald's Corporation",       "sector": "Consumer Cyclical"},
    {"ticker": "NKE",   "yahoo": "NKE",   "name": "Nike Inc",                     "sector": "Consumer Cyclical"},
    {"ticker": "SBUX",  "yahoo": "SBUX",  "name": "Starbucks Corporation",        "sector": "Consumer Cyclical"},
    
    # ===== INDUSTRIALS =====
    {"ticker": "BA",    "yahoo": "BA",    "name": "Boeing Company",               "sector": "Industrials"},
    {"ticker": "CAT",   "yahoo": "CAT",   "name": "Caterpillar Inc",              "sector": "Industrials"},
    {"ticker": "GE",    "yahoo": "GE",    "name": "General Electric",             "sector": "Industrials"},
    {"ticker": "UPS",   "yahoo": "UPS",   "name": "United Parcel Service",        "sector": "Industrials"},
    {"ticker": "HON",   "yahoo": "HON",   "name": "Honeywell International",      "sector": "Industrials"},
    
    # ===== ENERGY =====
    {"ticker": "XOM",   "yahoo": "XOM",   "name": "Exxon Mobil Corporation",      "sector": "Energy"},
    {"ticker": "CVX",   "yahoo": "CVX",   "name": "Chevron Corporation",          "sector": "Energy"},
    {"ticker": "COP",   "yahoo": "COP",   "name": "ConocoPhillips",               "sector": "Energy"},
    
    # ===== TELECOMMUNICATIONS =====
    {"ticker": "T",     "yahoo": "T",     "name": "AT&T Inc",                     "sector": "Telecom"},
    {"ticker": "VZ",    "yahoo": "VZ",    "name": "Verizon Communications",       "sector": "Telecom"},
    
    # ===== ENTERTAINMENT & MEDIA =====
    {"ticker": "DIS",   "yahoo": "DIS",   "name": "Walt Disney Company",          "sector": "Entertainment"},
    {"ticker": "CMCSA", "yahoo": "CMCSA", "name": "Comcast Corporation",          "sector": "Entertainment"},
    
    # ===== SEMICONDUCTORS =====
    {"ticker": "AVGO",  "yahoo": "AVGO",  "name": "Broadcom Inc",                 "sector": "Technology"},
    {"ticker": "QCOM",  "yahoo": "QCOM",  "name": "Qualcomm Inc",                 "sector": "Technology"},
    {"ticker": "TXN",   "yahoo": "TXN",   "name": "Texas Instruments",            "sector": "Technology"},
    
    # ===== ELECTRIC VEHICLES & CLEAN ENERGY =====
    {"ticker": "F",     "yahoo": "F",     "name": "Ford Motor Company",           "sector": "Automotive"},
    {"ticker": "GM",    "yahoo": "GM",    "name": "General Motors",               "sector": "Automotive"},
    
    # ===== REAL ESTATE =====
    {"ticker": "AMT",   "yahoo": "AMT",   "name": "American Tower Corp",          "sector": "Real Estate"},
    
    # ===== UTILITIES =====
    {"ticker": "NEE",   "yahoo": "NEE",   "name": "NextEra Energy",               "sector": "Utilities"},
]

# Período: últimos 6 meses
DATA_FIM = datetime.now()
DATA_INICIO = DATA_FIM - timedelta(days=180)
PERIODO_INICIO = DATA_INICIO.strftime("%Y-%m-%d")
PERIODO_FIM = DATA_FIM.strftime("%Y-%m-%d")

# =============================================================================
# UTILITÁRIOS
# =============================================================================

def header(msg: str):
    print(f"\n{'='*70}")
    print(f"  {msg}")
    print(f"{'='*70}")

def ok(msg):   print(f"  ✅ {msg}")
def warn(msg): print(f"  ⚠️  {msg}")
def err(msg):  print(f"  ❌ {msg}")
def info(msg): print(f"  ℹ️  {msg}")

# =============================================================================
# STEP 1: VERIFICAR SAÚDE DOS SERVIÇOS
# =============================================================================

def check_health():
    header("Verificando Saúde dos Microsserviços")
    servicos = {
        "user-service":      f"{BASE_URL['user']}/actuator/health",
        "asset-service":     f"{BASE_URL['asset']}/actuator/health",
        "portfolio-service": f"{BASE_URL['portfolio']}/actuator/health",
    }
    
    todos_ok = True
    for nome, url in servicos.items():
        try:
            r = requests.get(url, timeout=10)
            status = r.json().get("status", "UNKNOWN")
            if status == "UP":
                ok(f"{nome}: {status}")
            else:
                warn(f"{nome}: {status}")
                todos_ok = False
        except Exception as e:
            err(f"{nome}: Falha na conexão ({e})")
            todos_ok = False
    
    if not todos_ok:
        print("\n  ⚠️  Alguns serviços não estão UP. Verifique se a VM Azure está rodando.")
        resposta = input("\n  Deseja continuar mesmo assim? (s/n): ")
        if resposta.lower() != 's':
            sys.exit(1)
    
    return todos_ok

# =============================================================================
# STEP 2: BAIXAR DADOS HISTÓRICOS
# =============================================================================

def baixar_historico() -> Dict[str, pd.Series]:
    header(f"Baixando Histórico US Stocks (Últimos 6 Meses: {PERIODO_INICIO} → {PERIODO_FIM})")
    info(f"Total de ações: {len(ACOES_USA)}")
    
    historicos = {}
    sucesso = 0
    falhas = 0
    
    for i, acao in enumerate(ACOES_USA, 1):
        ticker = acao["ticker"]
        ticker_yf = acao["yahoo"]
        
        print(f"\n  [{i}/{len(ACOES_USA)}] {ticker:6s} ({ticker_yf:6s})", end=" ")
        
        try:
            df = yf.download(
                ticker_yf,
                start=PERIODO_INICIO,
                end=PERIODO_FIM,
                progress=False,
                auto_adjust=True
            )
            
            if df.empty:
                print("❌ Sem dados")
                falhas += 1
                continue
            
            preco_fechamento = df["Close"].squeeze()
            preco_fechamento = preco_fechamento.dropna()
            
            if len(preco_fechamento) < 20:
                print(f"❌ Poucos dados ({len(preco_fechamento)} pregões)")
                falhas += 1
                continue
            
            historicos[ticker] = preco_fechamento
            sucesso += 1
            
            primeiro = preco_fechamento.iloc[0]
            ultimo = preco_fechamento.iloc[-1]
            variacao = ((ultimo / primeiro) - 1) * 100
            
            emoji = "📈" if variacao > 0 else "📉"
            print(f"✅ {len(preco_fechamento):3d} dias | "
                  f"${primeiro:7.2f} → ${ultimo:7.2f} {emoji} {variacao:+6.2f}%")
            
            # Rate limiting
            time.sleep(0.15)
            
        except Exception as e:
            print(f"❌ Erro: {e}")
            falhas += 1
    
    print(f"\n  {'─'*70}")
    ok(f"Sucesso: {sucesso}/{len(ACOES_USA)} ações")
    if falhas > 0:
        warn(f"Falhas: {falhas} ações sem dados suficientes")
    
    return historicos

# =============================================================================
# STEP 3: CADASTRAR ATIVOS NA API
# =============================================================================

def cadastrar_ativos(historicos: Dict[str, pd.Series]) -> Dict[str, int]:
    header("Cadastrando Ativos no Asset-Service")
    
    ids = {}
    cadastrados = 0
    ja_existiam = 0
    erros = 0
    
    for acao in ACOES_USA:
        ticker = acao["ticker"]
        
        if ticker not in historicos:
            continue
        
        url = f"{BASE_URL['asset']}/api/assets"
        payload = {
            "ticker": ticker,
            "name":   acao["name"],
            "sector": acao["sector"],
        }
        
        try:
            r = requests.post(url, json=payload, timeout=15)
            
            if r.status_code == 201:
                asset_id = r.json()["id"]
                ids[ticker] = asset_id
                ok(f"{ticker:6s} cadastrado (ID={asset_id})")
                cadastrados += 1
                
            elif r.status_code == 409:
                r2 = requests.get(f"{BASE_URL['asset']}/api/assets/{ticker}", timeout=10)
                if r2.status_code == 200:
                    ids[ticker] = r2.json()["id"]
                    info(f"{ticker:6s} já cadastrado (ID={ids[ticker]})")
                    ja_existiam += 1
                else:
                    warn(f"{ticker:6s} já existe mas não conseguiu buscar ID")
                    erros += 1
            else:
                err(f"{ticker:6s} Erro HTTP {r.status_code}: {r.text[:100]}")
                erros += 1
                
        except Exception as e:
            err(f"{ticker:6s} Exceção: {e}")
            erros += 1
    
    print(f"\n  {'─'*70}")
    ok(f"Novos: {cadastrados} | Já existiam: {ja_existiam} | Erros: {erros}")
    
    return ids

# =============================================================================
# STEP 4: ENVIAR HISTÓRICO DE PREÇOS
# =============================================================================

def enviar_precos(historicos: Dict[str, pd.Series], asset_ids: Dict[str, int]):
    header("Enviando Histórico de Preços")
    info(f"Processando {len(historicos)} ativos...")
    
    total_enviados = 0
    total_pulados = 0
    
    for ticker, serie in historicos.items():
        print(f"\n  {ticker:6s} ", end="")
        
        try:
            r_check = requests.get(f"{BASE_URL['asset']}/api/assets/{ticker}", timeout=10)
            if r_check.status_code == 200:
                ja_tem = r_check.json().get("priceCount", 0)
            else:
                ja_tem = 0
        except:
            ja_tem = 0
        
        if ja_tem >= len(serie):
            print(f"✅ Já possui {ja_tem} preços (pulando)")
            total_pulados += ja_tem
            continue
        
        print(f"({len(serie)} preços)", end=" ")
        
        url = f"{BASE_URL['asset']}/api/assets/{ticker}/prices"
        enviados = 0
        duplicados = 0
        erros = 0
        
        for data, preco in serie.items():
            date_str = data.strftime("%Y-%m-%d") if hasattr(data, "strftime") else str(data)[:10]
            payload = {
                "price": round(float(preco), 4),
                "priceDate": date_str
            }
            
            try:
                r = requests.post(url, json=payload, timeout=10)
                
                if r.status_code in (200, 201):
                    enviados += 1
                elif r.status_code == 409:
                    duplicados += 1
                else:
                    erros += 1
                    
            except Exception:
                erros += 1
            
            if enviados % 50 == 0 and enviados > 0:
                time.sleep(0.1)
        
        total_enviados += enviados
        total_pulados += duplicados
        
        if enviados > 0:
            print(f"✅ {enviados} enviados", end="")
        if duplicados > 0:
            print(f" | {duplicados} já existiam", end="")
        if erros > 0:
            print(f" | ❌ {erros} erros", end="")
        print()
    
    print(f"\n  {'─'*70}")
    ok(f"Total enviado: {total_enviados} preços")
    if total_pulados > 0:
        info(f"Já existiam: {total_pulados} preços")

# =============================================================================
# STEP 5: ESTATÍSTICAS FINAIS
# =============================================================================

def exibir_estatisticas(historicos: Dict[str, pd.Series]):
    header("Estatísticas dos Dados Carregados (TOP 15)")
    
    if not historicos:
        warn("Nenhum dado carregado")
        return
    
    # Calcula variações
    stats = []
    for ticker, serie in historicos.items():
        primeiro = serie.iloc[0]
        ultimo = serie.iloc[-1]
        variacao = ((ultimo / primeiro) - 1) * 100
        stats.append({
            "ticker": ticker,
            "pregoes": len(serie),
            "primeiro": primeiro,
            "ultimo": ultimo,
            "variacao": variacao
        })
    
    # Top 15 maiores valorizações
    stats_sorted = sorted(stats, key=lambda x: x["variacao"], reverse=True)[:15]
    
    print(f"\n  {'Ticker':<7} {'Dias':>6} {'Inicial':>10} {'Final':>10} {'Variação':>11}")
    print(f"  {'-'*7} {'-'*6} {'-'*10} {'-'*10} {'-'*11}")
    
    for s in stats_sorted:
        emoji = "📈" if s["variacao"] > 0 else "📉"
        print(f"  {s['ticker']:<7} {s['pregoes']:>6} "
              f"${s['primeiro']:>9.2f} ${s['ultimo']:>9.2f} "
              f"{emoji} {s['variacao']:>+7.2f}%")
    
    print(f"\n  {'─'*70}")
    ok(f"Total: {len(historicos)} ações US com dados completos")
    
    # Período real
    todas_datas = pd.concat(historicos.values()).index
    inicio_real = todas_datas.min()
    fim_real = todas_datas.max()
    print(f"  📅 Período real: {inicio_real.strftime('%Y-%m-%d')} → {fim_real.strftime('%Y-%m-%d')}")
    
    # Média de variação
    media_var = sum(s["variacao"] for s in stats) / len(stats)
    print(f"  📊 Variação média: {media_var:+.2f}%")

# =============================================================================
# MAIN
# =============================================================================

def main():
    print("\n" + "=" * 70)
    print("  MARKOVITZ — POPULAÇÃO DE AÇÕES DOS ESTADOS UNIDOS (6 MESES)")
    print("=" * 70)
    print(f"  🇺🇸 Total de ações: {len(ACOES_USA)}")
    print(f"  📅 Período: {PERIODO_INICIO} → {PERIODO_FIM}")
    print("=" * 70)
    
    # 1. Health check
    check_health()
    
    # 2. Baixar dados
    print("\n  ⏳ Iniciando download dos dados (isso pode levar alguns minutos)...")
    historicos = baixar_historico()
    
    if not historicos:
        err("Nenhum dado histórico disponível. Verifique sua conexão com a internet.")
        sys.exit(1)
    
    # 3. Cadastrar ativos
    asset_ids = cadastrar_ativos(historicos)
    
    # 4. Enviar preços
    if asset_ids:
        enviar_precos(historicos, asset_ids)
    else:
        warn("Nenhum ativo foi cadastrado. Não há preços para enviar.")
    
    # 5. Estatísticas
    exibir_estatisticas(historicos)
    
    # Conclusão
    header("✅ PROCESSO CONCLUÍDO COM SUCESSO!")
    print(f"\n  🎉 Banco de dados populado com {len(historicos)} ações dos EUA")
    print(f"  📊 Dados dos últimos 6 meses ({PERIODO_INICIO} → {PERIODO_FIM})")
    print(f"  🚀 Agora você pode criar carteiras globais no aplicativo!")
    print("\n  💡 Dica: Combine ações brasileiras + americanas para")
    print("     diversificação internacional!\n")

if __name__ == "__main__":
    main()
