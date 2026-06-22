#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
populate_all.py

Script master que popula o banco com TODAS as ações:
- 37 ações brasileiras
- 60 ações americanas
= 97 ações totais (últimos 6 meses)

Uso:
    python populate_all.py
"""

import sys
import subprocess
import time
from datetime import datetime

def header(msg: str):
    print(f"\n{'='*70}")
    print(f"  {msg}")
    print(f"{'='*70}")

def ok(msg):   print(f"  ✅ {msg}")
def err(msg):  print(f"  ❌ {msg}")
def info(msg): print(f"  ℹ️  {msg}")

def executar_script(nome: str, descricao: str) -> bool:
    """Executa um script Python e retorna True se sucesso"""
    header(f"Executando: {descricao}")
    print(f"\n  🚀 Iniciando {nome}...\n")
    
    try:
        inicio = time.time()
        resultado = subprocess.run(
            [sys.executable, nome],
            check=True,
            text=True,
            capture_output=False
        )
        tempo = time.time() - inicio
        
        ok(f"{descricao} concluído em {tempo/60:.1f} minutos")
        return True
        
    except subprocess.CalledProcessError as e:
        err(f"Erro ao executar {nome}: {e}")
        return False
    except FileNotFoundError:
        err(f"Arquivo {nome} não encontrado")
        return False
    except Exception as e:
        err(f"Erro inesperado: {e}")
        return False

def main():
    print("\n" + "=" * 70)
    print("  MARKOVITZ — POPULAÇÃO COMPLETA (BRASIL + EUA)")
    print("=" * 70)
    print(f"  🌎 População global de ações")
    print(f"  📅 Data: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 70)
    
    info("Este script irá popular o banco com:")
    print("     • 37 ações brasileiras (Ibovespa)")
    print("     • 60 ações americanas (S&P 500, Dow Jones, NASDAQ)")
    print("     • Total: ~97 ações")
    print("     • Período: Últimos 6 meses")
    print()
    
    resposta = input("  Deseja continuar? (s/n): ")
    if resposta.lower() != 's':
        print("\n  ❌ Operação cancelada pelo usuário.")
        sys.exit(0)
    
    inicio_total = time.time()
    sucesso_br = False
    sucesso_us = False
    
    # Passo 1: Ações Brasileiras
    sucesso_br = executar_script(
        "populate_acoes_brasileiras.py",
        "População de Ações Brasileiras"
    )
    
    if not sucesso_br:
        print("\n  ⚠️  Erro na população das ações brasileiras.")
        resposta = input("  Deseja continuar com as ações americanas? (s/n): ")
        if resposta.lower() != 's':
            sys.exit(1)
    
    print("\n  ⏸️  Aguardando 5 segundos antes de continuar...")
    time.sleep(5)
    
    # Passo 2: Ações Americanas
    sucesso_us = executar_script(
        "populate_acoes_usa.py",
        "População de Ações Americanas"
    )
    
    tempo_total = time.time() - inicio_total
    
    # Resumo Final
    header("📊 RESUMO DA POPULAÇÃO")
    
    print()
    if sucesso_br:
        ok("Ações Brasileiras: ✅ Sucesso")
    else:
        err("Ações Brasileiras: ❌ Falha")
    
    if sucesso_us:
        ok("Ações Americanas: ✅ Sucesso")
    else:
        err("Ações Americanas: ❌ Falha")
    
    print(f"\n  ⏱️  Tempo total: {tempo_total/60:.1f} minutos")
    
    if sucesso_br and sucesso_us:
        header("🎉 POPULAÇÃO COMPLETA — SUCESSO TOTAL!")
        print()
        ok("Banco de dados populado com ~97 ações (Brasil + EUA)")
        ok("Dados dos últimos 6 meses disponíveis")
        ok("Sistema pronto para otimização de carteiras globais!")
        print()
        info("Próximos passos:")
        print("     1. Abra o app Godot (F5)")
        print("     2. Explore as ações brasileiras e americanas")
        print("     3. Crie carteiras mistas para diversificação internacional")
        print("     4. Compare resultados: BR vs US vs Global")
        print()
    elif sucesso_br or sucesso_us:
        header("⚠️  POPULAÇÃO PARCIAL")
        print()
        info("Apenas um dos mercados foi populado com sucesso.")
        info("Você pode tentar executar o script que falhou manualmente.")
        print()
    else:
        header("❌ FALHA NA POPULAÇÃO")
        print()
        err("Nenhum dos scripts foi executado com sucesso.")
        err("Verifique:")
        print("     • Conexão com internet")
        print("     • VM Azure está rodando")
        print("     • Dependências instaladas (pip install -r requirements.txt)")
        print()
        sys.exit(1)

if __name__ == "__main__":
    main()
