# =============================================================================
# vm.tf — Máquina Virtual Linux (Ubuntu) com provisionamento via cloud-init
# =============================================================================

# -----------------------------------------------------------------------------
# Renderiza o template bootstrap.sh.tpl com as variáveis e feature flags
# -----------------------------------------------------------------------------
locals {
  bootstrap_script_rendered = templatefile("${path.module}/templates/bootstrap.sh.tpl", {
    admin_username             = var.admin_username
    git_repo_url               = var.git_repo_url
    git_repo_branch            = var.git_repo_branch
    db_user                    = var.db_user
    db_password                = var.db_password
    rabbitmq_user              = var.rabbitmq_user
    rabbitmq_password          = var.rabbitmq_password
    enable_retry_logic         = var.feature_flags.retry_logic
    enable_structured_logs     = var.feature_flags.structured_logs
    enable_sequential_build    = var.feature_flags.sequential_build
    enable_health_checks       = var.feature_flags.health_checks
    enable_resource_monitoring = var.feature_flags.resource_monitoring
  })
}

# -----------------------------------------------------------------------------
# Monta o cloud-init.yaml final substituindo o placeholder do bootstrap script.
# Usa file() + replace() em vez de templatefile() para evitar que o template
# engine re-escape os caracteres $ do script bash renderizado (o que causava
# $$ no lugar de $ e quebrava a execução do bootstrap na VM).
# -----------------------------------------------------------------------------
locals {
  cloud_init_rendered = replace(
    file("${path.module}/cloud-init.yaml"),
    "$${bootstrap_script}",
    indent(6, local.bootstrap_script_rendered)
  )
}

# -----------------------------------------------------------------------------
# DISCO DO SISTEMA OPERACIONAL
# -----------------------------------------------------------------------------
# Definido dentro do bloco os_disk da VM (ver abaixo). Usamos Standard SSD
# (bom custo-benefício) com 64GB — suficiente para SO + imagens Docker
# (4 microsserviços + Postgres + RabbitMQ + camadas de build do Maven).
# -----------------------------------------------------------------------------

resource "azurerm_linux_virtual_machine" "main" {
  name                = "vm-${var.project_name}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  size                = var.vm_size
  admin_username      = var.admin_username
  tags                = var.tags
  # zone não especificada — Azure escolhe a zona com capacidade disponível

  network_interface_ids = [
    azurerm_network_interface.main.id,
  ]

  admin_ssh_key {
    username   = var.admin_username
    public_key = local.ssh_public_key
  }

  # Desabilita login por senha — só SSH com chave é permitido (boa prática)
  disable_password_authentication = true

  os_disk {
    name                 = "osdisk-${var.project_name}"
    caching              = "ReadWrite"
    storage_account_type = "StandardSSD_LRS"
    disk_size_gb         = 64
  }

  source_image_reference {
    publisher = "Canonical"
    offer     = "0001-com-ubuntu-server-jammy"
    sku       = "22_04-lts-gen2"
    version   = "latest"
  }

  # custom_data → é exatamente o mecanismo que o Azure usa para entregar
  # o cloud-init para a VM. Precisa estar em base64 (o Terraform faz isso
  # automaticamente com base64encode).
  custom_data = base64encode(local.cloud_init_rendered)

  boot_diagnostics {
    storage_account_uri = null # usa o storage gerenciado pela própria Azure
  }
}
