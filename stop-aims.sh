#!/bin/bash

# =================================
# AIMS Services Shutdown Script
# Graceful shutdown for all services
# =================================

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
BACKEND_PORT=8080
FRONTEND_PORT=3000
LOGS_DIR="logs"
SHUTDOWN_LOG_FILE="$LOGS_DIR/shutdown.log"

# Create logs directory
mkdir -p "$LOGS_DIR"

print_header() {
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}                           ${RED}AIMS PROJECT SHUTDOWN${NC}                            ${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}                        Graceful Services Termination                        ${CYAN}║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_status() {
    echo -e "${BLUE}[SHUTDOWN]${NC} $1" | tee -a "$SHUTDOWN_LOG_FILE"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$SHUTDOWN_LOG_FILE"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$SHUTDOWN_LOG_FILE"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$SHUTDOWN_LOG_FILE"
}

# Function to kill process on port
kill_port_process() {
    local port=$1
    local service_name=$2
    
    print_status "Stopping $service_name on port $port..."
    
    # Find processes using the port
    if command -v lsof &> /dev/null; then
        local pids=$(lsof -ti :$port 2>/dev/null)
    elif command -v netstat &> /dev/null; then
        local pids=$(netstat -tlnp 2>/dev/null | grep ":$port " | awk '{print $7}' | cut -d'/' -f1)
    else
        print_warning "Cannot find process management tools (lsof/netstat)"
        return 1
    fi
    
    if [ -z "$pids" ]; then
        print_warning "No processes found running on port $port"
        return 0
    fi
    
    print_status "Found processes on port $port: $pids"
    
    # First try graceful shutdown (SIGTERM)
    for pid in $pids; do
        if kill -0 $pid 2>/dev/null; then
            print_status "Sending SIGTERM to process $pid..."
            kill -TERM $pid 2>/dev/null
        fi
    done
    
    # Wait for graceful shutdown
    sleep 3
    
    # Check if processes are still running
    local remaining_pids=""
    for pid in $pids; do
        if kill -0 $pid 2>/dev/null; then
            remaining_pids="$remaining_pids $pid"
        fi
    done
    
    # Force kill if necessary
    if [ ! -z "$remaining_pids" ]; then
        print_warning "Processes still running, forcing termination..."
        for pid in $remaining_pids; do
            if kill -0 $pid 2>/dev/null; then
                print_status "Sending SIGKILL to process $pid..."
                kill -KILL $pid 2>/dev/null
            fi
        done
        sleep 1
    fi
    
    # Final verification
    if command -v lsof &> /dev/null; then
        local final_check=$(lsof -ti :$port 2>/dev/null)
    elif command -v netstat &> /dev/null; then
        local final_check=$(netstat -tlnp 2>/dev/null | grep ":$port " | awk '{print $7}' | cut -d'/' -f1)
    fi
    
    if [ -z "$final_check" ]; then
        print_success "$service_name stopped successfully"
        return 0
    else
        print_error "Failed to stop $service_name completely"
        return 1
    fi
}

# Function to kill processes by name pattern
kill_by_name() {
    local pattern=$1
    local service_name=$2
    
    print_status "Looking for $service_name processes..."
    
    local pids=$(pgrep -f "$pattern" 2>/dev/null)
    
    if [ -z "$pids" ]; then
        print_status "No $service_name processes found"
        return 0
    fi
    
    print_status "Found $service_name processes: $pids"
    
    # Graceful shutdown
    for pid in $pids; do
        if kill -0 $pid 2>/dev/null; then
            print_status "Stopping $service_name process $pid..."
            kill -TERM $pid 2>/dev/null
        fi
    done
    
    sleep 2
    
    # Force kill if necessary
    local remaining=$(pgrep -f "$pattern" 2>/dev/null)
    if [ ! -z "$remaining" ]; then
        print_warning "Force killing remaining $service_name processes..."
        pkill -KILL -f "$pattern" 2>/dev/null
    fi
    
    print_success "$service_name processes stopped"
}

print_header
print_status "$(date): Starting AIMS Project shutdown..." | tee "$SHUTDOWN_LOG_FILE"

# Stop backend service
print_status "🔧 Stopping Spring Boot Backend..."
kill_port_process $BACKEND_PORT "Backend"

# Stop frontend service
print_status "🌐 Stopping React Frontend..."
kill_port_process $FRONTEND_PORT "Frontend"

# Also check common alternative ports and kill related processes
print_status "🔍 Checking for related processes..."

# Kill any remaining Spring Boot processes
kill_by_name "spring-boot:run" "Spring Boot"
kill_by_name "maven.*spring-boot" "Maven Spring Boot"

# Kill any remaining Vite/Node processes
kill_by_name "vite.*dev" "Vite Dev Server"
kill_by_name "node.*vite" "Node Vite"

# Kill any remaining npm processes for this project
if [ -d "web-ui" ]; then
    cd web-ui
    kill_by_name "npm.*run.*dev" "NPM Dev"
    cd ..
fi

# Additional cleanup for common development ports
for alt_port in 3001 8081 8082 5173; do
    if command -v lsof &> /dev/null; then
        local alt_pids=$(lsof -ti :$alt_port 2>/dev/null)
        if [ ! -z "$alt_pids" ]; then
            print_warning "Found processes on alternative port $alt_port, cleaning up..."
            kill_port_process $alt_port "Alternative Service"
        fi
    fi
done

# Final verification
print_status "🔍 Final verification..."
sleep 1

backend_running=false
frontend_running=false

if command -v lsof &> /dev/null; then
    if lsof -ti :$BACKEND_PORT >/dev/null 2>&1; then
        backend_running=true
    fi
    if lsof -ti :$FRONTEND_PORT >/dev/null 2>&1; then
        frontend_running=true
    fi
fi

# Summary
echo ""
if [ "$backend_running" = false ] && [ "$frontend_running" = false ]; then
    print_success "🎉 All AIMS services stopped successfully!"
    echo ""
    echo -e "${GREEN}╔══════════════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║${NC}                             ${GREEN}SHUTDOWN COMPLETE${NC}                               ${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}                                                                              ${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}  ✅ Backend stopped (port $BACKEND_PORT)                                               ${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}  ✅ Frontend stopped (port $FRONTEND_PORT)                                              ${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}  ✅ All related processes terminated                                        ${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}                                                                              ${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}  💡 Use './start-aims.sh' to restart all services                          ${GREEN}║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"
else
    print_warning "Some services may still be running:"
    if [ "$backend_running" = true ]; then
        print_warning "  - Backend still detected on port $BACKEND_PORT"
    fi
    if [ "$frontend_running" = true ]; then
        print_warning "  - Frontend still detected on port $FRONTEND_PORT"
    fi
    echo ""
    print_status "You may need to manually check and kill remaining processes:"
    print_status "  - Check processes: lsof -i :$BACKEND_PORT,:$FRONTEND_PORT"
    print_status "  - Manual kill: kill -9 <PID>"
fi

echo ""
print_status "$(date): AIMS Project shutdown completed" | tee -a "$SHUTDOWN_LOG_FILE"