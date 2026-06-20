#!/bin/bash
# =============================================================================
# init-multi-db.sh
# =============================================================================
# A imagem oficial do Postgres já cria automaticamente o banco definido em
# POSTGRES_DB (userdb). Este script roda na PRIMEIRA inicialização do
# container (Postgres executa tudo em /docker-entrypoint-initdb.d/) e cria
# os outros 3 bancos que faltam: assetdb, portfoliodb, notificationdb.
#
# Isso replica o princípio "database per service" que o projeto já usava
# com H2 (cada microsserviço tinha seu próprio "banco" em memória), agora
# com bancos reais e persistentes dentro da mesma instância PostgreSQL.
# =============================================================================
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE assetdb;
    CREATE DATABASE portfoliodb;
    CREATE DATABASE notificationdb;
EOSQL

echo "✅ Bancos assetdb, portfoliodb e notificationdb criados com sucesso."
