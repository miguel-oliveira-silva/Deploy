#!/bin/bash

#######################################
# Enhanced Bootstrap Script for Markovitz Azure Infrastructure
# This template provides retry logic, structured logging, and monitoring
#######################################

# Configuration
LOG_FILE="/var/log/markovitz-bootstrap.log"

#######################################
# Logging Function
# Outputs structured logs with timestamp, level, and component
# Arguments:
#   $1 - Level (INFO, WARN, ERROR)
#   $2 - Component (GIT, DOCKER, APT, HEALTHCHECK, SYSTEM, MONITOR)
#   $3 - Message
# Validates: Requirements 1.4, 4.3
#######################################
log() {
  local level=$1
  local component=$2
  local message=$3
  local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%S+00:00")
  echo "[$timestamp] [$level] [$component] $message" | tee -a "$LOG_FILE"
}

#######################################
# Generic Retry Function
# Executes a command with configurable retry attempts and interval
# Arguments:
#   $1 - max_attempts: Maximum number of retry attempts
#   $2 - interval: Seconds to wait between retries
#   $3 - component: Component name for logging (GIT, DOCKER, APT, etc.)
#   $4+ - command: The command to execute (all remaining arguments)
# Returns:
#   0 - Command succeeded within max_attempts
#   1 - Command failed after all retries exhausted
# Validates: Requirements 1.1, 1.4, 4.3
#######################################
retry_command() {
  local max_attempts=$1
  local interval=$2
  local component=$3
  shift 3
  local command=("$@")
  local attempt=1
  
  while [ $attempt -le $max_attempts ]; do
    log "INFO" "$component" "Executing command (attempt $attempt/$max_attempts): $${command[*]}"
    
    # Execute the command
    if "$${command[@]}"; then
      log "INFO" "$component" "Command succeeded on attempt $attempt/$max_attempts"
      return 0
    else
      local exit_code=$?
      
      if [ $attempt -lt $max_attempts ]; then
        log "WARN" "$component" "Command failed with exit code $exit_code, retry $attempt/$max_attempts. Waiting $${interval}s before next attempt"
        sleep "$interval"
      else
        log "ERROR" "$component" "Command failed after $max_attempts retries with exit code $exit_code"
        return 1
      fi
    fi
    
    ((attempt++))
  done
  
  return 1
}

#######################################
# Specialized Retry Function: Git Clone
# Wrapper around retry_command for git clone operations
# Arguments:
#   $1 - repository_url: Git repository URL to clone
#   $2 - destination: Target directory for clone
#   $3 - branch (optional): Specific branch to clone
# Returns:
#   0 - Clone succeeded within 3 attempts
#   1 - Clone failed after all retries exhausted
# Validates: Requirements 1.1, 1.4
#######################################
retry_git_clone() {
  local repository_url=$1
  local destination=$2
  local branch=$${3:-""}
  
  if [ -n "$branch" ]; then
    retry_command 3 10 "GIT" git clone --branch "$branch" --depth 1 "$repository_url" "$destination"
  else
    retry_command 3 10 "GIT" git clone --depth 1 "$repository_url" "$destination"
  fi
  return $?
}

#######################################
# Specialized Retry Function: Docker Build
# Wrapper around retry_command for docker build operations
# Arguments:
#   $1 - image_name: Name/tag for the Docker image
#   $2+ - Additional docker build arguments (e.g., -f Dockerfile, context path)
# Returns:
#   0 - Build succeeded within 2 attempts
#   1 - Build failed after all retries exhausted
# Validates: Requirements 1.2, 1.4
#######################################
retry_docker_build() {
  local image_name=$1
  shift 1
  local build_args=("$@")
  
  retry_command 2 5 "DOCKER" docker build -t "$image_name" "$${build_args[@]}"
  return $?
}

#######################################
# Specialized Retry Function: apt-get
# Wrapper around retry_command for apt-get operations with cache cleaning
# Arguments:
#   $1+ - apt-get command and arguments (e.g., "update", "install docker.io")
# Returns:
#   0 - apt-get succeeded within 3 attempts
#   1 - apt-get failed after all retries exhausted
# Validates: Requirements 1.3, 1.4
# Note: Executes apt-get clean before each retry attempt
#######################################
retry_apt_get() {
  local apt_args=("$@")
  local max_attempts=3
  local interval=5
  local attempt=1
  
  while [ $attempt -le $max_attempts ]; do
    log "INFO" "APT" "Executing apt-get $${apt_args[*]} (attempt $attempt/$max_attempts)"
    
    # Execute apt-get command
    if apt-get "$${apt_args[@]}"; then
      log "INFO" "APT" "apt-get command succeeded on attempt $attempt/$max_attempts"
      return 0
    else
      local exit_code=$?
      
      if [ $attempt -lt $max_attempts ]; then
        log "WARN" "APT" "apt-get failed with exit code $exit_code, retry $attempt/$max_attempts. Cleaning cache before next attempt"
        # Clean apt cache before retry
        apt-get clean
        sleep "$interval"
      else
        log "ERROR" "APT" "apt-get command failed after $max_attempts retries with exit code $exit_code"
        return 1
      fi
    fi
    
    ((attempt++))
  done
  
  return 1
}

#######################################
# Build Infrastructure Containers Sequentially
# Builds postgres and rabbitmq images sequentially before service containers
# For infrastructure containers using pre-built images, this pulls them
# For custom Dockerfiles, this would build them
# Arguments:
#   None (uses global REPO_DIR and ENABLE_RETRY_LOGIC variables)
# Returns:
#   0 - All infrastructure images built successfully
#   1 - One or more infrastructure image builds failed
# Validates: Requirements 2.2, 2.6
#######################################
build_infrastructure_sequential() {
  log "INFO" "DOCKER" "Starting sequential build of infrastructure containers"
  
  cd "$REPO_DIR"
  
  # Build/Pull postgres image
  log "INFO" "DOCKER" "Building postgres image (started)"
  local postgres_build_start=$(date +%s)
  local memory_before_postgres=$(free -m | awk 'NR==2 {printf "%.1f", $3/1024}')
  
  # Use docker compose build for postgres service
  if [ "$ENABLE_RETRY_LOGIC" = "true" ]; then
    if ! retry_command 2 5 "DOCKER" docker compose build postgres; then
      log "ERROR" "DOCKER" "Failed to build postgres image after retries"
      return 1
    fi
  else
    if ! docker compose build postgres; then
      log "ERROR" "DOCKER" "Failed to build postgres image"
      return 1
    fi
  fi
  
  local postgres_build_end=$(date +%s)
  local postgres_build_duration=$((postgres_build_end - postgres_build_start))
  local memory_after_postgres=$(free -m | awk 'NR==2 {printf "%.1f", $3/1024}')
  log "INFO" "DOCKER" "Building postgres image (completed in $${postgres_build_duration}s, memory: $${memory_after_postgres}GB)"
  
  # Build/Pull rabbitmq image
  log "INFO" "DOCKER" "Building rabbitmq image (started)"
  local rabbitmq_build_start=$(date +%s)
  local memory_before_rabbitmq=$(free -m | awk 'NR==2 {printf "%.1f", $3/1024}')
  
  # Use docker compose build for rabbitmq service
  if [ "$ENABLE_RETRY_LOGIC" = "true" ]; then
    if ! retry_command 2 5 "DOCKER" docker compose build rabbitmq; then
      log "ERROR" "DOCKER" "Failed to build rabbitmq image after retries"
      return 1
    fi
  else
    if ! docker compose build rabbitmq; then
      log "ERROR" "DOCKER" "Failed to build rabbitmq image"
      return 1
    fi
  fi
  
  local rabbitmq_build_end=$(date +%s)
  local rabbitmq_build_duration=$((rabbitmq_build_end - rabbitmq_build_start))
  local memory_after_rabbitmq=$(free -m | awk 'NR==2 {printf "%.1f", $3/1024}')
  log "INFO" "DOCKER" "Building rabbitmq image (completed in $${rabbitmq_build_duration}s, memory: $${memory_after_rabbitmq}GB)"
  
  log "INFO" "DOCKER" "Sequential build of infrastructure containers completed successfully"
  return 0
}

#######################################
# Check Memory Usage
# Returns 0 if memory usage is below threshold (3.5GB), 1 otherwise
# Validates: Requirement 2.4
#######################################
check_memory_threshold() {
  local memory_used=$(free -m | awk 'NR==2 {printf "%.1f", $3/1024}')
  local threshold=3.5
  
  # Use bc for floating point comparison
  if command -v bc &> /dev/null; then
    if [ $(echo "$memory_used >= $threshold" | bc -l) -eq 1 ]; then
      return 1
    fi
  else
    # Fallback: convert to integer comparison (3500 MB)
    local memory_used_mb=$(free -m | awk 'NR==2 {print $3}')
    if [ $memory_used_mb -ge 3500 ]; then
      return 1
    fi
  fi
  
  return 0
}

#######################################
# Wait for Containers Running
# Waits until all 6 containers are in "running" state
# Arguments:
#   None (uses global REPO_DIR variable)
# Returns:
#   0 - All containers are running
#   1 - Timeout waiting for containers
# Validates: Requirement 3.1
#######################################
wait_for_containers_running() {
  log "INFO" "HEALTHCHECK" "Waiting for all containers to reach running state"
  
  local max_wait=60
  local interval=5
  local elapsed=0
  local target_count=6
  
  while [ $elapsed -lt $max_wait ]; do
    local running_count=$(docker compose -f "$REPO_DIR/docker-compose.yml" ps --format json 2>/dev/null | grep -c '"State":"running"' || echo "0")
    
    log "INFO" "HEALTHCHECK" "Containers running: $running_count/$target_count"
    
    if [ $running_count -eq $target_count ]; then
      log "INFO" "HEALTHCHECK" "All containers are running"
      return 0
    fi
    
    sleep $interval
    elapsed=$((elapsed + interval))
  done
  
  log "ERROR" "HEALTHCHECK" "Timeout waiting for containers to start ($elapsed seconds elapsed)"
  return 1
}

#######################################
# Resource Monitor (Background Process)
# Monitors CPU, memory, and disk usage during bootstrap
# Logs metrics every 30s (CPU/memory) and 60s (disk)
# Tracks peak usage and terminates when bootstrap completes
# Arguments:
#   None (uses global LOG_FILE and sentinel file)
# Returns:
#   None (runs in background until sentinel file created)
# Validates: Requirements 9.1, 9.2, 9.3, 9.4, 9.5, 9.6
#######################################
monitor_resources() {
  local cpu_interval=30
  local disk_interval=60
  local last_disk_check=0
  local peak_cpu=0
  local peak_memory=0
  
  log "INFO" "MONITOR" "Resource monitoring started (CPU/memory: $${cpu_interval}s, disk: $${disk_interval}s)"
  
  while [ ! -f /tmp/bootstrap-complete ]; do
    # Get current timestamp for disk interval calculation
    local current_time=$(date +%s)
    
    # CPU usage
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)
    
    # Memory usage
    local mem_total=$(free -m | awk 'NR==2 {print $2}')
    local mem_used=$(free -m | awk 'NR==2 {print $3}')
    local mem_available=$(free -m | awk 'NR==2 {print $7}')
    local mem_percent=$(awk "BEGIN {printf \"%.1f\", ($mem_used/$mem_total)*100}")
    local mem_gb=$(awk "BEGIN {printf \"%.1f\", $mem_used/1024}")
    
    # Log CPU and memory
    log "INFO" "MONITOR" "CPU: $${cpu_usage}%, Memory: $${mem_used}MB/$${mem_total}MB ($${mem_percent}%), Available: $${mem_available}MB"
    
    # Track peak CPU
    if command -v bc &> /dev/null; then
      if [ $(echo "$cpu_usage > $peak_cpu" | bc -l) -eq 1 ]; then
        peak_cpu=$cpu_usage
      fi
      if [ $(echo "$mem_gb > $peak_memory" | bc -l) -eq 1 ]; then
        peak_memory=$mem_gb
      fi
    else
      # Fallback without bc
      peak_cpu=$cpu_usage
      peak_memory=$mem_gb
    fi
    
    # Warning for high memory usage
    if [ $(echo "$mem_percent >= 90" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
      log "WARN" "MONITOR" "High memory usage: $${mem_percent}% ($${mem_gb}GB/$(awk "BEGIN {printf \"%.1f\", $mem_total/1024}")GB)"
    fi
    
    # Disk usage (every 60s)
    local time_since_disk=$((current_time - last_disk_check))
    if [ $time_since_disk -ge $disk_interval ] || [ $last_disk_check -eq 0 ]; then
      local disk_info=$(df -h / | awk 'NR==2 {print $3 "/" $2 " (" $5 ")"}')
      local disk_percent=$(df / | awk 'NR==2 {print $5}' | sed 's/%//')
      
      log "INFO" "MONITOR" "Disk: $disk_info"
      
      # Warning for high disk usage
      if [ $disk_percent -ge 80 ]; then
        log "WARN" "MONITOR" "High disk usage: $${disk_percent}%"
      fi
      
      last_disk_check=$current_time
    fi
    
    # Sleep for CPU/memory interval
    sleep $cpu_interval
  done
  
  # Log peak usage before terminating
  log "INFO" "MONITOR" "Resource monitoring completed. Peak CPU: $${peak_cpu}%, Peak Memory: $${peak_memory}GB"
}

#######################################
# Health Check Loop
# Polls service health endpoints until all are healthy or timeout
# Arguments:
#   $1 - timeout: Maximum seconds to wait (default 120)
# Returns:
#   0 - All 4 services are healthy
#   1 - Timeout or services failed health checks
# Validates: Requirements 3.1, 3.2, 3.3, 3.5
#######################################
health_check_loop() {
  local timeout=$${1:-120}
  local interval=10
  local elapsed=0
  
  # Service endpoints
  declare -A services=(
    ["user-service"]="8081"
    ["asset-service"]="8082"
    ["portfolio-service"]="8083"
    ["notification-service"]="8084"
  )
  
  declare -A healthy_services=()
  local healthy_count=0
  local target_count=4
  
  log "INFO" "HEALTHCHECK" "Starting health check loop (timeout: $${timeout}s, interval: $${interval}s)"
  
  while [ $elapsed -lt $timeout ]; do
    for service in "$${!services[@]}"; do
      # Skip if already marked healthy
      if [ "$${healthy_services[$service]}" = "true" ]; then
        continue
      fi
      
      local port=$${services[$service]}
      local health_url="http://localhost:$${port}/actuator/health"
      
      # Check health endpoint
      local response=$(curl -s "$health_url" 2>/dev/null || echo "")
      
      if echo "$response" | grep -q '"status":"UP"'; then
        local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%S+00:00")
        log "INFO" "HEALTHCHECK" "$service is healthy (timestamp: $timestamp)"
        healthy_services[$service]="true"
        ((healthy_count++))
      fi
    done
    
    # Check if all services are healthy
    if [ $healthy_count -eq $target_count ]; then
      log "INFO" "HEALTHCHECK" "All $target_count services are healthy"
      return 0
    fi
    
    log "INFO" "HEALTHCHECK" "Healthy services: $healthy_count/$target_count (elapsed: $${elapsed}s)"
    
    sleep $interval
    elapsed=$((elapsed + interval))
  done
  
  # Timeout - log which services failed
  log "ERROR" "HEALTHCHECK" "Health check timeout after $${timeout}s"
  
  local failed_services=""
  for service in "$${!services[@]}"; do
    if [ "$${healthy_services[$service]}" != "true" ]; then
      failed_services="$failed_services $service"
    fi
  done
  
  log "ERROR" "HEALTHCHECK" "Failed services:$failed_services"
  return 1
}

#######################################
# Build Service Containers in Pairs
# Builds microservices in two pairs to optimize memory usage on B2s VM (4GB RAM)
# Pair 1: user-service + asset-service
# Pair 2: portfolio-service + notification-service
# Arguments:
#   None (uses global REPO_DIR and ENABLE_RETRY_LOGIC variables)
# Returns:
#   0 - All service images built successfully
#   1 - One or more service image builds failed
# Validates: Requirements 2.1, 2.3, 2.4, 2.5, 2.6
#######################################
build_services_in_pairs() {
  log "INFO" "DOCKER" "Starting paired build of service containers"
  
  cd "$REPO_DIR"
  
  #######################################
  # PAIR 1: user-service + asset-service
  #######################################
  log "INFO" "DOCKER" "Building pair 1: user-service + asset-service"
  
  local pair1_build_start=$(date +%s)
  local memory_before_pair1=$(free -m | awk 'NR==2 {printf "%.1f", $3/1024}')
  
  # Build both services in parallel using background processes
  if [ "$ENABLE_RETRY_LOGIC" = "true" ]; then
    (retry_command 2 5 "DOCKER" docker compose build user-service) &
    local user_pid=$!
    (retry_command 2 5 "DOCKER" docker compose build asset-service) &
    local asset_pid=$!
    
    # Wait for both builds to complete
    wait $user_pid
    local user_exit=$?
    wait $asset_pid
    local asset_exit=$?
    
    if [ $user_exit -ne 0 ] || [ $asset_exit -ne 0 ]; then
      log "ERROR" "DOCKER" "Failed to build pair 1 services"
      return 1
    fi
  else
    (docker compose build user-service) &
    local user_pid=$!
    (docker compose build asset-service) &
    local asset_pid=$!
    
    # Wait for both builds to complete
    wait $user_pid
    local user_exit=$?
    wait $asset_pid
    local asset_exit=$?
    
    if [ $user_exit -ne 0 ] || [ $asset_exit -ne 0 ]; then
      log "ERROR" "DOCKER" "Failed to build pair 1 services"
      return 1
    fi
  fi
  
  local pair1_build_end=$(date +%s)
  local pair1_build_duration=$((pair1_build_end - pair1_build_start))
  local memory_after_pair1=$(free -m | awk 'NR==2 {printf "%.1f", $3/1024}')
  log "INFO" "DOCKER" "Pair 1 build completed in $${pair1_build_duration}s, memory: $${memory_after_pair1}GB"
  
  # Check memory and wait if needed before building pair 2
  if ! check_memory_threshold; then
    log "WARN" "DOCKER" "Memory usage above 3.5GB threshold, waiting 30s before next build pair"
    sleep 30
  else
    log "INFO" "DOCKER" "Memory usage below threshold, proceeding to pair 2"
  fi
  
  #######################################
  # PAIR 2: portfolio-service + notification-service
  #######################################
  log "INFO" "DOCKER" "Building pair 2: portfolio-service + notification-service"
  
  local pair2_build_start=$(date +%s)
  local memory_before_pair2=$(free -m | awk 'NR==2 {printf "%.1f", $3/1024}')
  
  # Build both services in parallel using background processes
  if [ "$ENABLE_RETRY_LOGIC" = "true" ]; then
    (retry_command 2 5 "DOCKER" docker compose build portfolio-service) &
    local portfolio_pid=$!
    (retry_command 2 5 "DOCKER" docker compose build notification-service) &
    local notification_pid=$!
    
    # Wait for both builds to complete
    wait $portfolio_pid
    local portfolio_exit=$?
    wait $notification_pid
    local notification_exit=$?
    
    if [ $portfolio_exit -ne 0 ] || [ $notification_exit -ne 0 ]; then
      log "ERROR" "DOCKER" "Failed to build pair 2 services"
      return 1
    fi
  else
    (docker compose build portfolio-service) &
    local portfolio_pid=$!
    (docker compose build notification-service) &
    local notification_pid=$!
    
    # Wait for both builds to complete
    wait $portfolio_pid
    local portfolio_exit=$?
    wait $notification_pid
    local notification_exit=$?
    
    if [ $portfolio_exit -ne 0 ] || [ $notification_exit -ne 0 ]; then
      log "ERROR" "DOCKER" "Failed to build pair 2 services"
      return 1
    fi
  fi
  
  local pair2_build_end=$(date +%s)
  local pair2_build_duration=$((pair2_build_end - pair2_build_start))
  local memory_after_pair2=$(free -m | awk 'NR==2 {printf "%.1f", $3/1024}')
  log "INFO" "DOCKER" "Pair 2 build completed in $${pair2_build_duration}s, memory: $${memory_after_pair2}GB"
  
  log "INFO" "DOCKER" "Paired build of service containers completed successfully"
  return 0
}

#######################################
# Main Bootstrap Logic
# Integrates retry logic with feature flag checks
# Validates: Requirements 1.5, 14.2
#######################################

# Feature flags passed from Terraform as environment variables
ENABLE_RETRY_LOGIC="${enable_retry_logic}"
ENABLE_STRUCTURED_LOGS="${enable_structured_logs}"
ENABLE_SEQUENTIAL_BUILD="${enable_sequential_build}"
ENABLE_HEALTH_CHECKS="${enable_health_checks}"
ENABLE_RESOURCE_MONITORING="${enable_resource_monitoring}"

# Configuration
REPO_DIR="/opt/markovitz/app"
ADMIN_USERNAME="${admin_username}"
GIT_REPO_URL="${git_repo_url}"
GIT_REPO_BRANCH="${git_repo_branch}"
DB_USER="${db_user}"
DB_PASSWORD="${db_password}"
RABBITMQ_USER="${rabbitmq_user}"
RABBITMQ_PASSWORD="${rabbitmq_password}"

# Initialize log file
mkdir -p "$(dirname "$LOG_FILE")"
touch "$LOG_FILE"

# Record bootstrap start time for duration calculation
BOOTSTRAP_START_TIME=$(date +%s)

# Log separator at bootstrap start (graceful failure handling)
{
  log "INFO" "SYSTEM" "========================================="
  log "INFO" "SYSTEM" "Markovitz Bootstrap Started"
  log "INFO" "SYSTEM" "========================================="
} 2>/dev/null || true

log "INFO" "SYSTEM" "Feature Flags: retry_logic=$ENABLE_RETRY_LOGIC, structured_logs=$ENABLE_STRUCTURED_LOGS, sequential_build=$ENABLE_SEQUENTIAL_BUILD, health_checks=$ENABLE_HEALTH_CHECKS, resource_monitoring=$ENABLE_RESOURCE_MONITORING"

# Start resource monitoring in background if enabled
if [ "$ENABLE_RESOURCE_MONITORING" = "true" ]; then
  log "INFO" "MONITOR" "Starting resource monitoring in background"
  (monitor_resources) &
  MONITOR_PID=$!
else
  log "INFO" "MONITOR" "Resource monitoring disabled"
fi

#######################################
# 1) INSTALAÃ‡ÃƒO DO DOCKER ENGINE + COMPOSE PLUGIN
#######################################
if ! command -v docker &> /dev/null; then
  log "INFO" "DOCKER" "Docker not found, installing..."
  
  # Preparar diretÃ³rios e chaves
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  
  # Adicionar repositÃ³rio Docker
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
    $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
    tee /etc/apt/sources.list.d/docker.list > /dev/null
  
  # Instalar Docker com ou sem retry logic
  if [ "$ENABLE_RETRY_LOGIC" = "true" ]; then
    log "INFO" "APT" "Retry logic enabled, using retry_apt_get for updates and installation"
    
    # apt-get update com retry
    if ! retry_apt_get update -y; then
      log "ERROR" "APT" "Failed to update apt cache after retries. Aborting bootstrap."
      exit 1
    fi
    
    # apt-get install com retry
    if ! retry_apt_get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin; then
      log "ERROR" "APT" "Failed to install Docker after retries. Aborting bootstrap."
      exit 1
    fi
  else
    log "INFO" "APT" "Retry logic disabled, using direct apt-get commands"
    apt-get update -y
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  fi
  
  # Configurar e iniciar Docker
  systemctl enable docker
  systemctl start docker
  
  # Adicionar usuÃ¡rio ao grupo docker
  usermod -aG docker "$ADMIN_USERNAME"
  log "INFO" "DOCKER" "Docker installed successfully"
else
  log "INFO" "DOCKER" "Docker already installed, skipping installation"
fi

#######################################
# 2) CLONE DO REPOSITÃ“RIO GIT
#######################################
if [ ! -d "$REPO_DIR/.git" ]; then
  log "INFO" "GIT" "Repository not found, cloning from $GIT_REPO_URL (branch $GIT_REPO_BRANCH)"
  rm -rf "$REPO_DIR"
  
  # Clone com ou sem retry logic
  if [ "$ENABLE_RETRY_LOGIC" = "true" ]; then
    log "INFO" "GIT" "Retry logic enabled, using retry_git_clone"
    
    if ! retry_git_clone "$GIT_REPO_URL" "$REPO_DIR" "$GIT_REPO_BRANCH"; then
      log "ERROR" "GIT" "Failed to clone repository after retries. Aborting bootstrap."
      exit 1
    fi
  else
    log "INFO" "GIT" "Retry logic disabled, using direct git clone"
    git clone --branch "$GIT_REPO_BRANCH" --depth 1 "$GIT_REPO_URL" "$REPO_DIR"
  fi
else
  log "INFO" "GIT" "Repository already exists, updating (git pull)"
  cd "$REPO_DIR"
  git fetch origin "$GIT_REPO_BRANCH"
  git reset --hard "origin/$GIT_REPO_BRANCH"
fi

cd "$REPO_DIR"

#######################################
# 3) GERAR ARQUIVO .env COM CREDENCIAIS
#######################################
log "INFO" "SYSTEM" "Generating .env file with credentials"
cat > "$REPO_DIR/.env" <<EOF
DB_USER=$DB_USER
DB_PASSWORD=$DB_PASSWORD
RABBITMQ_USER=$RABBITMQ_USER
RABBITMQ_PASSWORD=$RABBITMQ_PASSWORD
RISK_FREE_RATE=0.1075
EOF
chmod 600 "$REPO_DIR/.env"
log "INFO" "SYSTEM" ".env file created successfully"

#######################################
# 4) BUILD E START DOS CONTAINERS
#######################################
log "INFO" "DOCKER" "Starting build and deployment of containers"

# Check if sequential build is enabled
if [ "$ENABLE_SEQUENTIAL_BUILD" = "true" ]; then
  log "INFO" "DOCKER" "Sequential build strategy enabled"
  
  # Build infrastructure containers first
  if ! build_infrastructure_sequential; then
    log "ERROR" "DOCKER" "Failed to build infrastructure containers. Aborting bootstrap."
    exit 1
  fi
  
  # Build service containers in pairs
  if ! build_services_in_pairs; then
    log "ERROR" "DOCKER" "Failed to build service containers. Aborting bootstrap."
    exit 1
  fi
  
  # Start all containers (images are already built)
  log "INFO" "DOCKER" "Starting all containers"
  if ! docker compose -f "$REPO_DIR/docker-compose.yml" up -d --no-build; then
    log "ERROR" "DOCKER" "Failed to start containers. Aborting bootstrap."
    exit 1
  fi
else
  log "INFO" "DOCKER" "Sequential build strategy disabled, using standard parallel build"
  
  # Use standard docker compose build and start
  if ! docker compose -f "$REPO_DIR/docker-compose.yml" up -d --build; then
    log "ERROR" "DOCKER" "Failed to build and start containers. Aborting bootstrap."
    exit 1
  fi
fi

log "INFO" "DOCKER" "Containers started successfully"

# Run health checks if enabled
if [ "$ENABLE_HEALTH_CHECKS" = "true" ]; then
  log "INFO" "HEALTHCHECK" "Health checks enabled, waiting for containers to be running"
  
  # Wait for containers to reach running state
  if ! wait_for_containers_running; then
    log "ERROR" "HEALTHCHECK" "Containers failed to reach running state. Aborting bootstrap."
    exit 1
  fi
  
  # Run health check loop
  if ! health_check_loop 120; then
    log "ERROR" "HEALTHCHECK" "Health checks failed. Bootstrap incomplete."
    exit 1
  fi
  
  log "INFO" "HEALTHCHECK" "All health checks passed"
else
  log "INFO" "HEALTHCHECK" "Health checks disabled, skipping validation"
fi

#######################################
# 5) FINALIZAÃ‡ÃƒO DO BOOTSTRAP
#######################################

# Create sentinel file to stop resource monitoring
touch /tmp/bootstrap-complete

# Wait for monitor to finish if it was started
if [ "$ENABLE_RESOURCE_MONITORING" = "true" ] && [ -n "$MONITOR_PID" ]; then
  wait $MONITOR_PID 2>/dev/null || true
fi

# Calculate bootstrap duration
BOOTSTRAP_END_TIME=$(date +%s)
BOOTSTRAP_DURATION=$((BOOTSTRAP_END_TIME - BOOTSTRAP_START_TIME))

# Log separator at bootstrap end (graceful failure handling)
{
  log "INFO" "SYSTEM" "========================================="
  log "INFO" "SYSTEM" "Bootstrap concluÃ­do com sucesso"
  log "INFO" "SYSTEM" "Total duration: $${BOOTSTRAP_DURATION}s"
  log "INFO" "SYSTEM" "========================================="
} 2>/dev/null || true

# Exibir status dos containers
docker compose -f "$REPO_DIR/docker-compose.yml" ps
