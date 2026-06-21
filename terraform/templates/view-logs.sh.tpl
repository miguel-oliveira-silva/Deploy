#!/bin/bash
# =============================================================================
# view-logs.sh — Script de Visualização de Logs de Containers
# =============================================================================
# Gerado automaticamente pelo Terraform para facilitar visualização de logs
# dos containers Docker na VM Azure.
#
# Uso:
#   ./view-logs.sh <service-name> [lines]
#
# Argumentos:
#   service-name: Nome do serviço (obrigatório)
#                 Valores válidos: user-service, asset-service, 
#                                 portfolio-service, notification-service,
#                                 postgres, rabbitmq
#   lines:       Número de linhas a exibir (opcional, padrão: 50)
#                Use valores maiores quando benéfico para diagnóstico
#
# Exemplos:
#   ./view-logs.sh user-service          # Últimas 50 linhas
#   ./view-logs.sh postgres 100          # Últimas 100 linhas
#   ./view-logs.sh notification-service 200  # Últimas 200 linhas
#
# Exit codes:
#   0 = logs exibidos com sucesso
#   1 = erro (nome de serviço inválido, falha de conexão SSH, etc.)
# =============================================================================

set -e  # Falhar em caso de erro não tratado

# =============================================================================
# Configuração
# =============================================================================
VM_IP="${vm_ip}"
VM_USER="${vm_user}"
CONTAINER_PREFIX="markovitz"
DEFAULT_LINES=50

# Serviços válidos
VALID_SERVICES=("user-service" "asset-service" "portfolio-service" "notification-service" "postgres" "rabbitmq")

# =============================================================================
# Cores para Output
# =============================================================================
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Funções de output colorido
error() {
    echo -e "$${RED}[✗]$${NC} $1" >&2
}

info() {
    echo -e "$${BLUE}[ℹ]$${NC} $1"
}

# =============================================================================
# Validação de Argumentos
# =============================================================================

# Verificar se service-name foi fornecido
if [ $# -lt 1 ]; then
    error "Nome do serviço é obrigatório"
    echo ""
    echo "Uso: $0 <service-name> [lines]"
    echo ""
    echo "Serviços válidos:"
    for service in "$${VALID_SERVICES[@]}"; do
        echo "  - $service"
    done
    echo ""
    echo "Exemplos:"
    echo "  $0 user-service          # Últimas 50 linhas (padrão)"
    echo "  $0 postgres 100          # Últimas 100 linhas"
    echo "  $0 rabbitmq 200          # Últimas 200 linhas"
    echo ""
    exit 1
fi

SERVICE_NAME="$1"
LINES="${2:-$DEFAULT_LINES}"

# Validar que service-name é válido
VALID=false
for valid_service in "$${VALID_SERVICES[@]}"; do
    if [ "$SERVICE_NAME" = "$valid_service" ]; then
        VALID=true
        break
    fi
done

if [ "$VALID" = false ]; then
    error "Nome de serviço inválido: '$SERVICE_NAME'"
    echo ""
    echo "Serviços válidos:"
    for service in "$${VALID_SERVICES[@]}"; do
        echo "  - $service"
    done
    echo ""
    exit 1
fi

# Validar que lines é um número positivo
if ! [[ "$LINES" =~ ^[0-9]+$ ]] || [ "$LINES" -le 0 ]; then
    error "Número de linhas deve ser um inteiro positivo: '$LINES'"
    echo ""
    echo "Uso: $0 <service-name> [lines]"
    echo "Exemplo: $0 user-service 100"
    echo ""
    exit 1
fi

# =============================================================================
# Nome do Container
# =============================================================================
# Formato do nome do container: markovitz-<service-name>
CONTAINER_NAME="$${CONTAINER_PREFIX}-$${SERVICE_NAME}"

# =============================================================================
# Executar docker logs via SSH
# =============================================================================
info "Conectando a $${VM_USER}@$${VM_IP}..."
info "Exibindo últimas $${LINES} linhas de logs do container: $${CONTAINER_NAME}"
echo ""
echo "========================================"
echo ""

# Executar docker logs via SSH
# - Conectar à VM
# - Executar docker logs com --tail para limitar linhas
# - Capturar exit code para detectar erros
if ssh -o ConnectTimeout=10 -o StrictHostKeyChecking=no "$${VM_USER}@$${VM_IP}" \
    "docker logs $${CONTAINER_NAME} --tail $${LINES}" 2>&1; then
    
    # Registrar operação no bootstrap log
    ssh -o ConnectTimeout=10 -o StrictHostKeyChecking=no "$${VM_USER}@$${VM_IP}" \
        "echo \"[\$(date -u +\"%Y-%m-%dT%H:%M:%S+00:00\")] [INFO] [MAINTENANCE] Viewed logs: $${SERVICE_NAME} (last $${LINES} lines)\" >> /var/log/markovitz-bootstrap.log" 2>/dev/null || true
    
    echo ""
    echo "========================================"
    info "Logs exibidos com sucesso"
    exit 0
else
    SSH_EXIT_CODE=$?
    echo ""
    echo "========================================"
    error "Falha ao obter logs do container $${CONTAINER_NAME}"
    echo ""
    echo "Possíveis causas:"
    echo "  1. Container não existe ou não está rodando"
    echo "  2. Falha de conexão SSH com a VM"
    echo "  3. Serviço ainda não foi inicializado"
    echo ""
    echo "Comandos de diagnóstico:"
    echo "  # Verificar status de todos os containers"
    echo "  ssh $${VM_USER}@$${VM_IP} 'docker ps -a'"
    echo ""
    echo "  # Verificar se o container existe"
    echo "  ssh $${VM_USER}@$${VM_IP} 'docker ps -a | grep $${CONTAINER_NAME}'"
    echo ""
    echo "  # Verificar logs do bootstrap"
    echo "  ssh $${VM_USER}@$${VM_IP} 'tail -100 /var/log/markovitz-bootstrap.log'"
    echo ""
    exit 1
fi
