# =============================================================================
# main.tf — Resource Group (contêiner lógico de todos os recursos)
# =============================================================================
# No Azure, TUDO vive dentro de um Resource Group: VM, rede, disco, IP
# público, etc. Apagar o Resource Group apaga tudo dentro dele de uma vez —
# é a forma mais simples de "desligar tudo" no fim do projeto.
# =============================================================================

resource "azurerm_resource_group" "main" {
  name     = "rg-${var.project_name}-${var.environment}"
  location = var.location
  tags     = var.tags
}
