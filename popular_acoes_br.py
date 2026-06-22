#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
popular_acoes_br.py
===================
Popula o asset-service com as 70 ações brasileiras de maior liquidez/volume,
adicionando 6 meses de histórico de preços de fechamento via Yahoo Finance.

Execução:
    pip install requests yfinance
    python popular_acoes_br.py

O script é IDEMPOTENTE: pode ser rodado múltiplas vezes sem duplicar dados.
Ativos já cadastrados (HTTP 409) e datas de preço repetidas são ignorados.
"""

import sys
import time
import requests
from datetime import date, timedelta

# ── Fix encoding Windows ──────────────────────────────────────────────────────
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

try:
    import yfinance as yf
except ImportError:
    print("❌  yfinance não instalado. Execute:  pip install yfinance")
    sys.exit(1)

# =============================================================================
# CONFIGURAÇÃO
# =============================================================================

ASSET_SERVICE_URL = "http://20.195.170.160:8082"

# Últimos 6 meses a partir de hoje
_HOJE       = date.today()
DATA_FIM    = _HOJE.strftime("%Y-%m-%d")
DATA_INICIO = (_HOJE - timedelta(days=182)).strftime("%Y-%m-%d")   # ~6 meses

# Pausa entre chamadas à API (segundos) — evita sobrecarregar o servidor
DELAY_ENTRE_PRECOS  = 0.0   # sem delay individual; enviamos em batches
DELAY_ENTRE_ATIVOS  = 0.5   # meio segundo entre cada ativo

# =============================================================================
# 70 AÇÕES BRASILEIRAS — maior liquidez / volume médio na B3
# Formato: ticker B3 | ticker Yahoo Finance (.SA) | nome | setor
# =============================================================================

ACOES = [
    # ── Blue Chips / Commodities ──────────────────────────────────────────────
    {"ticker": "PETR4",  "yahoo": "PETR4.SA",  "name": "Petrobras S.A. PN",           "sector": "Energia"},
    {"ticker": "PETR3",  "yahoo": "PETR3.SA",  "name": "Petrobras S.A. ON",           "sector": "Energia"},
    {"ticker": "VALE3",  "yahoo": "VALE3.SA",  "name": "Vale S.A. ON",                "sector": "Mineração"},
    {"ticker": "ITUB4",  "yahoo": "ITUB4.SA",  "name": "Itaú Unibanco PN",            "sector": "Financeiro"},
    {"ticker": "BBAS3",  "yahoo": "BBAS3.SA",  "name": "Banco do Brasil ON",          "sector": "Financeiro"},
    {"ticker": "BBDC4",  "yahoo": "BBDC4.SA",  "name": "Bradesco PN",                 "sector": "Financeiro"},
    {"ticker": "ABEV3",  "yahoo": "ABEV3.SA",  "name": "Ambev S.A. ON",               "sector": "Consumo"},
    {"ticker": "WEGE3",  "yahoo": "WEGE3.SA",  "name": "WEG S.A. ON",                 "sector": "Industrial"},
    {"ticker": "ITSA4",  "yahoo": "ITSA4.SA",  "name": "Itaúsa PN",                   "sector": "Financeiro"},
    {"ticker": "BBSE3",  "yahoo": "BBSE3.SA",  "name": "BB Seguridade ON",            "sector": "Seguros"},

    # ── Bancos e Seguradoras ──────────────────────────────────────────────────
    {"ticker": "SANB11", "yahoo": "SANB11.SA", "name": "Santander Brasil Unit",       "sector": "Financeiro"},
    {"ticker": "BPAC11", "yahoo": "BPAC11.SA", "name": "BTG Pactual Unit",            "sector": "Financeiro"},
    {"ticker": "PSSA3",  "yahoo": "PSSA3.SA",  "name": "Porto Seguro ON",             "sector": "Seguros"},
    {"ticker": "CXSE3",  "yahoo": "CXSE3.SA",  "name": "Caixa Seguridade ON",         "sector": "Seguros"},
    {"ticker": "IRBR3",  "yahoo": "IRBR3.SA",  "name": "IRB Brasil Resseguros ON",    "sector": "Seguros"},

    # ── Energia Elétrica ──────────────────────────────────────────────────────
    {"ticker": "ELET3",  "yahoo": "ELET3.SA",  "name": "Eletrobras ON",               "sector": "Utilidades"},
    {"ticker": "ELET6",  "yahoo": "ELET6.SA",  "name": "Eletrobras PNB",              "sector": "Utilidades"},
    {"ticker": "EQTL3",  "yahoo": "EQTL3.SA",  "name": "Equatorial Energia ON",       "sector": "Utilidades"},
    {"ticker": "CPFE3",  "yahoo": "CPFE3.SA",  "name": "CPFL Energia ON",             "sector": "Utilidades"},
    {"ticker": "ENEV3",  "yahoo": "ENEV3.SA",  "name": "Eneva ON",                    "sector": "Energia"},
    {"ticker": "CMIG4",  "yahoo": "CMIG4.SA",  "name": "Cemig PN",                    "sector": "Utilidades"},
    {"ticker": "CPLE6",  "yahoo": "CPLE6.SA",  "name": "Copel PNB",                   "sector": "Utilidades"},
    {"ticker": "ENGI11", "yahoo": "ENGI11.SA", "name": "Energisa Unit",               "sector": "Utilidades"},
    {"ticker": "TAEE11", "yahoo": "TAEE11.SA", "name": "Taesa Unit",                  "sector": "Utilidades"},

    # ── Petróleo e Gás ────────────────────────────────────────────────────────
    {"ticker": "PRIO3",  "yahoo": "PRIO3.SA",  "name": "PetroRio ON",                 "sector": "Energia"},
    {"ticker": "RRRP3",  "yahoo": "RRRP3.SA",  "name": "3R Petroleum ON",             "sector": "Energia"},
    {"ticker": "CSAN3",  "yahoo": "CSAN3.SA",  "name": "Cosan ON",                    "sector": "Energia"},

    # ── Siderurgia e Mineração ────────────────────────────────────────────────
    {"ticker": "GGBR4",  "yahoo": "GGBR4.SA",  "name": "Gerdau PN",                   "sector": "Mineração"},
    {"ticker": "CSNA3",  "yahoo": "CSNA3.SA",  "name": "CSN ON",                      "sector": "Mineração"},
    {"ticker": "USIM5",  "yahoo": "USIM5.SA",  "name": "Usiminas PNA",                "sector": "Mineração"},
    {"ticker": "BRAP4",  "yahoo": "BRAP4.SA",  "name": "Bradespar PN",                "sector": "Mineração"},
    {"ticker": "KLBN11", "yahoo": "KLBN11.SA", "name": "Klabin Unit",                 "sector": "Industrial"},
    {"ticker": "SUZB3",  "yahoo": "SUZB3.SA",  "name": "Suzano ON",                   "sector": "Industrial"},

    # ── Varejo e Consumo ──────────────────────────────────────────────────────
    {"ticker": "MGLU3",  "yahoo": "MGLU3.SA",  "name": "Magazine Luiza ON",           "sector": "Varejo"},
    {"ticker": "LREN3",  "yahoo": "LREN3.SA",  "name": "Lojas Renner ON",             "sector": "Varejo"},
    {"ticker": "SOMA3",  "yahoo": "SOMA3.SA",  "name": "Grupo Soma ON",               "sector": "Varejo"},
    {"ticker": "NTCO3",  "yahoo": "NTCO3.SA",  "name": "Natura &Co ON",               "sector": "Consumo"},
    {"ticker": "PETZ3",  "yahoo": "PETZ3.SA",  "name": "Petz ON",                     "sector": "Varejo"},
    {"ticker": "ASAI3",  "yahoo": "ASAI3.SA",  "name": "Assaí Atacadista ON",         "sector": "Varejo"},
    {"ticker": "PCAR3",  "yahoo": "PCAR3.SA",  "name": "GPA ON",                      "sector": "Varejo"},

    # ── Saúde ─────────────────────────────────────────────────────────────────
    {"ticker": "RDOR3",  "yahoo": "RDOR3.SA",  "name": "Rede D'Or ON",                "sector": "Saúde"},
    {"ticker": "HAPV3",  "yahoo": "HAPV3.SA",  "name": "Hapvida ON",                  "sector": "Saúde"},
    {"ticker": "FLRY3",  "yahoo": "FLRY3.SA",  "name": "Fleury ON",                   "sector": "Saúde"},
    {"ticker": "RADL3",  "yahoo": "RADL3.SA",  "name": "Raia Drogasil ON",            "sector": "Saúde"},

    # ── Logística e Transporte ────────────────────────────────────────────────
    {"ticker": "RAIL3",  "yahoo": "RAIL3.SA",  "name": "Rumo ON",                     "sector": "Logística"},
    {"ticker": "RENT3",  "yahoo": "RENT3.SA",  "name": "Localiza ON",                 "sector": "Logística"},
    {"ticker": "MOVI3",  "yahoo": "MOVI3.SA",  "name": "Movida ON",                   "sector": "Transporte"},
    {"ticker": "SIMH3",  "yahoo": "SIMH3.SA",  "name": "Simpar ON",                   "sector": "Logística"},
    {"ticker": "EMBR3",  "yahoo": "EMBR3.SA",  "name": "Embraer ON",                  "sector": "Industrial"},
    {"ticker": "AZUL4",  "yahoo": "AZUL4.SA",  "name": "Azul PN",                     "sector": "Transporte"},
    {"ticker": "GOLL4",  "yahoo": "GOLL4.SA",  "name": "Gol PN",                      "sector": "Transporte"},

    # ── Saneamento e Infraestrutura ───────────────────────────────────────────
    {"ticker": "SBSP3",  "yahoo": "SBSP3.SA",  "name": "Sabesp ON",                   "sector": "Utilidades"},
    {"ticker": "SAPR11", "yahoo": "SAPR11.SA", "name": "Sanepar Unit",                "sector": "Utilidades"},
    {"ticker": "TRPL4",  "yahoo": "TRPL4.SA",  "name": "CTEEP PN",                    "sector": "Utilidades"},

    # ── Telecomunicações ──────────────────────────────────────────────────────
    {"ticker": "VIVT3",  "yahoo": "VIVT3.SA",  "name": "Telefonica Brasil ON",        "sector": "Tecnologia"},
    {"ticker": "TIMS3",  "yahoo": "TIMS3.SA",  "name": "TIM ON",                      "sector": "Tecnologia"},

    # ── Tecnologia ────────────────────────────────────────────────────────────
    {"ticker": "TOTS3",  "yahoo": "TOTS3.SA",  "name": "Totvs ON",                    "sector": "Tecnologia"},
    {"ticker": "INTB3",  "yahoo": "INTB3.SA",  "name": "Intelbras ON",                "sector": "Tecnologia"},
    {"ticker": "LWSA3",  "yahoo": "LWSA3.SA",  "name": "Locaweb ON",                  "sector": "Tecnologia"},

    # ── Alimentos e Agro ──────────────────────────────────────────────────────
    {"ticker": "JBSS3",  "yahoo": "JBSS3.SA",  "name": "JBS ON",                      "sector": "Consumo"},
    {"ticker": "MRFG3",  "yahoo": "MRFG3.SA",  "name": "Marfrig ON",                  "sector": "Consumo"},
    {"ticker": "BRFS3",  "yahoo": "BRFS3.SA",  "name": "BRF ON",                      "sector": "Consumo"},
    {"ticker": "BEEF3",  "yahoo": "BEEF3.SA",  "name": "Minerva Foods ON",            "sector": "Consumo"},

    # ── Imobiliário / FIIs ────────────────────────────────────────────────────
    {"ticker": "HGLG11", "yahoo": "HGLG11.SA", "name": "CSHG Logística FII",          "sector": "Imobiliário"},
    {"ticker": "KNRI11", "yahoo": "KNRI11.SA", "name": "Kinea Renda Imobiliária FII", "sector": "Imobiliário"},
    {"ticker": "MXRF11", "yahoo": "MXRF11.SA", "name": "Maxi Renda FII",              "sector": "Imobiliário"},
    {"ticker": "XPML11", "yahoo": "XPML11.SA", "name": "XP Malls FII",               "sector": "Imobiliário"},
    {"ticker": "VISC11", "yahoo": "VISC11.SA", "name": "Vinci Shopping Centers FII",  "sector": "Imobiliário"},

    # ── ETFs ──────────────────────────────────────────────────────────────────
    {"ticker": "BOVA11", "yahoo": "BOVA11.SA", "name": "iShares Ibovespa ETF",        "sector": "ETF"},
    {"ticker": "IVVB11", "yahoo": "IVVB11.SA", "name": "iShares S&P 500 ETF",         "sector": "ETF"},
    {"ticker": "SMAL11", "yahoo": "SMAL11.SA", "name": "iShares Small Cap ETF",       "sector": "ETF"},
]

# =============================================================================
# UTILITÁRIOS DE LOG
# =============================================================================

def _sep(char="─", n=60):
    print(char * n)

def header(msg: str):
    print()
    _sep("═")
    print(f"  {msg}")
    _sep("═")

def ok(msg):    print(f"  ✅  {msg}")
def warn(msg):  print(f"  ⚠️   {msg}")
def erro(msg):  print(f"  ❌  {msg}")
def info(msg):  print(f"  ·   {msg}")
def step(n, total, msg): print(f"  [{n:>2}/{total}]  {msg}")

# =============================================================================
# PASSO 1 — HEALTH CHECK
# =============================================================================

def health_check() -> bool:
    header("PASSO 1 — Verificando asset-service")
    url = f"{ASSET_SERVICE_URL}/actuator/health"
    try:
        r = requests.get(url, timeout=10)
        status = r.json().get("status", "?")
        if status == "UP":
            ok(f"asset-service está UP  ({url})")
            return True
        else:
            warn(f"asset-service retornou status '{status}' — continuando mesmo assim")
            return True
    except Exception as e:
        erro(f"Não foi possível conectar ao asset-service: {e}")
        print()
        print("  Verifique se a Azure VM está rodando e os containers estão up.")
        print("  Comando útil (SSH na VM):  docker compose ps")
        return False

# =============================================================================
# PASSO 2 — BAIXAR HISTÓRICO DO YAHOO FINANCE
# =============================================================================

def baixar_historico() -> dict:
    """
    Baixa os últimos 6 meses de preço de fechamento de todas as 70 ações.
    Retorna: { "PETR4": [(data_str, preco), ...], ... }
    """
    header(f"PASSO 2 — Baixando histórico ({DATA_INICIO}  →  {DATA_FIM})")
    info(f"Período: últimos 6 meses  |  {len(ACOES)} ativos")
    print()

    historicos: dict[str, list] = {}
    falhas = []

    for i, acao in enumerate(ACOES, 1):
        ticker_yf = acao["yahoo"]
        step(i, len(ACOES), f"Baixando  {acao['ticker']:8s} ({ticker_yf}) ...")

        try:
            df = yf.download(
                ticker_yf,
                start=DATA_INICIO,
                end=DATA_FIM,
                progress=False,
                auto_adjust=True,
                actions=False,
            )

            if df.empty:
                warn(f"  Sem dados para {ticker_yf} — será ignorado")
                falhas.append(acao["ticker"])
                continue

            # Pega apenas a coluna Close e remove NaN
            close = df["Close"].squeeze().dropna()

            if len(close) < 2:
                warn(f"  {acao['ticker']}: menos de 2 pregões — ignorado")
                falhas.append(acao["ticker"])
                continue

            precos = []
            for data, preco in close.items():
                data_str = data.strftime("%Y-%m-%d") if hasattr(data, "strftime") else str(data)[:10]
                precos.append((data_str, round(float(preco), 4)))

            historicos[acao["ticker"]] = precos
            print(f"         → {len(precos):>3} pregões  |  "
                  f"R$ {precos[0][1]:.2f} → R$ {precos[-1][1]:.2f}")

        except Exception as e:
            warn(f"  Erro ao baixar {ticker_yf}: {e}")
            falhas.append(acao["ticker"])

    print()
    ok(f"Histórico baixado:  {len(historicos)} ativos com dados")
    if falhas:
        warn(f"Sem dados:          {len(falhas)} ativos ignorados → {', '.join(falhas)}")

    return historicos

# =============================================================================
# PASSO 3 — CADASTRAR ATIVOS NO asset-service
# =============================================================================

def cadastrar_ativos(historicos: dict) -> dict:
    """
    Cadastra no asset-service apenas os ativos que têm dados históricos.
    Retorna { "PETR4": <asset_id>, ... }
    """
    header("PASSO 3 — Cadastrando ativos no asset-service")

    ativos_com_dados = [a for a in ACOES if a["ticker"] in historicos]
    info(f"{len(ativos_com_dados)} ativos serão cadastrados")
    print()

    ids: dict[str, int] = {}
    cadastrados = 0
    ja_existiam = 0
    erros_cad   = 0

    for i, acao in enumerate(ativos_com_dados, 1):
        ticker = acao["ticker"]
        step(i, len(ativos_com_dados), f"Cadastrando  {ticker:8s} — {acao['name'][:35]}")

        payload = {
            "ticker": ticker,
            "name":   acao["name"],
            "sector": acao["sector"],
        }

        try:
            r = requests.post(
                f"{ASSET_SERVICE_URL}/api/assets",
                json=payload,
                timeout=15,
            )

            if r.status_code == 201:
                asset_id = r.json()["id"]
                ids[ticker] = asset_id
                cadastrados += 1
                print(f"         → ✅ criado  (id={asset_id})")

            elif r.status_code == 409:
                # Já existe — busca o ID atual
                r2 = requests.get(
                    f"{ASSET_SERVICE_URL}/api/assets/{ticker}",
                    timeout=10,
                )
                if r2.status_code == 200:
                    asset_id = r2.json()["id"]
                    ids[ticker] = asset_id
                    ja_existiam += 1
                    print(f"         → ⏭️  já existe (id={asset_id})")
                else:
                    erros_cad += 1
                    print(f"         → ⚠️  409 mas não conseguiu buscar ID")

            else:
                erros_cad += 1
                print(f"         → ❌ HTTP {r.status_code}: {r.text[:80]}")

        except Exception as e:
            erros_cad += 1
            print(f"         → ❌ Exceção: {e}")

    print()
    ok(f"Novos cadastros:   {cadastrados}")
    ok(f"Já existiam:       {ja_existiam}")
    if erros_cad:
        warn(f"Erros de cadastro: {erros_cad}")

    return ids

# =============================================================================
# PASSO 4 — ENVIAR PREÇOS HISTÓRICOS
# =============================================================================

def enviar_precos(historicos: dict, ids: dict) -> None:
    """
    Envia os preços de fechamento para cada ativo cadastrado.
    Ignora datas já existentes (HTTP 409).
    """
    header("PASSO 4 — Enviando preços históricos")

    ativos = [t for t in ids if t in historicos]
    info(f"{len(ativos)} ativos × ~{len(next(iter(historicos.values())))} pregões cada")
    print()

    total_enviados   = 0
    total_repetidos  = 0
    total_erros      = 0

    for i, ticker in enumerate(ativos, 1):
        precos = historicos[ticker]
        url    = f"{ASSET_SERVICE_URL}/api/assets/{ticker}/prices"

        # Verifica quantos preços já existem para não reenviar tudo
        try:
            r_info = requests.get(
                f"{ASSET_SERVICE_URL}/api/assets/{ticker}",
                timeout=10,
            )
            preco_count_atual = r_info.json().get("priceCount", 0) if r_info.ok else 0
        except Exception:
            preco_count_atual = 0

        step(i, len(ativos), f"{ticker:8s}  ({len(precos)} pregões)  "
             f"[já tem {preco_count_atual} no banco]")

        # Se já tem preços suficientes, pula
        if preco_count_atual >= len(precos):
            print(f"         → ⏭️  banco já completo — pulando")
            total_repetidos += len(precos)
            continue

        enviados  = 0
        repetidos = 0
        erros_p   = 0

        for data_str, preco in precos:
            try:
                r = requests.post(
                    url,
                    json={"price": preco, "priceDate": data_str},
                    timeout=10,
                )
                if r.status_code in (200, 201):
                    enviados += 1
                elif r.status_code == 409:
                    repetidos += 1   # data já existia
                else:
                    erros_p += 1
            except Exception:
                erros_p += 1

        total_enviados  += enviados
        total_repetidos += repetidos
        total_erros     += erros_p

        status_emoji = "✅" if erros_p == 0 else "⚠️ "
        print(f"         → {status_emoji}  {enviados} enviados  |  "
              f"{repetidos} repetidos  |  {erros_p} erros")

        # Pausa leve entre ativos para não sobrecarregar
        if DELAY_ENTRE_ATIVOS > 0:
            time.sleep(DELAY_ENTRE_ATIVOS)

    print()
    ok(f"Total preços novos enviados:  {total_enviados}")
    ok(f"Total datas repetidas (ok):   {total_repetidos}")
    if total_erros:
        warn(f"Total erros de envio:         {total_erros}")

# =============================================================================
# PASSO 5 — VERIFICAÇÃO FINAL
# =============================================================================

def verificar_resultado() -> None:
    header("PASSO 5 — Verificação final no banco")

    try:
        r = requests.get(f"{ASSET_SERVICE_URL}/api/assets", timeout=15)
        if not r.ok:
            warn(f"Não foi possível listar ativos: HTTP {r.status_code}")
            return

        ativos = r.json()
        info(f"Total de ativos cadastrados no banco: {len(ativos)}")
        print()

        # Ordena por priceCount decrescente
        ativos_ord = sorted(ativos, key=lambda a: a.get("priceCount", 0), reverse=True)

        # Agrupa por setor
        setores: dict[str, list] = {}
        sem_precos = []
        for a in ativos_ord:
            count = a.get("priceCount", 0)
            setor = a.get("sector", "Sem setor") or "Sem setor"
            if count == 0:
                sem_precos.append(a["ticker"])
            setores.setdefault(setor, []).append(a)

        print(f"  {'SETOR':<22}  {'ATIVOS':>6}  {'PREÇOS TOTAIS':>14}")
        _sep()
        for setor, lista in sorted(setores.items()):
            total_precos = sum(a.get("priceCount", 0) for a in lista)
            tickers_str  = ", ".join(a["ticker"] for a in lista)
            print(f"  {setor:<22}  {len(lista):>6}  {total_precos:>14,}   "
                  f"({tickers_str})")
        _sep()

        total_precos_global = sum(a.get("priceCount", 0) for a in ativos)
        print(f"  {'TOTAL':<22}  {len(ativos):>6}  {total_precos_global:>14,}")

        if sem_precos:
            print()
            warn(f"Ativos SEM preços ({len(sem_precos)}): {', '.join(sem_precos)}")
            warn("Estes não podem ser usados na otimização de Markowitz.")

    except Exception as e:
        warn(f"Erro na verificação: {e}")

# =============================================================================
# MAIN
# =============================================================================

def main():
    print()
    _sep("═", 60)
    print("  MARKOVITZ — POPULAR 70 AÇÕES BRASILEIRAS")
    print(f"  Período de preços: {DATA_INICIO}  →  {DATA_FIM}  (6 meses)")
    print(f"  Destino: {ASSET_SERVICE_URL}")
    _sep("═", 60)

    # ── PASSO 1: Health check ─────────────────────────────────────────────────
    if not health_check():
        print()
        print("  Abortando — asset-service inacessível.")
        sys.exit(1)

    # ── PASSO 2: Baixar histórico do Yahoo Finance ────────────────────────────
    historicos = baixar_historico()
    if not historicos:
        erro("Nenhum dado histórico baixado. Verifique sua conexão com a internet.")
        sys.exit(1)

    # ── PASSO 3: Cadastrar ativos ─────────────────────────────────────────────
    ids = cadastrar_ativos(historicos)
    if not ids:
        erro("Nenhum ativo foi cadastrado. Verifique o asset-service.")
        sys.exit(1)

    # ── PASSO 4: Enviar preços ────────────────────────────────────────────────
    enviar_precos(historicos, ids)

    # ── PASSO 5: Verificar resultado ──────────────────────────────────────────
    verificar_resultado()

    # ── Conclusão ─────────────────────────────────────────────────────────────
    print()
    _sep("═")
    print("  ✅  CONCLUÍDO!")
    print()
    print("  Os ativos já estão disponíveis na API e no app FORMA.")
    print("  No app, toque em '···' para ver as carteiras ou monte")
    print("  uma nova carteira com os ativos recém-cadastrados.")
    _sep("═")
    print()


if __name__ == "__main__":
    main()
