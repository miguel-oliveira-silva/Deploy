# =============================================================================
# providers.tf — Configuração do Terraform e do Provider Azure (azurerm)
# =============================================================================

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.110"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
    local = {
      source  = "hashicorp/local"
      version = "~> 2.4"
    }
  }

  # ---------------------------------------------------------------------------
  # BACKEND REMOTO (opcional, recomendado para trabalho em equipe/CI-CD)
  # ---------------------------------------------------------------------------
  # Por padrão, o Terraform guarda o "state" (terraform.tfstate) localmente.
  # Isso funciona para um único integrante rodando da própria máquina, mas
  # se o grupo for usar Terraform em mais de uma máquina ou em pipeline CI/CD,
  # o ideal é guardar o state em um Azure Storage Account remoto, para que
  # todo mundo enxergue o mesmo estado da infraestrutura.
  #
  # Para habilitar, crie um Storage Account + container manualmente (ou em
  # um Terraform "bootstrap" separado) e descomente o bloco abaixo:
  #
  # backend "azurerm" {
  #   resource_group_name  = "rg-markovitz-tfstate"
  #   storage_account_name = "stmarkovitztfstate"
  #   container_name       = "tfstate"
  #   key                  = "markovitz.tfstate"
  # }
}

provider "azurerm" {
  features {
    resource_group {
      # Permite que o "terraform destroy" apague o Resource Group mesmo que
      # ele contenha recursos não criados pelo Terraform (útil em ambiente
      # de estudo/projeto acadêmico). Em produção real, normalmente deixamos
      # como false por segurança.
      prevent_deletion_if_contains_resources = false
    }
  }
}
