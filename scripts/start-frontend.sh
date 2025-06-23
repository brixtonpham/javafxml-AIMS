#!/bin/bash

# ============================
# AIMS Frontend Startup Script
# React + Vite Development Server
# ============================

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
FRONTEND_PORT=3000
WEB_UI_DIR="web-ui"
LOGS_DIR="logs"
FRONTEND_LOG_FILE="$LOGS_DIR/frontend.log"

# Create logs directory
mkdir -p "$LOGS_DIR"

print_status() {
    echo -e "${PURPLE}[FRONTEND]${NC} $1" | tee -a "$FRONTEND_LOG_FILE"
}

print_success() {
    echo -e "${GREEN}[FRONTEND SUCCESS]${NC} $1" | tee -a "$FRONTEND_LOG_FILE"
}

print_warning() {
    echo -e "${YELLOW}[FRONTEND WARNING]${NC} $1" | tee -a "$FRONTEND_LOG_FILE"
}

print_error() {
    echo -e "${RED}[FRONTEND ERROR]${NC} $1" | tee -a "$FRONTEND_LOG_FILE"
}

# Header
echo -e "${CYAN}╔══════════════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║${NC}                          ${PURPLE}AIMS FRONTEND SERVER${NC}                             ${CYAN}║${NC}"
echo -e "${CYAN}║${NC}                       React + Vite (Port $FRONTEND_PORT)                           ${CYAN}║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"
echo ""

print_status "$(date): Starting AIMS Frontend Server..." | tee "$FRONTEND_LOG_FILE"

# Check if web-ui directory exists
if [ ! -d "$WEB_UI_DIR" ]; then
    print_error "Frontend directory '$WEB_UI_DIR' not found!"
    print_status "Please ensure you're running this script from the project root"
    exit 1
fi

cd "$WEB_UI_DIR"
print_success "Changed to frontend directory: $PWD"

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    print_warning "node_modules not found, installing dependencies..."
    if npm install; then
        print_success "Dependencies installed successfully"
    else
        print_error "Failed to install dependencies"
        exit 1
    fi
else
    print_success "Dependencies found in node_modules"
fi

# Check for package updates (optional)
print_status "Checking for package.json changes..."
if [ package.json -nt node_modules ]; then
    print_warning "package.json is newer than node_modules, updating dependencies..."
    if npm install; then
        print_success "Dependencies updated successfully"
    else
        print_warning "Failed to update dependencies, continuing with existing ones"
    fi
fi

# Set environment variables for development
export NODE_ENV=development
export VITE_API_BASE_URL=http://localhost:8080
export VITE_APP_TITLE="AIMS E-commerce Platform"

print_status "Environment configured:"
print_status "  - Mode: $NODE_ENV"
print_status "  - API Base URL: $VITE_API_BASE_URL"
print_status "  - App Title: $VITE_APP_TITLE"

print_success "Starting Vite development server..."
print_status "Frontend will be available at: http://localhost:$FRONTEND_PORT"
print_status "Vite will automatically open your browser"

echo ""
print_status "🌐 Frontend Features:"
echo "  📱 Responsive Design (Mobile-first)"
echo "  🎨 TailwindCSS v4 Styling"
echo "  ⚡ Hot Module Replacement (HMR)"
echo "  🔄 Auto-reload on changes"
echo "  📊 Real-time backend integration"
echo ""
echo "Press Ctrl+C to stop the frontend server"
echo ""

# Start with logging
npm run dev 2>&1 | tee -a "../$FRONTEND_LOG_FILE"

print_status "$(date): Frontend server stopped" | tee -a "../$FRONTEND_LOG_FILE"