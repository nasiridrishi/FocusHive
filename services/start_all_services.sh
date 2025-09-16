#!/bin/bash

# =========================================
# FOCUSHIVE SERVICES LAUNCHER
# =========================================
# This script launches all FocusHive services in separate Terminal tabs
# =========================================

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Get the absolute path to the services directory
SERVICES_DIR="$(cd "$(dirname "$0")" && pwd)"

print_info "Starting FocusHive services in separate Terminal tabs..."

# Service configurations: name, directory, port
declare -a services=(
    "Identity Service:identity-service:8081"
    "Notification Service:notification-service:8083" 
    "Buddy Service:buddy-service:8087"
    "FocusHive Backend:focushive-backend:8080"
)

# Launch each service in a new Terminal tab
for service_config in "${services[@]}"; do
    IFS=':' read -r service_name service_dir service_port <<< "$service_config"
    
    print_info "Launching $service_name (port $service_port)..."
    
    # Create AppleScript command to open new Terminal tab and run the service
    osascript << EOF
        tell application "Terminal"
            if not (exists window 1) then
                do script "cd '$SERVICES_DIR/$service_dir' && echo 'Starting $service_name on port $service_port...' && ./run_local.sh"
            else
                tell application "System Events" to keystroke "t" using command down
                delay 0.5
                do script "cd '$SERVICES_DIR/$service_dir' && echo 'Starting $service_name on port $service_port...' && ./run_local.sh" in front window
            end if
        end tell
EOF
    
    # Small delay between launches
    sleep 1
done

print_success "All services launched in Terminal tabs!"
print_info ""
print_info "Services will be available at:"
print_info "  • Identity Service: http://localhost:8081"
print_info "  • Notification Service: http://localhost:8083" 
print_info "  • Buddy Service: http://localhost:8087"
print_info "  • FocusHive Backend: http://localhost:8080"
print_info ""
print_info "Each service will setup its database automatically on first run."
print_info "Use Cmd+W to close individual service tabs when done."