#!/bin/bash

# ===========================
# AIMS Backend Startup Script
# Spring Boot API Server
# ===========================

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
BACKEND_PORT=8080
LOGS_DIR="logs"
BACKEND_LOG_FILE="$LOGS_DIR/backend.log"

# Create logs directory
mkdir -p "$LOGS_DIR"

print_status() {
    echo -e "${BLUE}[BACKEND]${NC} $1" | tee -a "$BACKEND_LOG_FILE"
}

print_success() {
    echo -e "${GREEN}[BACKEND SUCCESS]${NC} $1" | tee -a "$BACKEND_LOG_FILE"
}

print_warning() {
    echo -e "${YELLOW}[BACKEND WARNING]${NC} $1" | tee -a "$BACKEND_LOG_FILE"
}

print_error() {
    echo -e "${RED}[BACKEND ERROR]${NC} $1" | tee -a "$BACKEND_LOG_FILE"
}

# Header
echo -e "${CYAN}╔══════════════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║${NC}                           ${BLUE}AIMS BACKEND SERVER${NC}                              ${CYAN}║${NC}"
echo -e "${CYAN}║${NC}                        Spring Boot API (Port $BACKEND_PORT)                         ${CYAN}║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"
echo ""

print_status "$(date): Starting AIMS Backend Server..." | tee "$BACKEND_LOG_FILE"

# Set environment variables
export SPRING_PROFILES_ACTIVE=dev
export SERVER_PORT=$BACKEND_PORT
export SPRING_DATASOURCE_URL=jdbc:sqlite:aims_database.db

print_status "Environment configured:"
print_status "  - Profile: $SPRING_PROFILES_ACTIVE"
print_status "  - Port: $SERVER_PORT"
print_status "  - Database: $SPRING_DATASOURCE_URL"

# Database check
if [ -f "aims_database.db" ]; then
    print_success "Database file found: aims_database.db"
else
    print_warning "Database file not found, will be created on first run"
fi

print_status "Cleaning and compiling project..."
if mvn clean compile -q -Dmaven.test.skip=true; then
    print_success "Project compiled successfully (tests skipped)"
else
    print_error "Failed to compile project"
    print_error "Please check compilation errors above"
    exit 1
fi

# Always skip tests for demo reliability
print_warning "[DEMO MODE] Skipping all tests for backend startup."

print_success "Starting Spring Boot application..."
print_status "Backend will be available at: http://localhost:$BACKEND_PORT"
print_status "API Documentation: http://localhost:$BACKEND_PORT/swagger-ui/index.html"
print_status "Health Check: http://localhost:$BACKEND_PORT/actuator/health"

echo ""
print_status "📋 Available API Endpoints:"
echo "  🔐 Authentication: http://localhost:$BACKEND_PORT/api/auth"
echo "  📦 Products: http://localhost:$BACKEND_PORT/api/products"
echo "  🛒 Cart: http://localhost:$BACKEND_PORT/api/cart"
echo "  📋 Orders: http://localhost:$BACKEND_PORT/api/orders"
echo "  👥 Users: http://localhost:$BACKEND_PORT/api/users"
echo "  💳 Payments: http://localhost:$BACKEND_PORT/api/payments"
echo "  🔧 Admin Products: http://localhost:$BACKEND_PORT/api/admin/products"
echo ""
echo "Press Ctrl+C to stop the backend server"
echo ""

# Start with logging
mvn spring-boot:run -Dmaven.test.skip=true 2>&1 | tee -a "$BACKEND_LOG_FILE"

print_status "$(date): Backend server stopped" | tee -a "$BACKEND_LOG_FILE"