# Implementation Plan: Azure Infrastructure Improvements

## Overview

This implementation plan covers enhancements to the Azure infrastructure for the Markovitz project, focusing on resilience, observability, and operational reliability. The implementation uses Terraform (HCL) for infrastructure provisioning and Bash scripting for bootstrap automation on Ubuntu 22.04 VM.

**Key Technologies:**
- Terraform (HCL) for IaC
- Bash scripting for cloud-init and maintenance
- Azure Cloud (VM, networking, cloud-init)
- Docker & Docker Compose orchestration

## Tasks

- [x] 1. Set up feature flags and Terraform variables
  - Add `feature_flags` variable to variables.tf with all 10 toggles (retry_logic, sequential_build, health_checks, structured_logs, resource_monitoring, auto_rollback, pre_deploy_validation, maintenance_scripts, log_parser, deployment_report)
  - Add `enable_auto_rollback` boolean variable with default false
  - Create template structure for scripts directory (terraform/templates/)
  - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 14.7, 14.8_

- [ ] 2. Implement retry logic functions in bootstrap script
  - [x] 2.1 Create bash retry_command() function with configurable attempts and interval
    - Implement generic retry function accepting command, max_attempts, and interval parameters
    - Add structured logging for each retry attempt with component and level
    - Return exit code 0 on success, 1 on failure after all retries
    - _Requirements: 1.1, 1.4, 4.3_
  
  - [x] 2.2 Create specialized retry functions for Git and Docker operations
    - Implement retry_git_clone() with 3 attempts, 10s interval
    - Implement retry_docker_build() with 2 attempts, 5s interval for specific image
    - Implement retry_apt_get() with 3 attempts, 5s interval, apt-get clean between attempts
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
  
  - [x] 2.3 Integrate retry logic into bootstrap script with feature flag check
    - Add feature flag check before applying retry logic
    - Replace direct git clone with retry_git_clone when enabled
    - Replace direct docker build with retry_docker_build when enabled
    - Replace direct apt-get with retry_apt_get when enabled
    - Log fatal error and abort on final retry failure
    - _Requirements: 1.5, 14.2_

- [ ] 3. Implement structured logging system
  - [x] 3.1 Create log() bash function with timestamp and structured format
    - Implement function accepting level, component, message parameters
    - Use ISO-8601 UTC timestamp format (date -u +"%Y-%m-%dT%H:%M:%S+00:00")
    - Format output as "[TIMESTAMP] [LEVEL] [COMPONENT] message"
    - Write to both stdout and /var/log/markovitz-bootstrap.log using tee
    - _Requirements: 4.1, 4.2, 4.3, 4.4_
  
  - [x] 3.2 Add visual separators and duration tracking to bootstrap
    - Add separator logging at bootstrap start (gracefully handle failures)
    - Record start timestamp in variable
    - Add separator logging at bootstrap end (gracefully handle failures)
    - Calculate and log total duration in seconds at end
    - _Requirements: 4.5, 4.6, 4.7, 4.8_

- [x] 4. Checkpoint - Verify retry and logging basics
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Implement sequential build orchestrator
  - [x] 5.1 Create build_infrastructure_sequential() function
    - Build postgres image first with structured logging
    - Build rabbitmq image second with structured logging
    - Record build time and memory usage for each image
    - _Requirements: 2.2, 2.6_
  
  - [x] 5.2 Create build_services_in_pairs() function
    - Implement memory check function (returns true if > 3.5GB)
    - Build user-service and asset-service in parallel (background processes with &)
    - Wait for pair 1 completion, check memory, wait 30s if needed
    - Build portfolio-service and notification-service in parallel
    - Wait for pair 2 completion
    - _Requirements: 2.1, 2.3, 2.4, 2.5, 2.6_
  
  - [x] 5.3 Integrate sequential build into bootstrap with feature flag
    - Add feature flag check for sequential_build
    - Replace docker compose build with sequential orchestrator when enabled
    - Keep original parallel build as fallback when disabled
    - Log which build strategy is being used
    - _Requirements: 14.3_

- [x] 6. Implement health check loop
  - [x] 6.1 Create wait_for_containers_running() helper function
    - Check all 6 containers are in "running" state with docker ps
    - Wait up to 60s with 5s polling interval
    - Log when all containers are running
    - _Requirements: 3.1_
  
  - [x] 6.2 Create health_check_loop() function
    - Poll each service health endpoint (8081-8084) every 10s
    - Parse JSON response for "status":"UP" using grep or jq
    - Mark service healthy in log with timestamp
    - Track healthy services count (target: 4)
    - Timeout after 120s with error logging
    - _Requirements: 3.1, 3.2, 3.3, 3.5_
  
  - [x] 6.3 Integrate health checks into bootstrap with success criteria
    - Add feature flag check for health_checks
    - Call health_check_loop after docker compose up
    - Log "Bootstrap concluído com sucesso" when all 4 services healthy
    - Log specific failed services on timeout
    - _Requirements: 3.4, 3.5, 14.4_

- [ ] 7. Checkpoint - Verify build and health check logic
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Implement resource monitoring background process
  - [x] 8.1 Create monitor_resources() background function
    - Start background loop with & operator
    - Log CPU usage every 30s using top -bn1
    - Log memory usage every 30s using free -m
    - Log disk usage every 60s using df -h /
    - Track peak CPU and memory values
    - _Requirements: 9.1, 9.2, 9.3_
  
  - [x] 8.2 Add threshold warnings and termination logic
    - Emit WARNING log when memory >= 90%
    - Emit WARNING log when disk >= 80%
    - Check for /tmp/bootstrap-complete sentinel file
    - Calculate and log peak metrics before terminating
    - _Requirements: 9.4, 9.5, 9.6_
  
  - [x] 8.3 Integrate resource monitor into bootstrap with feature flag
    - Add feature flag check for resource_monitoring
    - Spawn monitor_resources in background at bootstrap start
    - Create sentinel file at bootstrap end
    - Gracefully handle monitor failure (log warning, continue)
    - _Requirements: 14.5_

- [ ] 9. Create pre-deploy validation script
  - [x] 9.1 Generate pre-deploy-validate.sh template
    - Create Terraform templatefile for validation script
    - Add validation for terraform.tfvars existence
    - Add validation for non-empty required variables (db_password, rabbitmq_password, git_repo_url)
    - Add validation for SSH key existence or generation capability
    - _Requirements: 7.1, 7.2, 7.3_
  
  - [x] 9.2 Add connectivity test and error reporting
    - Implement HTTP HEAD test for git_repo_url using curl
    - Add colorized output (green for success, red for error)
    - Add specific error messages with corrective actions
    - Return exit code 0 on success, 1 on any failure
    - Display "Pré-condições validadas com sucesso" message on success
    - _Requirements: 7.4, 7.5, 7.6_
  
  - [ ] 9.3 Integrate pre-deploy validator with Terraform outputs
    - Add local_file resource to generate validation script
    - Add output variable with path to validation script
    - Set executable permissions on generated script
    - _Requirements: 14.7_

- [ ] 10. Create post-deploy verification script
  - [-] 10.1 Generate verify-deployment.sh template with network checks
    - Create Terraform templatefile with parameterized VM IP
    - Implement ping test for VM public IP
    - Implement SSH port (22) connectivity test using nc or telnet
    - Implement application port tests (8081-8084, 15672)
    - _Requirements: 5.2, 5.3, 5.4_
  
  - [ ] 10.2 Add health endpoint and UI validation checks
    - Implement HTTP GET for each /actuator/health endpoint
    - Parse JSON response to validate "status":"UP"
    - Implement HTTP GET for each /swagger-ui.html endpoint
    - Implement HTTP GET for RabbitMQ Management UI (:15672)
    - _Requirements: 5.5, 5.6, 5.7_
  
  - [ ] 10.3 Add result reporting and exit code handling
    - Add colorized output (green for passed, red for failed)
    - Add diagnostic suggestions for each failure type
    - Calculate and display summary (X/11 checks passed)
    - Display "Deploy verificado com sucesso" on all pass
    - Handle message display failure with proper exit code
    - Return exit code 0 only when all checks pass and message displays
    - Return exit code 1 on any failure
    - _Requirements: 5.8, 5.9, 5.10, 5.11_
  
  - [ ] 10.4 Integrate verification script with Terraform outputs
    - Add local_file resource to generate script after apply
    - Add output with script path and usage instructions
    - Set executable permissions
    - _Requirements: 5.1_

- [ ] 11. Checkpoint - Verify validation and verification scripts
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 12. Create maintenance scripts suite
  - [-] 12.1 Generate restart-services.sh script
    - Create template with SSH command to restart containers
    - Add logging to bootstrap log with timestamp
    - Add error handling for non-zero exit codes
    - _Requirements: 8.1, 8.6_
  
  - [ ] 12.2 Generate view-logs.sh script with parameters
    - Accept service-name and lines parameters
    - Default to 50 lines (allow more when beneficial)
    - Support all 6 containers (4 services + postgres + rabbitmq)
    - Add usage help and error messages
    - _Requirements: 8.2, 8.5, 8.6_
  
  - [ ] 12.3 Generate update-application.sh script
    - Implement git pull + docker compose rebuild + restart
    - Add structured logging for each step
    - Add error handling and rollback logic
    - _Requirements: 8.3, 8.6_
  
  - [ ] 12.4 Generate check-resources.sh script
    - Display CPU, memory, disk usage via SSH
    - Display container status with docker ps -a
    - Format output in readable sections
    - _Requirements: 8.4, 8.5, 8.6_
  
  - [ ] 12.5 Integrate maintenance scripts with Terraform
    - Add local_file resources for all 4 scripts
    - Set executable permissions (+x)
    - Add output variables listing generated scripts
    - _Requirements: 14.8_

- [ ] 13. Create log parser and pretty printer
  - [ ] 13.1 Generate parse-logs.sh script with filtering
    - Implement SSH download of bootstrap log
    - Parse structured log format with regex
    - Extract timestamp, level, component, message into JSON structure
    - Support --level filter (INFO, WARN, ERROR)
    - Support --component filter (GIT, DOCKER, APT, HEALTHCHECK, SYSTEM, MONITOR)
    - Support --from and --to timestamp filters
    - _Requirements: 13.1, 13.2, 13.4, 13.5, 13.6_
  
  - [ ] 13.2 Add format validation and error reporting
    - Validate log line format matches expected structure
    - Return descriptive error for invalid lines with line number
    - Validate timestamp is ISO-8601 format
    - Accept valid structured objects and convert them back without loss
    - _Requirements: 13.3, 13.8_
  
  - [ ] 13.3 Generate pretty-print-logs.sh script
    - Format parsed JSON back to human-readable format
    - Add --color option for terminal colorization
    - Use green for INFO, yellow for WARN, red for ERROR
    - Verify round-trip property (parse then pretty-print equals original)
    - _Requirements: 13.7, 13.8, 13.9_
  
  - [ ] 13.4 Integrate log tools with Terraform
    - Generate both scripts via local_file resources
    - Set executable permissions
    - Add output variables with usage examples
    - _Requirements: 14.9_

- [ ] 14. Implement rollback mechanism
  - [ ] 14.1 Generate rollback-monitor.sh script
    - Create background script that waits 30 minutes
    - Check bootstrap log for "Bootstrap concluído com sucesso" via SSH
    - Implement 5-minute grace period on failure detection
    - Execute terraform destroy -auto-approve when rollback enabled
    - Log rollback reason and timestamp to local rollback.log
    - _Requirements: 10.1, 10.2, 10.3, 10.4_
  
  - [ ] 14.2 Add rollback configuration and conditional execution
    - Add enable_auto_rollback boolean variable to Terraform
    - Pass variable to rollback script via templatefile
    - Only execute destroy when enable_auto_rollback=true
    - Display notification when disabled (no destroy)
    - _Requirements: 10.5, 10.6_
  
  - [ ] 14.3 Integrate rollback agent with Terraform workflow
    - Add null_resource to trigger rollback monitor after apply
    - Add variable to enable/disable feature flag for rollback
    - Document rollback behavior in outputs
    - _Requirements: 14.6_

- [ ] 15. Checkpoint - Verify operational tooling
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 16. Create deployment report generator
  - [ ] 16.1 Design deployment-report.tpl template
    - Add metadata section (timestamp, git commit hash)
    - Add access section (public IP, SSH command)
    - Create endpoints table for all 4 services + RabbitMQ
    - Include health endpoints and Swagger UI links
    - _Requirements: 11.2, 11.3, 11.4, 11.5_
  
  - [ ] 16.2 Add credentials and troubleshooting sections
    - Include RabbitMQ username and password
    - Add troubleshooting section with diagnostic commands
    - List all generated maintenance scripts with descriptions
    - _Requirements: 11.6, 11.7_
  
  - [ ] 16.3 Integrate report generation with Terraform
    - Add data source for git commit hash (external or null_resource)
    - Create local_file resource with timestamped filename
    - Generate report after successful apply
    - Add feature flag check for deployment_report
    - _Requirements: 11.1, 11.8, 11.9, 14.10_

- [ ] 17. Enhance Terraform outputs
  - [ ] 17.1 Add operational command outputs
    - Add output for direct SSH command to view bootstrap log
    - Add output for container status check command
    - Add output for verification script path
    - _Requirements: 6.1, 6.4, 6.6_
  
  - [ ] 17.2 Add endpoint and timing outputs
    - Create structured output with all health endpoint URLs
    - Create structured output with all Swagger UI URLs
    - Add estimated ready time (15-20 minutes) with monitoring command
    - _Requirements: 6.2, 6.3, 6.5_

- [ ] 18. Ensure backward compatibility
  - [ ] 18.1 Verify existing Terraform structure preserved
    - Confirm separate files maintained (main.tf, vm.tf, network.tf, ssh-key.tf, providers.tf, variables.tf, outputs.tf)
    - Verify all existing variables keep same names and defaults
    - Verify network configuration unchanged (VNet, subnet, NSG, ports)
    - Verify Standard_B2s remains default VM type
    - _Requirements: 12.2, 12.3, 12.5, 12.6_
  
  - [ ] 18.2 Verify docker-compose.yml compatibility
    - Ensure no modifications to existing docker-compose.yml required
    - Verify .env file generation maintains same variables
    - Test that all features disabled equals original behavior
    - _Requirements: 12.1, 12.4, 12.7_

- [ ] 19. Update cloud-init configuration
  - [ ] 19.1 Create enhanced cloud-init.yaml template
    - Preserve existing cloud-init structure
    - Inject enhanced bootstrap.sh via write_files or runcmd
    - Pass feature flags as environment variables to bootstrap script
    - Ensure custom_data stays under 64KB Azure limit
    - _Requirements: 12.1, 12.7_
  
  - [ ] 19.2 Wire bootstrap script with all implemented features
    - Integrate all functions: retry, logging, sequential build, health checks, monitoring
    - Add feature flag checks before each enhancement
    - Log when features are skipped due to disabled flags
    - Ensure proper error handling and graceful failures
    - _Requirements: 14.7, 14.8_

- [ ] 20. Final checkpoint and integration testing
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- This is an Infrastructure as Code (IaC) project using Terraform (HCL) and Bash scripting
- Property-based testing is not applicable due to side effects and infrastructure provisioning
- Testing strategy focuses on: static validation (terraform validate, shellcheck), integration tests (actual deployment), and smoke tests
- All scripts are generated via Terraform `templatefile()` function for parameterization
- Feature flags allow incremental rollout and testing of individual improvements
- Backward compatibility is critical - all enhancements must be optional and non-breaking
- VM resource constraints (4GB RAM) drive sequential build strategy
- All maintenance and verification scripts require SSH access to deployed VM
- Checkpoints ensure incremental validation before proceeding to next phase

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1"] },
    { "id": 1, "tasks": ["2.1", "3.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "3.2"] },
    { "id": 3, "tasks": ["5.1", "5.2"] },
    { "id": 4, "tasks": ["5.3", "6.1", "8.1"] },
    { "id": 5, "tasks": ["6.2", "8.2"] },
    { "id": 6, "tasks": ["6.3", "8.3", "9.1"] },
    { "id": 7, "tasks": ["9.2", "10.1", "12.1", "12.2"] },
    { "id": 8, "tasks": ["9.3", "10.2", "12.3", "12.4", "13.1"] },
    { "id": 9, "tasks": ["10.3", "12.5", "13.2", "14.1"] },
    { "id": 10, "tasks": ["10.4", "13.3", "14.2", "16.1"] },
    { "id": 11, "tasks": ["13.4", "14.3", "16.2", "17.1"] },
    { "id": 12, "tasks": ["16.3", "17.2", "18.1"] },
    { "id": 13, "tasks": ["18.2", "19.1"] },
    { "id": 14, "tasks": ["19.2"] }
  ]
}
```
