# =============================================================================
# variables.tf — Variáveis de entrada do projeto Terraform
# =============================================================================
# Cada variável pode ser sobrescrita via:
#   - arquivo terraform.tfvars
#   - flag -var="nome=valor" na linha de comando
#   - variável de ambiente TF_VAR_nome
# =============================================================================

variable "location" {
  description = "Região do Azure onde os recursos serão criados (ex: 'Brazil South', 'East US')."
  type        = string
  default     = "Brazil South"
}

variable "project_name" {
  description = "Prefixo usado para nomear todos os recursos (sem espaços, minúsculo)."
  type        = string
  default     = "markovitz"
}

variable "environment" {
  description = "Nome do ambiente (ex: dev, prod). Usado em tags e nomes de recursos."
  type        = string
  default     = "prod"
}

variable "vm_size" {
  description = "Tamanho da VM no Azure. Standard_B2s tem 2 vCPUs e 4GB RAM — suficiente para rodar 4 microsserviços Spring Boot + Postgres + RabbitMQ em modo didático."
  type        = string
  default     = "Standard_B2s"
}

variable "admin_username" {
  description = "Usuário administrador da VM Linux."
  type        = string
  default     = "azureuser"
}

variable "ssh_public_key_path" {
  description = "Caminho local para a chave pública SSH a ser injetada na VM. Se o arquivo não existir, o Terraform gera um par de chaves automaticamente (veja ssh-key.tf)."
  type        = string
  default     = "~/.ssh/id_rsa.pub"
}

variable "git_repo_url" {
  description = "URL do repositório Git contendo o projeto (com docker-compose.yml na raiz)."
  type        = string
  default     = "https://github.com/miguel-oliveira-silva/Deploy.git"
}

variable "git_repo_branch" {
  description = "Branch do repositório a ser clonada."
  type        = string
  default     = "main"
}

variable "db_user" {
  description = "Usuário do banco PostgreSQL usado pelos microsserviços."
  type        = string
  default     = "markovitz"
}

variable "db_password" {
  description = "Senha do banco PostgreSQL. Defina via terraform.tfvars ou TF_VAR_db_password — NÃO deixe um valor default em produção real."
  type        = string
  sensitive   = true
}

variable "rabbitmq_user" {
  description = "Usuário do RabbitMQ."
  type        = string
  default     = "guest"
}

variable "rabbitmq_password" {
  description = "Senha do RabbitMQ. Defina via terraform.tfvars ou TF_VAR_rabbitmq_password."
  type        = string
  sensitive   = true
}

variable "allowed_ssh_cidr" {
  description = "Faixa de IPs autorizada a acessar a VM via SSH (porta 22). Use seu IP público /32 em produção; '0.0.0.0/0' libera para qualquer IP (apenas para fins didáticos)."
  type        = string
  default     = "0.0.0.0/0"
}

variable "tags" {
  description = "Tags aplicadas a todos os recursos."
  type        = map(string)
  default = {
    projeto    = "markovitz-portfolio-optimizer"
    disciplina = "devops"
    gerenciado_por = "terraform"
  }
}
