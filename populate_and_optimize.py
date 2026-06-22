#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
populate_and_optimize.py

Preenche o banco da API Markovitz com dados reais de 5 ações brasileiras,
cria uma carteira, otimiza pelo algoritmo de Markowitz e plota a Fronteira Eficiente.

Dependências:
    pip install requests yfinance numpy pandas matplotlib scipy

Uso:
    python populate_and_optimize.py
"""

import sys
import time
import requests
import numpy as np
import pandas as pd
import matplotlib
import matplotlib.pyplot as plt
import matplotlib.patheffects as pe
from matplotlib.gridspec import GridSpec
from matplotlib.patches import FancyArrowPatch
import warnings
warnings.filterwarnings("ignore")

# Fix Windows cp1252 encoding — force UTF-8 output
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
if hasattr(sys.stderr, 'reconfigure'):
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

# ── Tenta importar yfinance ───────────────────────────────────────────────────
try:
    import yfinance as yf
except ImportError:
    print("❌ yfinance não encontrado. Execute: pip install yfinance")
    sys.exit(1)

# ── Tenta importar scipy (para a fronteira eficiente) ────────────────────────
try:
    from scipy.optimize import minimize
except ImportError:
    print("❌ scipy não encontrado. Execute: pip install scipy")
    sys.exit(1)

# =============================================================================
# CONFIGURAÇÃO
# =============================================================================

BASE_URL = {
    "user":      "http://20.195.170.160:8081",
    "asset":     "http://20.195.170.160:8082",
    "portfolio": "http://20.195.170.160:8083",
    "notify":    "http://20.195.170.160:8084",
}

# 5 ações brasileiras com histórico sólido — formato Yahoo Finance = ticker + ".SA"
ACOES = [
    {"ticker": "PETR4", "yahoo": "PETR4.SA", "name": "Petrobras S.A. PN",     "sector": "Energia"},
    {"ticker": "VALE3", "yahoo": "VALE3.SA", "name": "Vale S.A. ON",           "sector": "Mineração"},
    {"ticker": "ITUB4", "yahoo": "ITUB4.SA", "name": "Itaú Unibanco PN",      "sector": "Financeiro"},
    {"ticker": "WEGE3", "yahoo": "WEGE3.SA", "name": "WEG S.A. ON",            "sector": "Industria"},
    {"ticker": "BBAS3", "yahoo": "BBAS3.SA", "name": "Banco do Brasil ON",    "sector": "Financeiro"},
]

PERIODO_INICIO = "2023-01-01"
PERIODO_FIM    = "2024-12-31"

USUARIO = {
    "name":        "Markowitz Trader",
    "email":       "markowitz@trader.com",
    "password":    "senha123",
    "riskProfile": "AGRESSIVO",
}

CARTEIRA_NOME = "Fronteira Eficiente — Ações BR"
TAXA_SELIC    = 0.1075   # aprox. 10,75% a.a. como taxa livre de risco

# =============================================================================
# UTILITÁRIOS
# =============================================================================

def header(msg: str):
    print(f"\n{'='*60}")
    print(f"  {msg}")
    print(f"{'='*60}")

def ok(msg):   print(f"  [OK]   {msg}")
def warn(msg): print(f"  [WARN] {msg}")
def err(msg):  print(f"  [ERR]  {msg}")
def info(msg): print(f"  [...]  {msg}")


def check_health():
    header("Verificando saúde dos serviços")
    servicos = {
        "user-service":      f"{BASE_URL['user']}/actuator/health",
        "asset-service":     f"{BASE_URL['asset']}/actuator/health",
        "portfolio-service": f"{BASE_URL['portfolio']}/actuator/health",
        "notification-service": f"{BASE_URL['notify']}/actuator/health",
    }
    todos_ok = True
    for nome, url in servicos.items():
        try:
            r = requests.get(url, timeout=10)
            status = r.json().get("status", "?")
            if status == "UP":
                ok(f"{nome}: {status}")
            else:
                warn(f"{nome}: {status}")
                todos_ok = False
        except Exception as e:
            err(f"{nome}: {e}")
            todos_ok = False
    if not todos_ok:
        print("\n  [WARN] Alguns serviços não estão 'UP'. Continuando mesmo assim...")
    return todos_ok


# =============================================================================
# STEP 1: BAIXAR DADOS REAIS
# =============================================================================

def baixar_historico() -> dict[str, pd.Series]:
    header("Baixando histórico real via Yahoo Finance")
    historicos = {}
    for acao in ACOES:
        ticker_yf = acao["yahoo"]
        info(f"Baixando {acao['ticker']} ({ticker_yf}) de {PERIODO_INICIO} até {PERIODO_FIM}...")
        try:
            df = yf.download(ticker_yf, start=PERIODO_INICIO, end=PERIODO_FIM,
                             progress=False, auto_adjust=True)
            if df.empty:
                warn(f"Nenhum dado retornado para {ticker_yf}")
                continue
            preco_fechamento = df["Close"].squeeze()
            # Remove NaN
            preco_fechamento = preco_fechamento.dropna()
            historicos[acao["ticker"]] = preco_fechamento
            ok(f"{acao['ticker']}: {len(preco_fechamento)} pregões carregados  "
               f"| Preço inicial: R${preco_fechamento.iloc[0]:.2f}  "
               f"| Preço final: R${preco_fechamento.iloc[-1]:.2f}")
        except Exception as e:
            err(f"Erro ao baixar {ticker_yf}: {e}")
    return historicos


# =============================================================================
# STEP 2: CRIAR USUÁRIO
# =============================================================================

def criar_usuario() -> int:
    header("Criando usuário na API")
    url = f"{BASE_URL['user']}/api/users/register"
    try:
        r = requests.post(url, json=USUARIO, timeout=15)
        if r.status_code == 201:
            user_id = r.json()["id"]
            ok(f"Usuário criado! ID = {user_id}")
            return user_id
        elif r.status_code == 409:
            warn("E-mail já cadastrado — buscando usuário existente...")
            # Tenta listar e achar pelo email
            r2 = requests.get(f"{BASE_URL['user']}/api/users", timeout=10)
            if r2.status_code == 200:
                for u in r2.json():
                    if u["email"] == USUARIO["email"]:
                        ok(f"Usuário encontrado! ID = {u['id']}")
                        return u["id"]
            err("Não conseguiu recuperar ID do usuário existente")
            return 1
        else:
            err(f"Status {r.status_code}: {r.text}")
            return 1
    except Exception as e:
        err(f"Falha ao criar usuário: {e}")
        return 1


# =============================================================================
# STEP 3: CADASTRAR ATIVOS
# =============================================================================

def cadastrar_ativos() -> dict[str, int]:
    header("Cadastrando ativos no asset-service")
    ids = {}
    for acao in ACOES:
        url = f"{BASE_URL['asset']}/api/assets"
        payload = {
            "ticker": acao["ticker"],
            "name":   acao["name"],
            "sector": acao["sector"],
        }
        try:
            r = requests.post(url, json=payload, timeout=15)
            if r.status_code == 201:
                asset_id = r.json()["id"]
                ids[acao["ticker"]] = asset_id
                ok(f"{acao['ticker']} cadastrado! ID = {asset_id}")
            elif r.status_code == 409:
                warn(f"{acao['ticker']} já cadastrado — continuando...")
                r2 = requests.get(f"{BASE_URL['asset']}/api/assets/{acao['ticker']}", timeout=10)
                if r2.status_code == 200:
                    ids[acao["ticker"]] = r2.json()["id"]
            else:
                err(f"{acao['ticker']}: Status {r.status_code} — {r.text}")
        except Exception as e:
            err(f"Erro ao cadastrar {acao['ticker']}: {e}")
    return ids


# =============================================================================
# STEP 4: ENVIAR HISTÓRICO DE PREÇOS
# =============================================================================

def enviar_precos(historicos: dict[str, pd.Series]):
    header("Enviando histórico de preços para a API")
    for acao in ACOES:
        ticker = acao["ticker"]
        if ticker not in historicos:
            warn(f"{ticker}: sem dados históricos — pulando")
            continue

        serie = historicos[ticker]
        url = f"{BASE_URL['asset']}/api/assets/{ticker}/prices"

        # Verifica quantos preços já existem
        r_check = requests.get(f"{BASE_URL['asset']}/api/assets/{ticker}", timeout=10)
        ja_tem = 0
        if r_check.status_code == 200:
            ja_tem = r_check.json().get("priceCount", 0)

        if ja_tem >= len(serie):
            ok(f"{ticker}: já possui {ja_tem} preços — pulando envio")
            continue

        info(f"{ticker}: enviando {len(serie)} preços...")
        enviados = 0
        erros = 0
        for data, preco in serie.items():
            date_str = data.strftime("%Y-%m-%d") if hasattr(data, "strftime") else str(data)[:10]
            payload = {"price": round(float(preco), 4), "priceDate": date_str}
            try:
                r = requests.post(url, json=payload, timeout=10)
                if r.status_code in (200, 201):
                    enviados += 1
                elif r.status_code == 409:
                    pass  # Data já cadastrada
                else:
                    erros += 1
            except Exception:
                erros += 1
                time.sleep(0.1)

        ok(f"{ticker}: {enviados} preços enviados, {erros} erros")


# =============================================================================
# STEP 5: CRIAR E OTIMIZAR CARTEIRA
# =============================================================================

def criar_e_otimizar(user_id: int) -> dict:
    header("Criando e otimizando carteira")
    tickers = [a["ticker"] for a in ACOES]

    # Criar carteira
    payload = {
        "userId":           user_id,
        "name":             CARTEIRA_NOME,
        "tickers":          tickers,
        "optimizationGoal": "MAX_SHARPE",
    }
    r = requests.post(f"{BASE_URL['portfolio']}/api/portfolios", json=payload, timeout=20)
    if r.status_code not in (200, 201):
        err(f"Erro ao criar carteira: {r.status_code} — {r.text}")
        return {}

    portfolio_id = r.json()["id"]
    ok(f"Carteira criada! ID = {portfolio_id}")

    # Otimizar
    info("Executando algoritmo de Markowitz...")
    r2 = requests.post(f"{BASE_URL['portfolio']}/api/portfolios/{portfolio_id}/optimize", timeout=60)
    if r2.status_code != 200:
        err(f"Erro ao otimizar: {r2.status_code} — {r2.text}")
        return {}

    resultado = r2.json()
    ok(f"Carteira otimizada com sucesso!")
    print()
    print(f"  📊 Retorno esperado : {resultado.get('expectedReturn', 0)*100:.2f}% a.a.")
    print(f"  📊 Risco da carteira: {resultado.get('portfolioRisk', 0)*100:.2f}% a.a.")
    print(f"  📊 Índice de Sharpe : {resultado.get('sharpeRatio', 0):.4f}")
    print()
    print("  Pesos ótimos:")
    for ativo in resultado.get("assets", []):
        print(f"     {ativo['ticker']:6s}  {ativo['weight']*100:6.2f}%"
              f"  | retorno={ativo.get('expectedReturn', 0)*100:.1f}%"
              f"  | risco={ativo.get('risk', 0)*100:.1f}%")
    return resultado


# =============================================================================
# STEP 6: CALCULAR FRONTEIRA EFICIENTE LOCALMENTE
# =============================================================================

def calcular_fronteira_eficiente(historicos: dict[str, pd.Series]) -> dict:
    header("Calculando Fronteira Eficiente")

    # Alinha as séries em um DataFrame
    df = pd.DataFrame(historicos).dropna()
    tickers = list(df.columns)
    n = len(tickers)

    if n < 2:
        err("Precisa de pelo menos 2 ativos com dados para calcular a fronteira")
        return {}

    # Retornos logarítmicos diários
    log_ret = np.log(df / df.shift(1)).dropna()

    # Parâmetros anualizados
    mu    = log_ret.mean().values * 252          # vetor de retornos esperados
    cov   = log_ret.cov().values   * 252          # matriz de covariância
    info(f"Calculando com {n} ativos e {len(log_ret)} observações diárias")

    # ── Funções auxiliares ───────────────────────────────────────────────────
    def portfolio_stats(w):
        ret  = np.dot(w, mu)
        risk = np.sqrt(w @ cov @ w)
        return ret, risk

    def neg_sharpe(w):
        ret, risk = portfolio_stats(w)
        return -(ret - TAXA_SELIC) / risk if risk > 0 else 0

    def portfolio_risk(w):
        return portfolio_stats(w)[1]

    constraints = [{"type": "eq", "fun": lambda w: np.sum(w) - 1}]
    bounds      = [(0, 1)] * n
    w0          = np.array([1/n] * n)

    # ── Portfólio de máximo Sharpe ───────────────────────────────────────────
    res_sharpe = minimize(neg_sharpe, w0, method="SLSQP",
                          bounds=bounds, constraints=constraints)
    w_sharpe   = res_sharpe.x
    ret_sharpe, risk_sharpe = portfolio_stats(w_sharpe)
    sharpe_val  = (ret_sharpe - TAXA_SELIC) / risk_sharpe

    # ── Portfólio de mínimo risco ────────────────────────────────────────────
    res_minrisk = minimize(portfolio_risk, w0, method="SLSQP",
                           bounds=bounds, constraints=constraints)
    w_minrisk   = res_minrisk.x
    ret_minrisk, risk_minrisk = portfolio_stats(w_minrisk)

    # ── Fronteira eficiente (sweep de retorno-alvo) ──────────────────────────
    ret_min  = ret_minrisk
    ret_max  = mu.max()
    alvos    = np.linspace(ret_min, ret_max, 150)

    frontier_risks, frontier_rets = [], []
    for alvo in alvos:
        cons = constraints + [{"type": "eq", "fun": lambda w, a=alvo: np.dot(w, mu) - a}]
        res  = minimize(portfolio_risk, w0, method="SLSQP",
                        bounds=bounds, constraints=cons)
        if res.success:
            r, s = portfolio_stats(res.x)
            frontier_rets.append(r)
            frontier_risks.append(s)

    # ── Simulação de Monte Carlo ─────────────────────────────────────────────
    N_SIM   = 8_000
    sim_ret = np.zeros(N_SIM)
    sim_risk= np.zeros(N_SIM)
    sim_sh  = np.zeros(N_SIM)

    rng = np.random.default_rng(42)
    for i in range(N_SIM):
        w = rng.random(n); w /= w.sum()
        r_, s_ = portfolio_stats(w)
        sim_ret[i]  = r_
        sim_risk[i] = s_
        sim_sh[i]   = (r_ - TAXA_SELIC) / s_ if s_ > 0 else 0

    ok(f"Portfólio ótimo (Máx. Sharpe): retorno={ret_sharpe*100:.1f}%  risco={risk_sharpe*100:.1f}%  Sharpe={sharpe_val:.3f}")
    ok(f"Portfólio mínimo risco:         retorno={ret_minrisk*100:.1f}%  risco={risk_minrisk*100:.1f}%")

    return {
        "tickers":        tickers,
        "mu":             mu,
        "cov":            cov,
        "frontier_risks": frontier_risks,
        "frontier_rets":  frontier_rets,
        "sim_ret":        sim_ret,
        "sim_risk":       sim_risk,
        "sim_sh":         sim_sh,
        "w_sharpe":       w_sharpe,
        "ret_sharpe":     ret_sharpe,
        "risk_sharpe":    risk_sharpe,
        "sharpe_val":     sharpe_val,
        "w_minrisk":      w_minrisk,
        "ret_minrisk":    ret_minrisk,
        "risk_minrisk":   risk_minrisk,
    }


# =============================================================================
# STEP 7: PLOTAR FRONTEIRA EFICIENTE
# =============================================================================

def plotar(ef: dict, resultado_api: dict):
    header("Gerando gráfico da Fronteira Eficiente")

    matplotlib.rcParams.update({
        "font.family":  "DejaVu Sans",
        "font.size":    11,
        "axes.spines.top":   False,
        "axes.spines.right": False,
    })

    # ── Paleta ───────────────────────────────────────────────────────────────
    BG       = "#0d1117"
    CARD_BG  = "#161b22"
    GRID_C   = "#21262d"
    TEXT_C   = "#e6edf3"
    ACCENT1  = "#58a6ff"   # Azul
    ACCENT2  = "#3fb950"   # Verde
    ACCENT3  = "#f78166"   # Vermelho/laranja
    ACCENT4  = "#d2a8ff"   # Lilás
    GOLD     = "#ffc300"

    fig = plt.figure(figsize=(18, 12), facecolor=BG)
    fig.suptitle("Fronteira Eficiente de Markowitz — Ações Brasileiras",
                 color=TEXT_C, fontsize=20, fontweight="bold", y=0.97)

    gs = GridSpec(2, 3, figure=fig, hspace=0.45, wspace=0.38,
                  left=0.06, right=0.97, top=0.91, bottom=0.06)

    ax_main  = fig.add_subplot(gs[:, :2])   # Gráfico principal (2 linhas, 2 colunas)
    ax_pie   = fig.add_subplot(gs[0, 2])    # Pizza de pesos
    ax_bar   = fig.add_subplot(gs[1, 2])    # Barras risco/retorno

    # ─────────────────────────────────────────────────────────────────────────
    # Gráfico principal: Monte Carlo + Fronteira
    # ─────────────────────────────────────────────────────────────────────────
    ax_main.set_facecolor(CARD_BG)
    ax_main.tick_params(colors=TEXT_C)
    ax_main.xaxis.label.set_color(TEXT_C)
    ax_main.yaxis.label.set_color(TEXT_C)
    ax_main.set_xlabel("Risco Anualizado (σ)", fontsize=13)
    ax_main.set_ylabel("Retorno Esperado Anual (μ)", fontsize=13)
    ax_main.set_title("Espaço Risco-Retorno & Fronteira Eficiente",
                      color=TEXT_C, fontsize=14, pad=12)

    for sp in ax_main.spines.values():
        sp.set_color(GRID_C)
    ax_main.grid(color=GRID_C, linewidth=0.6, linestyle="--", alpha=0.5)

    # Simulação de Monte Carlo (fundo)
    sc = ax_main.scatter(ef["sim_risk"], ef["sim_ret"],
                         c=ef["sim_sh"], cmap="plasma",
                         s=5, alpha=0.35, linewidths=0, zorder=1)
    cbar = fig.colorbar(sc, ax=ax_main, pad=0.01)
    cbar.set_label("Índice de Sharpe", color=TEXT_C, fontsize=10)
    cbar.ax.yaxis.set_tick_params(color=TEXT_C)
    plt.setp(cbar.ax.yaxis.get_ticklabels(), color=TEXT_C)
    cbar.outline.set_edgecolor(GRID_C)

    # Fronteira Eficiente
    ax_main.plot(ef["frontier_risks"], ef["frontier_rets"],
                 color=ACCENT1, linewidth=3.5, zorder=3, label="Fronteira Eficiente")

    # Capital Market Line (CML)
    selic_ponto = (0, TAXA_SELIC)
    slope = (ef["ret_sharpe"] - TAXA_SELIC) / ef["risk_sharpe"]
    x_cml = np.linspace(0, max(ef["sim_risk"]) * 1.05, 100)
    y_cml = TAXA_SELIC + slope * x_cml
    ax_main.plot(x_cml, y_cml, color=ACCENT4, linewidth=1.8, linestyle="--",
                 alpha=0.7, zorder=2, label=f"CML (Selic ≈ {TAXA_SELIC*100:.1f}%)")

    # Ponto de Mínimo Risco
    ax_main.scatter(ef["risk_minrisk"], ef["ret_minrisk"],
                    color=ACCENT2, s=180, zorder=5, marker="D",
                    edgecolors="white", linewidths=1.2,
                    label=f"Mín. Risco  (μ={ef['ret_minrisk']*100:.1f}%, σ={ef['risk_minrisk']*100:.1f}%)")

    # Ponto de Máximo Sharpe
    ax_main.scatter(ef["risk_sharpe"], ef["ret_sharpe"],
                    color=GOLD, s=260, zorder=6, marker="*",
                    edgecolors="white", linewidths=1.2,
                    label=f"Máx. Sharpe (μ={ef['ret_sharpe']*100:.1f}%, σ={ef['risk_sharpe']*100:.1f}%)")

    # Anotação do ponto ótimo
    ax_main.annotate(
        f"Ótimo\nSharpe={ef['sharpe_val']:.3f}",
        xy=(ef["risk_sharpe"], ef["ret_sharpe"]),
        xytext=(ef["risk_sharpe"] + 0.02, ef["ret_sharpe"] - 0.06),
        color=GOLD, fontsize=10, fontweight="bold",
        arrowprops=dict(arrowstyle="->", color=GOLD, lw=1.5),
        bbox=dict(boxstyle="round,pad=0.3", fc=BG, ec=GOLD, alpha=0.8),
    )

    # Ativos individuais
    for i, ticker in enumerate(ef["tickers"]):
        r_i = ef["mu"][i]
        s_i = np.sqrt(ef["cov"][i, i])
        ax_main.scatter(s_i, r_i, marker="^", s=120, zorder=4,
                        edgecolors="white", linewidths=0.8,
                        color=[ACCENT1, ACCENT2, ACCENT3, ACCENT4, GOLD][i % 5])
        ax_main.annotate(ticker, (s_i, r_i),
                         textcoords="offset points", xytext=(6, 4),
                         color=TEXT_C, fontsize=9.5, fontweight="bold")

    # Formatar eixos como %
    ax_main.xaxis.set_major_formatter(matplotlib.ticker.PercentFormatter(xmax=1, decimals=0))
    ax_main.yaxis.set_major_formatter(matplotlib.ticker.PercentFormatter(xmax=1, decimals=0))

    legend = ax_main.legend(loc="upper left", fontsize=9.5,
                             facecolor=BG, edgecolor=GRID_C,
                             labelcolor=TEXT_C, framealpha=0.9)

    # ─────────────────────────────────────────────────────────────────────────
    # Pizza: Pesos da carteira ótima
    # ─────────────────────────────────────────────────────────────────────────
    ax_pie.set_facecolor(CARD_BG)
    ax_pie.set_title("Alocação Ótima\n(Máx. Sharpe)", color=TEXT_C, fontsize=12)

    tickers_ef = ef["tickers"]
    weights_ef = ef["w_sharpe"]
    colors_pie = [ACCENT1, ACCENT2, ACCENT3, ACCENT4, GOLD]
    explode    = [0.04] * len(tickers_ef)

    # Se tiver resultado da API, usa os pesos dela
    if resultado_api.get("assets"):
        api_pesos = {a["ticker"]: a["weight"] for a in resultado_api["assets"]}
        weights_ef = np.array([api_pesos.get(t, 0) for t in tickers_ef])

    wedges, texts, autotexts = ax_pie.pie(
        weights_ef, labels=tickers_ef, autopct="%1.1f%%",
        colors=colors_pie[:len(tickers_ef)], explode=explode,
        startangle=140, pctdistance=0.75,
        wedgeprops={"linewidth": 1.5, "edgecolor": BG},
    )
    for t in texts:    t.set_color(TEXT_C)
    for at in autotexts:
        at.set_color(BG)
        at.set_fontweight("bold")

    # ─────────────────────────────────────────────────────────────────────────
    # Barras: Risco e Retorno individual
    # ─────────────────────────────────────────────────────────────────────────
    ax_bar.set_facecolor(CARD_BG)
    ax_bar.tick_params(colors=TEXT_C)
    ax_bar.xaxis.label.set_color(TEXT_C)
    ax_bar.yaxis.label.set_color(TEXT_C)
    ax_bar.set_title("Retorno × Risco Individual", color=TEXT_C, fontsize=12)
    for sp in ax_bar.spines.values():
        sp.set_color(GRID_C)
    ax_bar.grid(axis="y", color=GRID_C, linewidth=0.6, linestyle="--", alpha=0.5)

    x      = np.arange(len(tickers_ef))
    width  = 0.35
    mus    = ef["mu"]
    sigmas = np.sqrt(np.diag(ef["cov"]))

    bars1 = ax_bar.bar(x - width/2, mus * 100, width, label="Retorno (μ)",
                       color=ACCENT2, alpha=0.85, edgecolor=BG)
    bars2 = ax_bar.bar(x + width/2, sigmas * 100, width, label="Risco (σ)",
                       color=ACCENT3, alpha=0.85, edgecolor=BG)

    ax_bar.set_xticks(x)
    ax_bar.set_xticklabels(tickers_ef, color=TEXT_C, fontsize=10)
    ax_bar.yaxis.set_major_formatter(matplotlib.ticker.PercentFormatter(decimals=0))
    ax_bar.legend(facecolor=BG, edgecolor=GRID_C, labelcolor=TEXT_C, fontsize=9)

    for bar in bars1:
        ax_bar.text(bar.get_x() + bar.get_width()/2,
                    bar.get_height() + 0.5,
                    f"{bar.get_height():.1f}%",
                    ha="center", va="bottom", color=TEXT_C, fontsize=8)
    for bar in bars2:
        ax_bar.text(bar.get_x() + bar.get_width()/2,
                    bar.get_height() + 0.5,
                    f"{bar.get_height():.1f}%",
                    ha="center", va="bottom", color=TEXT_C, fontsize=8)

    # ─────────────────────────────────────────────────────────────────────────
    # Rodapé com métricas da API
    # ─────────────────────────────────────────────────────────────────────────
    metricas = []
    if resultado_api:
        er = resultado_api.get("expectedReturn", 0) or 0
        pr = resultado_api.get("portfolioRisk",  0) or 0
        sh = resultado_api.get("sharpeRatio",    0) or 0
        metricas = [
            f"Retorno API: {er*100:.2f}% a.a.",
            f"Risco API: {pr*100:.2f}% a.a.",
            f"Sharpe API: {sh:.4f}",
            f"Taxa livre de risco (Selic): {TAXA_SELIC*100:.2f}%",
            f"Período: {PERIODO_INICIO} → {PERIODO_FIM}",
        ]
        rodape = "   |   ".join(metricas)
        fig.text(0.5, 0.01, rodape, ha="center", va="bottom",
                 color=TEXT_C, fontsize=9, alpha=0.7)

    # Salva
    caminho = "fronteira_eficiente.png"
    plt.savefig(caminho, dpi=160, bbox_inches="tight", facecolor=BG)
    plt.show()
    ok(f"Gráfico salvo em: {caminho}")
    return caminho


# =============================================================================
# MAIN
# =============================================================================

def main():
    print("\n" + "=" * 60)
    print("  MARKOVITZ -- PIPELINE COMPLETO DE OTIMIZACAO DE CARTEIRA")
    print("=" * 60)

    # 1. Health check
    check_health()

    # 2. Baixar dados reais
    historicos = baixar_historico()
    if not historicos:
        err("Nenhum dado histórico disponível. Verifique sua conexão com a internet.")
        sys.exit(1)

    # 3. Criar usuário
    user_id = criar_usuario()

    # 4. Cadastrar ativos
    cadastrar_ativos()

    # 5. Enviar histórico
    enviar_precos(historicos)

    # 6. Criar e otimizar carteira via API
    resultado_api = criar_e_otimizar(user_id)

    # 7. Calcular fronteira eficiente
    ef = calcular_fronteira_eficiente(historicos)

    # 8. Plotar
    if ef:
        plotar(ef, resultado_api)

    header("[DONE] Pipeline concluido!")
    print("  O grafico 'fronteira_eficiente.png' foi gerado na pasta do projeto.")
    print()


if __name__ == "__main__":
    main()
