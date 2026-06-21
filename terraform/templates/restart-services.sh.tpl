#!/bin/bash
# =============================================================================
# restart-services.sh — Restart All Services Without Rebuild
# =============================================================================
# Gerado automaticamente pelo Terraform para facilitar o restart de todos os
# containers Docker sem reconstruir as imagens.
#
# Uso:
#   ./restart-services.sh
#
# Exit codes:
#   0 = restart executado com sucesso
#   1 = falha ao executar restart
# =============================================================================

set -e  # Falhar em caso de erro não tratado

# =============================================================================
# Cores para Output
# =============================================================================
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Funções de output colorido
success() {
    echo -e "$${GREEN}[✓]$${NC} $1"
}

error() {
    echo -e "$${RED}[✗]$${NC} $1"
}

info() {
    echo -e "$${YELLOW}[i]$${NC} $1"
}

# =============================================================================
# Variáveis de Configuração (injetadas pelo Terraform)
# =============================================================================
VM_IP="${vm_ip}"
ADMIN_USERNAME="${admin_username}"
APP_DIR="/opt/markovitz/app"
LOG_FILE="/var/log/markovitz-bootstrap.log"

# =============================================================================
# Função: Executar Comando Remoto via SSH
# =============================================================================
execute_remote() {
    local command="$1"
    local description="$2"
    
    info "$description"
    
    if ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 "$ADMIN_USERNAME@$VM_IP" "$command"; then
        return 0
    else
        return 1
    fi
}

# =============================================================================
# Função: Registrar Operação no Bootstrap Log Remoto
# =============================================================================
log_operation() {
    local status="$1"
    local message="$2"
    local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%S+00:00")
    local log_entry="[$timestamp] [INFO] [SYSTEM] Maintenance Script - restart-services.sh: $message"
    
    # Registrar no log remoto (não falha se log falhar)
    ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 "$ADMIN_USERNAME@$VM_IP" \
        "echo '$log_entry' | sudo tee -a $LOG_FILE >/dev/null 2>&1" || true
}

# =============================================================================
# Main: Restart Services
# =============================================================================
echo ""
echo "================================================"
echo "  Restart All Services (Without Rebuild)"
echo "================================================"
echo ""
echo "  VM IP: $VM_IP"
echo "  Usuário: $ADMIN_USERNAME"
echo ""

# Verificar conectividade SSH
info "Verificando conectividade SSH..."
if ! ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 "$ADMIN_USERNAME@$VM_IP" "echo 'SSH OK'" >/dev/null 2>&1; then
    error "Falha ao conectar via SSH em $ADMIN_USERNAME@$VM_IP"
    echo ""
    echo "Ação corretiva:"
    echo "  1. Verifique se a VM está em execução no Azure Portal"
    echo "  2. Verifique se o IP público está correto"
    echo "  3. Verifique sua conexão de rede"
    echo "  4. Teste manualmente com:"
    echo "     ssh $ADMIN_USERNAME@$VM_IP"
    echo ""
    exit 1
fi
success "Conectividade SSH OK"

# Executar restart via docker compose
echo ""
info "Executando 'docker compose restart' em $APP_DIR..."

RESTART_COMMAND="cd $APP_DIR && docker compose restart"

if execute_remote "$RESTART_COMMAND" "Reiniciando todos os containers..."; then
    success "Restart executado com sucesso!"
    log_operation "SUCCESS" "All services restarted successfully"
    
    echo ""
    echo "================================================"
    echo -e "$${GREEN}  ✓ Serviços reiniciados com sucesso$${NC}"
    echo "================================================"
    echo ""
    echo "Próximos passos:"
    echo "  1. Aguarde alguns segundos para os serviços inicializarem"
    echo "  2. Verifique health dos serviços:"
    echo "     curl http://$VM_IP:8081/actuator/health"
    echo "     curl http://$VM_IP:8082/actuator/health"
    echo "     curl http://$VM_IP:8083/actuator/health"
    echo "     curl http://$VM_IP:8084/actuator/health"
    echo "  3. Verifique logs se necessário:"
    echo "     ./view-logs.sh [service-name]"
    echo ""
    
    exit 0
else
    error "Falha ao executar restart"
    log_operation "FAILED" "Failed to restart services"
    
    echo ""
    echo "Ação corretiva:"
    echo "  1. Verifique se o diretório $APP_DIR existe na VM:"
    echo "     ssh $ADMIN_USERNAME@$VM_IP 'ls -la $APP_DIR'"
    echo "  2. Verifique se o docker compose está instalado:"
    echo "     ssh $ADMIN_USERNAME@$VM_IP 'docker compose version'"
    echo "  3. Verifique o status dos containers:"
    echo "     ssh $ADMIN_USERNAME@$VM_IP 'docker ps -a'"
    echo "  4. Consulte o bootstrap log:"
    echo "     ssh $ADMIN_USERNAME@$VM_IP 'sudo tail -100 $LOG_FILE'"
    echo ""
    
    exit 1
fi
