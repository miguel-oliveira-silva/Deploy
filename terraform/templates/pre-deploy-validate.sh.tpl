#!/bin/bash
# =============================================================================
# pre-deploy-validate.sh — Validações Pré-Deploy para Terraform
# =============================================================================
# Gerado automaticamente pelo Terraform para validar pré-condições antes do
# terraform apply, evitando falhas e desperdício de créditos Azure.
#
# Uso:
#   ./pre-deploy-validate.sh
#
# Exit codes:
#   0 = todas validações passaram
#   1 = alguma validação falhou
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

warning() {
    echo -e "$${YELLOW}[!]$${NC} $1"
}

# =============================================================================
# Validação 1: Verificar existência de terraform.tfvars
# =============================================================================
echo ""
echo "================================================"
echo "  Validação 1: terraform.tfvars"
echo "================================================"

if [ ! -f "terraform.tfvars" ]; then
    error "terraform.tfvars not found"
    echo ""
    echo "Ação corretiva:"
    echo "  1. Crie o arquivo terraform.tfvars a partir do exemplo:"
    echo "     cp terraform.tfvars.example terraform.tfvars"
    echo "  2. Edite o arquivo e preencha as variáveis obrigatórias"
    echo ""
    exit 1
fi

success "terraform.tfvars encontrado"

# =============================================================================
# Validação 2: Verificar variáveis obrigatórias não estão vazias
# =============================================================================
echo ""
echo "================================================"
echo "  Validação 2: Variáveis Obrigatórias"
echo "================================================"

REQUIRED_VARS=("db_password" "rabbitmq_password" "git_repo_url")
MISSING_VARS=()

for var in "$${REQUIRED_VARS[@]}"; do
    # Extrair valor da variável do terraform.tfvars
    # Suporta formatos: var = "value" ou var="value"
    value=$(grep -E "^[[:space:]]*$${var}[[:space:]]*=" terraform.tfvars | sed -E 's/^[[:space:]]*[^=]*=[[:space:]]*"?([^"]*)"?[[:space:]]*(#.*)?$/\1/' | tr -d '"' | tr -d "'")
    
    if [ -z "$value" ]; then
        MISSING_VARS+=("$var")
        error "Variável '$var' está vazia ou não definida"
    else
        success "Variável '$var' definida"
    fi
done

if [ $${#MISSING_VARS[@]} -gt 0 ]; then
    echo ""
    echo "Ação corretiva:"
    echo "  Edite terraform.tfvars e defina as seguintes variáveis:"
    for var in "$${MISSING_VARS[@]}"; do
        echo "    - $var"
    done
    echo ""
    echo "  Exemplo:"
    echo "    db_password = \"sua-senha-segura\""
    echo "    rabbitmq_password = \"outra-senha-segura\""
    echo "    git_repo_url = \"https://github.com/usuario/repo.git\""
    echo ""
    exit 1
fi

# =============================================================================
# Validação 3: Verificar chave SSH existe ou pode ser gerada
# =============================================================================
echo ""
echo "================================================"
echo "  Validação 3: Chave SSH"
echo "================================================"

# Extrair ssh_public_key_path do terraform.tfvars (se definido) ou usar default
SSH_KEY_PATH=$(grep -E "^[[:space:]]*ssh_public_key_path[[:space:]]*=" terraform.tfvars | sed -E 's/^[[:space:]]*[^=]*=[[:space:]]*"?([^"]*)"?[[:space:]]*(#.*)?$/\1/' | tr -d '"' | tr -d "'")

# Se não encontrado, usar default do variables.tf
if [ -z "$SSH_KEY_PATH" ]; then
    SSH_KEY_PATH="${ssh_public_key_path}"
fi

# Expandir ~ para home directory
SSH_KEY_PATH="$${SSH_KEY_PATH/#\~/$HOME}"

if [ -f "$SSH_KEY_PATH" ]; then
    success "Chave SSH encontrada em $SSH_KEY_PATH"
else
    warning "Chave SSH não encontrada em $SSH_KEY_PATH"
    echo "  Tentando gerar novo par de chaves..."
    
    # Extrair diretório e nome base
    SSH_KEY_DIR=$(dirname "$SSH_KEY_PATH")
    SSH_PRIVATE_KEY="$${SSH_KEY_PATH%.pub}"
    
    # Criar diretório se não existir
    mkdir -p "$SSH_KEY_DIR"
    
    # Gerar par de chaves
    if ssh-keygen -t rsa -b 4096 -f "$SSH_PRIVATE_KEY" -N "" -C "markovitz-azure-key" >/dev/null 2>&1; then
        success "Novo par de chaves SSH gerado em $SSH_PRIVATE_KEY"
        success "Chave pública em $SSH_KEY_PATH"
    else
        error "Falha ao gerar chave SSH"
        echo ""
        echo "Ação corretiva:"
        echo "  1. Verifique permissões do diretório $SSH_KEY_DIR"
        echo "  2. Gere manualmente com:"
        echo "     ssh-keygen -t rsa -b 4096 -f $SSH_PRIVATE_KEY"
        echo "  3. Execute este script novamente"
        echo ""
        exit 1
    fi
fi

# =============================================================================
# Validação 4: Testar conectividade com git_repo_url
# =============================================================================
echo ""
echo "================================================"
echo "  Validação 4: Conectividade Git Repository"
echo "================================================"

# Extrair git_repo_url do terraform.tfvars
GIT_REPO_URL=$(grep -E "^[[:space:]]*git_repo_url[[:space:]]*=" terraform.tfvars | sed -E 's/^[[:space:]]*[^=]*=[[:space:]]*"?([^"]*)"?[[:space:]]*(#.*)?$/\1/' | tr -d '"' | tr -d "'")

# Se não encontrado, usar default do variables.tf
if [ -z "$GIT_REPO_URL" ]; then
    GIT_REPO_URL="${git_repo_url}"
fi

echo "  Testando conectividade com: $GIT_REPO_URL"

# Fazer HTTP HEAD request para verificar se o repositório existe
# Timeout de 10 segundos
if curl -sSf --head --max-time 10 "$GIT_REPO_URL" >/dev/null 2>&1; then
    success "Repositório Git acessível"
elif curl -sSf --head --max-time 10 --location "$GIT_REPO_URL" >/dev/null 2>&1; then
    # Tentar novamente com -L para seguir redirects (repositórios podem redirecionar)
    success "Repositório Git acessível (com redirect)"
else
    error "Repositório Git não acessível: $GIT_REPO_URL"
    echo ""
    echo "Ação corretiva:"
    echo "  1. Verifique se a URL está correta"
    echo "  2. Verifique sua conexão de rede"
    echo "  3. Se o repositório é privado, certifique-se que é acessível via HTTPS sem autenticação"
    echo "     ou configure autenticação no cloud-init script"
    echo "  4. Teste manualmente com:"
    echo "     curl -I $GIT_REPO_URL"
    echo ""
    exit 1
fi

# =============================================================================
# Validação Completa
# =============================================================================
echo ""
echo "================================================"
echo -e "$${GREEN}  ✓ Pré-condições validadas com sucesso$${NC}"
echo "================================================"
echo ""
echo "Próximos passos:"
echo "  1. Execute: terraform plan"
echo "  2. Revise as mudanças planejadas"
echo "  3. Execute: terraform apply"
echo ""
echo "Tempo estimado do deploy: 15-20 minutos"
echo ""

exit 0
