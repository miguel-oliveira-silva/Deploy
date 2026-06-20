# =============================================================================
# ssh-key.tf — Chave SSH para acesso administrativo à VM
# =============================================================================
# Tentamos ler uma chave pública já existente na máquina de quem roda o
# Terraform (var.ssh_public_key_path). Se não existir, geramos um par de
# chaves automaticamente com o provider "tls" e salvamos a CHAVE PRIVADA
# localmente em terraform/generated_key.pem (NUNCA commitar esse arquivo —
# veja .gitignore).
# =============================================================================

# Gera um par de chaves novo (só será efetivamente usado se a chave local
# informada em var.ssh_public_key_path não existir).
resource "tls_private_key" "generated" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

locals {
  ssh_key_exists = fileexists(pathexpand(var.ssh_public_key_path))

  # Se a chave local existir, usa ela. Senão, usa a chave pública gerada.
  ssh_public_key = local.ssh_key_exists ? file(pathexpand(var.ssh_public_key_path)) : tls_private_key.generated.public_key_openssh
}

# Salva a chave privada gerada em disco SOMENTE se ela foi de fato necessária
# (ou seja, nenhuma chave local foi encontrada). Permissão 0600 = só o dono lê.
resource "local_sensitive_file" "private_key" {
  count           = local.ssh_key_exists ? 0 : 1
  content         = tls_private_key.generated.private_key_pem
  filename        = "${path.module}/generated_key.pem"
  file_permission = "0600"
}
