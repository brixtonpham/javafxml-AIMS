#!/bin/bash

# =================================
# AIMS Dual-Side Startup Script
# Master orchestrator for backend + frontend
# =================================

# Colors for output
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
BACKEND_LOG_FILE="$LOGS_DIR/backend.log"
FRONTEND_LOG_FILE="$LOGS_DIR/frontend.log"
STARTUP_LOG_FILE="$LOGS_DIR/startup.log"

# Create logs directory
mkdir -p "$LOGS_DIR"

# Function to print colored output
print_header() {
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}                           ${PURPLE}AIMS PROJECT STARTUP${NC}                              ${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}                        Automated Dual-Side Launch                           ${CYAN}║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$STARTUP_LOG_FILE"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$STARTUP_LOG_FILE"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$STARTUP_LOG_FILE"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$STARTUP_LOG_FILE"
}

# Function to check if port is available
is_port_available() {
    local port=$1
    if command -v lsof &> /dev/null; then
        ! lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1
    elif command -v netstat &> /dev/null; then
        ! netstat -an | grep -q ":$port.*LISTEN"
    else
        # Fallback: try to connect to the port
        ! nc -z localhost $port 2>/dev/null
    fi
}

# Function to wait for service to be ready
wait_for_service() {
    local port=$1
    local service_name=$2
    local max_attempts=60
    local attempt=1
    
    print_status "Waiting for $service_name to be ready on port $port..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s "http://localhost:$port" >/dev/null 2>&1 || \
           curl -s "http://localhost:$port/health" >/dev/null 2>&1 || \
           curl -s "http://localhost:$port/api/health" >/dev/null 2>&1; then
            print_success "$service_name is ready on port $port"
            return 0
        fi
        
        echo -ne "${YELLOW}[WAIT]${NC} Attempt $attempt/$max_attempts - $service_name starting...\r"
        sleep 2
        ((attempt++))
    done
    
    print_error "$service_name failed to start on port $port after $max_attempts attempts"
    return 1
}

# Function to kill process on port
kill_port_process() {
    local port=$1
    local pids=$(lsof -ti :$port 2>/dev/null)
    
    if [ ! -z "$pids" ]; then
        print_warning "Killing existing processes on port $port: $pids"
        echo $pids | xargs kill -9 2>/dev/null
        sleep 2
    fi
}

# Pre-flight system checks
print_header
print_status "$(date): Starting AIMS Project pre-flight checks..." | tee "$STARTUP_LOG_FILE"

# Check Java 21
if ! command -v java &> /dev/null; then
    print_error "Java is not installed or not in PATH"
    print_status "Please install Java 21 or higher"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [[ "$JAVA_VERSION" -lt 21 ]]; then
    print_error "Java 21 or higher is required. Current version: $JAVA_VERSION"
    exit 1
fi
print_success "Java $JAVA_VERSION detected"

# Check Maven
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed or not in PATH"
    print_status "Please install Apache Maven"
    exit 1
fi
print_success "Maven detected: $(mvn -version | head -n 1)"

# Check Node.js
if ! command -v node &> /dev/null; then
    print_error "Node.js is not installed or not in PATH"
    print_status "Please install Node.js 18 or higher"
    exit 1
fi
NODE_VERSION=$(node -v | sed 's/v//')
print_success "Node.js $NODE_VERSION detected"

# Check npm
if ! command -v npm &> /dev/null; then
    print_error "npm is not installed or not in PATH"
    exit 1
fi
print_success "npm detected: $(npm -v)"

# Check ports availability
if ! is_port_available $BACKEND_PORT; then
    print_warning "Port $BACKEND_PORT is already in use"
    kill_port_process $BACKEND_PORT
    
    if ! is_port_available $BACKEND_PORT; then
        print_error "Failed to free port $BACKEND_PORT"
        exit 1
    fi
fi

if ! is_port_available $FRONTEND_PORT; then
    print_warning "Port $FRONTEND_PORT is already in use"
    kill_port_process $FRONTEND_PORT
    
    if ! is_port_available $FRONTEND_PORT; then
        print_error "Failed to free port $FRONTEND_PORT"
        exit 1
    fi
fi

print_success "Ports $BACKEND_PORT and $FRONTEND_PORT are available"

# DEBUG mode: set to 1 to run backend/FE in background for troubleshooting
DEBUG=1

# Start Backend in new terminal
print_status "Starting Spring Boot backend in new terminal..."
if [[ "$DEBUG" == "1" ]]; then
    print_warning "[DEBUG MODE] Running backend in background for troubleshooting"
    ./scripts/start-backend.sh > "$BACKEND_LOG_FILE" 2>&1 &
else
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS - Use Terminal.app
        osascript -e "
            tell application \"Terminal\"
                do script \"cd '$PWD' && ./scripts/start-backend.sh\"
                set custom title of front window to \"AIMS Backend (Port $BACKEND_PORT)\"
            end tell
        " &
    elif command -v gnome-terminal &> /dev/null; then
        # Linux with GNOME Terminal
        gnome-terminal --title="AIMS Backend (Port $BACKEND_PORT)" --working-directory="$PWD" -- bash -c "./scripts/start-backend.sh; exec bash" &
    elif command -v xterm &> /dev/null; then
        # Fallback to xterm
        xterm -title "AIMS Backend (Port $BACKEND_PORT)" -e "cd '$PWD' && ./scripts/start-backend.sh; bash" &
    else
        # Fallback - run in background
        print_warning "No terminal emulator detected, running backend in background"
        ./scripts/start-backend.sh > "$BACKEND_LOG_FILE" 2>&1 &
    fi
fi

# Start Frontend in new terminal
print_status "Starting React frontend in new terminal..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS - Use Terminal.app
    osascript -e "
        tell application \"Terminal\"
            do script \"cd '$PWD' && ./scripts/start-frontend.sh\"
            set custom title of front window to \"AIMS Frontend (Port $FRONTEND_PORT)\"
        end tell
    " &
elif command -v gnome-terminal &> /dev/null; then
    # Linux with GNOME Terminal
    gnome-terminal --title="AIMS Frontend (Port $FRONTEND_PORT)" --working-directory="$PWD" -- bash -c "./scripts/start-frontend.sh; exec bash" &
elif command -v xterm &> /dev/null; then
    # Fallback to xterm
    xterm -title "AIMS Frontend (Port $FRONTEND_PORT)" -e "cd '$PWD' && ./scripts/start-frontend.sh; bash" &
else
    # Fallback - run in background
    print_warning "No terminal emulator detected, running frontend in background"
    ./scripts/start-frontend.sh > "$FRONTEND_LOG_FILE" 2>&1 &
fi

# Wait for services to be ready
print_status "Waiting for services to initialize..."

# Wait for backend
print_status "Checking backend availability..."
if wait_for_service $BACKEND_PORT "Spring Boot Backend"; then
    print_success "✅ Backend is running at http://localhost:$BACKEND_PORT"
    print_status "   📋 API Documentation: http://localhost:$BACKEND_PORT/swagger-ui/index.html"
    print_status "   🔗 API Endpoints: http://localhost:$BACKEND_PORT/v3/api-docs"
else
    print_error "❌ Backend failed to start properly"
    print_status "Check backend logs: tail -f $BACKEND_LOG_FILE"
    exit 1
fi

# Wait for frontend
print_status "Checking frontend availability..."
if wait_for_service $FRONTEND_PORT "React Frontend"; then
    print_success "✅ Frontend is running at http://localhost:$FRONTEND_PORT"
else
    print_error "❌ Frontend failed to start properly"
    print_status "Check frontend logs: tail -f $FRONTEND_LOG_FILE"
    exit 1
fi

# Success summary
echo ""
print_success "🎉 AIMS Project successfully started!"
echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║${NC}                              ${GREEN}SERVICES READY${NC}                                ${CYAN}║${NC}"
echo -e "${CYAN}║${NC}                                                                              ${CYAN}║${NC}"
echo -e "${CYAN}║${NC}  🌐 Frontend:  http://localhost:$FRONTEND_PORT                                        ${CYAN}║${NC}"
echo -e "${CYAN}║${NC}  🔧 Backend:   http://localhost:$BACKEND_PORT                                         ${CYAN}║${NC}"
echo -e "${CYAN}║${NC}  📋 API Docs:  http://localhost:$BACKEND_PORT/swagger-ui/index.html               ${CYAN}║${NC}"
echo -e "${CYAN}║${NC}                                                                              ${CYAN}║${NC}"
echo -e "${CYAN}║${NC}  💡 Use './stop-aims.sh' to shutdown all services                          ${CYAN}║${NC}"
echo -e "${CYAN}║${NC}  📊 Use './health-check.sh' to check system status                         ${CYAN}║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}To view backend logs: tail -f $BACKEND_LOG_FILE${NC}"
echo -e "${YELLOW}To view frontend logs: tail -f $FRONTEND_LOG_FILE${NC}"

print_status "$(date): AIMS Project startup completed successfully" | tee -a "$STARTUP_LOG_FILE"