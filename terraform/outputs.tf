# =============================================================================
# outputs.tf — Valores exibidos após "terraform apply"
# =============================================================================

output "vm_public_ip" {
  description = "IP público da VM."
  value       = azurerm_public_ip.main.ip_address
}

output "ssh_command" {
  description = "Comando para acessar a VM via SSH."
  value       = "ssh ${var.admin_username}@${azurerm_public_ip.main.ip_address}"
}

output "ssh_private_key_path" {
  description = "Caminho da chave privada GERADA pelo Terraform (só existe se você não tinha uma chave local em var.ssh_public_key_path). Use com: ssh -i terraform/generated_key.pem ..."
  value       = local.ssh_key_exists ? "Usando chave local existente: ${var.ssh_public_key_path}" : "${path.module}/generated_key.pem"
}

output "user_service_url" {
  description = "URL base do user-service."
  value       = "http://${azurerm_public_ip.main.ip_address}:8081"
}

output "asset_service_url" {
  description = "URL base do asset-service."
  value       = "http://${azurerm_public_ip.main.ip_address}:8082"
}

output "portfolio_service_url" {
  description = "URL base do portfolio-service."
  value       = "http://${azurerm_public_ip.main.ip_address}:8083"
}

output "notification_service_url" {
  description = "URL base do notification-service."
  value       = "http://${azurerm_public_ip.main.ip_address}:8084"
}

output "rabbitmq_management_url" {
  description = "URL do painel de administração do RabbitMQ."
  value       = "http://${azurerm_public_ip.main.ip_address}:15672"
}

output "bootstrap_log_hint" {
  description = "Como acompanhar o provisionamento automático da aplicação."
  value       = "Após o SSH, rode: sudo tail -f /var/log/markovitz-bootstrap.log"
}
