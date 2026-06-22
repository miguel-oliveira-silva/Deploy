#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
populate_acoes_brasileiras.py

Popula o banco de dados Markovitz com dados REAIS dos últimos 6 meses
de todas as principais ações brasileiras (Ibovespa).

Dependências:
    pip install requests yfinance pandas

Uso:
    python populate_acoes_brasileiras.py
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

# Principais ações brasileiras do Ibovespa (ampliada)
ACOES_BRASILEIRAS = [
    # Petróleo & Gás
    {"ticker": "PETR3", "yahoo": "PETR3.SA", "name": "Petrobras ON",           "sector": "Energia"},
    {"ticker": "PETR4", "yahoo": "PETR4.SA", "name": "Petrobras PN",           "sector": "Energia"},
    {"ticker": "PRIO3", "yahoo": "PRIO3.SA", "name": "PRIO ON",                "sector": "Energia"},
    
    # Mineração
    {"ticker": "VALE3", "yahoo": "VALE3.SA", "name": "Vale ON",                "sector": "Mineração"},
    
    # Bancos
    {"ticker": "ITUB4", "yahoo": "ITUB4.SA", "name": "Itaú Unibanco PN",       "sector": "Financeiro"},
    {"ticker": "BBDC4", "yahoo": "BBDC4.SA", "name": "Bradesco PN",            "sector": "Financeiro"},
    {"ticker": "BBAS3", "yahoo": "BBAS3.SA", "name": "Banco do Brasil ON",     "sector": "Financeiro"},
    {"ticker": "SANB11","yahoo": "SANB11.SA","name": "Santander Units",        "sector": "Financeiro"},
    
    # Varejo
    {"ticker": "MGLU3", "yahoo": "MGLU3.SA", "name": "Magazine Luiza ON",      "sector": "Varejo"},
    {"ticker": "AMER3", "yahoo": "AMER3.SA", "name": "Americanas ON",          "sector": "Varejo"},
    {"ticker": "LREN3", "yahoo": "LREN3.SA", "name": "Lojas Renner ON",        "sector": "Varejo"},
    {"ticker": "PCAR3", "yahoo": "PCAR3.SA", "name": "Grupo Pão de Açúcar ON", "sector": "Varejo"},
    
    # Indústria
    {"ticker": "WEGE3", "yahoo": "WEGE3.SA", "name": "WEG ON",                 "sector": "Indústria"},
    {"ticker": "EMBR3", "yahoo": "EMBR3.SA", "name": "Embraer ON",             "sector": "Indústria"},
    {"ticker": "RAIL3", "yahoo": "RAIL3.SA", "name": "Rumo ON",                "sector": "Logística"},
    
    # Alimentos & Bebidas
    {"ticker": "ABEV3", "yahoo": "ABEV3.SA", "name": "Ambev ON",               "sector": "Alimentos"},
    {"ticker": "JBSS3", "yahoo": "JBSS3.SA", "name": "JBS ON",                 "sector": "Alimentos"},
    {"ticker": "BRFS3", "yahoo": "BRFS3.SA", "name": "BRF ON",                 "sector": "Alimentos"},
    
    # Telecomunicações
    {"ticker": "VIVT3", "yahoo": "VIVT3.SA", "name": "Vivo ON",                "sector": "Telecom"},
    {"ticker": "TIMS3", "yahoo": "TIMS3.SA", "name": "Tim ON",                 "sector": "Telecom"},
    
    # Utilities
    {"ticker": "ELET3", "yahoo": "ELET3.SA", "name": "Eletrobras ON",          "sector": "Energia Elétrica"},
    {"ticker": "ELET6", "yahoo": "ELET6.SA", "name": "Eletrobras PNB",         "sector": "Energia Elétrica"},
    {"ticker": "CMIG4", "yahoo": "CMIG4.SA", "name": "Cemig PN",               "sector": "Energia Elétrica"},
    {"ticker": "CPFE3", "yahoo": "CPFE3.SA", "name": "CPFL Energia ON",        "sector": "Energia Elétrica"},
    {"ticker": "SBSP3", "yahoo": "SBSP3.SA", "name": "Sabesp ON",              "sector": "Saneamento"},
    
    # Construção
    {"ticker": "CYRE3", "yahoo": "CYRE3.SA", "name": "Cyrela ON",              "sector": "Construção"},
    {"ticker": "MRVE3", "yahoo": "MRVE3.SA", "name": "MRV ON",                 "sector": "Construção"},
    
    # Papel & Celulose
    {"ticker": "SUZB3", "yahoo": "SUZB3.SA", "name": "Suzano ON",              "sector": "Papel"},
    
    # Siderurgia
    {"ticker": "CSNA3", "yahoo": "CSNA3.SA", "name": "CSN ON",                 "sector": "Siderurgia"},
    {"ticker": "GGBR4", "yahoo": "GGBR4.SA", "name": "Gerdau PN",              "sector": "Siderurgia"},
    {"ticker": "GOAU4", "yahoo": "GOAU4.SA", "name": "Gerdau Metalúrgica PN",  "sector": "Siderurgia"},
    
    # Saúde
    {"ticker": "RADL3", "yahoo": "RADL3.SA", "name": "Raia Drogasil ON",       "sector": "Saúde"},
    {"ticker": "HAPV3", "yahoo": "HAPV3.SA", "name": "Hapvida ON",             "sector": "Saúde"},
    
    # Tecnologia
    {"ticker": "TOTS3", "yahoo": "TOTS3.SA", "name": "TOTVS ON",               "sector": "Tecnologia"},
    
    # Shopping & Imóveis
    {"ticker": "MULT3", "yahoo": "MULT3.SA", "name": "Multiplan ON",           "sector": "Imobiliário"},
    {"ticker": "BRML3", "yahoo": "BRML3.SA", "name": "BR Malls ON",            "sector": "Imobiliário"},
    
    # Seguros
    {"ticker": "BBSE3", "yahoo": "BBSE3.SA", "name": "BB Seguridade ON",       "sector": "Seguros"},
]

# Período: últimos 6 meses
DATA_FIM = datetime.now()
DATA_INICIO = DATA_FIM - timedelta(days=180)  # ~6 meses
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
    header(f"Baixando Histórico (Últimos 6 Meses: {PERIODO_INICIO} → {PERIODO_FIM})")
    info(f"Total de ações: {len(ACOES_BRASILEIRAS)}")
    
    historicos = {}
    sucesso = 0
    falhas = 0
    
    for i, acao in enumerate(ACOES_BRASILEIRAS, 1):
        ticker = acao["ticker"]
        ticker_yf = acao["yahoo"]
        
        print(f"\n  [{i}/{len(ACOES_BRASILEIRAS)}] {ticker:8s} ({ticker_yf:12s})", end=" ")
        
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
            
            if len(preco_fechamento) < 20:  # Mínimo de 20 pregões
                print(f"❌ Poucos dados ({len(preco_fechamento)} pregões)")
                falhas += 1
                continue
            
            historicos[ticker] = preco_fechamento
            sucesso += 1
            
            primeiro = preco_fechamento.iloc[0]
            ultimo = preco_fechamento.iloc[-1]
            variacao = ((ultimo / primeiro) - 1) * 100
            
            emoji = "📈" if variacao > 0 else "📉"
            print(f"✅ {len(preco_fechamento):3d} pregões | "
                  f"R${primeiro:6.2f} → R${ultimo:6.2f} {emoji} {variacao:+6.2f}%")
            
            # Evita rate limit do Yahoo Finance
            time.sleep(0.2)
            
        except Exception as e:
            print(f"❌ Erro: {e}")
            falhas += 1
    
    print(f"\n  {'─'*70}")
    ok(f"Sucesso: {sucesso}/{len(ACOES_BRASILEIRAS)} ações")
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
    
    for acao in ACOES_BRASILEIRAS:
        ticker = acao["ticker"]
        
        # Só cadastra se tiver dados históricos
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
                ok(f"{ticker:8s} cadastrado (ID={asset_id})")
                cadastrados += 1
                
            elif r.status_code == 409:
                # Já existe — busca o ID
                r2 = requests.get(f"{BASE_URL['asset']}/api/assets/{ticker}", timeout=10)
                if r2.status_code == 200:
                    ids[ticker] = r2.json()["id"]
                    info(f"{ticker:8s} já cadastrado (ID={ids[ticker]})")
                    ja_existiam += 1
                else:
                    warn(f"{ticker:8s} já existe mas não conseguiu buscar ID")
                    erros += 1
            else:
                err(f"{ticker:8s} Erro HTTP {r.status_code}: {r.text[:100]}")
                erros += 1
                
        except Exception as e:
            err(f"{ticker:8s} Exceção: {e}")
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
        print(f"\n  {ticker:8s} ", end="")
        
        # Verifica quantos preços já existem
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
                    duplicados += 1  # Data já cadastrada
                else:
                    erros += 1
                    
            except Exception:
                erros += 1
            
            # Rate limiting suave
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
    header("Estatísticas dos Dados Carregados")
    
    if not historicos:
        warn("Nenhum dado carregado")
        return
    
    print(f"\n  {'Ticker':<8} {'Pregões':>8} {'Primeiro':>10} {'Último':>10} {'Variação':>10}")
    print(f"  {'-'*8} {'-'*8} {'-'*10} {'-'*10} {'-'*10}")
    
    for ticker in sorted(historicos.keys()):
        serie = historicos[ticker]
        primeiro = serie.iloc[0]
        ultimo = serie.iloc[-1]
        variacao = ((ultimo / primeiro) - 1) * 100
        
        emoji = "📈" if variacao > 0 else "📉"
        
        print(f"  {ticker:<8} {len(serie):>8} "
              f"R${primeiro:>8.2f} R${ultimo:>8.2f} "
              f"{emoji} {variacao:>+7.2f}%")
    
    print(f"\n  {'─'*70}")
    ok(f"Total: {len(historicos)} ações com dados completos")
    
    # Período real
    todas_datas = pd.concat(historicos.values()).index
    inicio_real = todas_datas.min()
    fim_real = todas_datas.max()
    print(f"  📅 Período real: {inicio_real.strftime('%Y-%m-%d')} → {fim_real.strftime('%Y-%m-%d')}")

# =============================================================================
# MAIN
# =============================================================================

def main():
    print("\n" + "=" * 70)
    print("  MARKOVITZ — POPULAÇÃO DE AÇÕES BRASILEIRAS (6 MESES)")
    print("=" * 70)
    print(f"  📊 Total de ações: {len(ACOES_BRASILEIRAS)}")
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
    print(f"\n  🎉 Banco de dados populado com {len(historicos)} ações brasileiras")
    print(f"  📊 Dados dos últimos 6 meses ({PERIODO_INICIO} → {PERIODO_FIM})")
    print(f"  🚀 Agora você pode criar e otimizar carteiras no aplicativo Godot!")
    print("\n  💡 Dica: No app, selecione de 2 a 10 ativos e clique em 'Continuar'")
    print("     para ver a carteira otimizada pelo algoritmo de Markowitz.\n")

if __name__ == "__main__":
    main()
