#!/bin/bash

# =================================
# AIMS Health Check Script
# Service monitoring and integration testing
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
HEALTH_LOG_FILE="$LOGS_DIR/health-check.log"

# Timeout settings
CONNECTION_TIMEOUT=5
REQUEST_TIMEOUT=10

# Create logs directory
mkdir -p "$LOGS_DIR"

print_header() {
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}                           ${BLUE}AIMS HEALTH CHECK${NC}                               ${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}                        Service Monitoring & Testing                         ${CYAN}║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_status() {
    echo -e "${BLUE}[HEALTH]${NC} $1" | tee -a "$HEALTH_LOG_FILE"
}

print_success() {
    echo -e "${GREEN}[✅ PASS]${NC} $1" | tee -a "$HEALTH_LOG_FILE"
}

print_warning() {
    echo -e "${YELLOW}[⚠️  WARN]${NC} $1" | tee -a "$HEALTH_LOG_FILE"
}

print_error() {
    echo -e "${RED}[❌ FAIL]${NC} $1" | tee -a "$HEALTH_LOG_FILE"
}

# Function to check service health
check_service_health() {
    local port=$1
    local service_name=$2
    local expected_response=$3
    
    print_status "Checking $service_name on port $port..."
    
    # Check if port is listening
    if ! command -v nc &> /dev/null; then
        if ! command -v telnet &> /dev/null; then
            print_warning "Neither nc nor telnet available for port check"
        else
            if timeout $CONNECTION_TIMEOUT telnet localhost $port >/dev/null 2>&1; then
                print_success "$service_name port $port is listening"
            else
                print_error "$service_name port $port is not accessible"
                return 1
            fi
        fi
    else
        if nc -z localhost $port; then
            print_success "$service_name port $port is listening"
        else
            print_error "$service_name port $port is not accessible"
            return 1
        fi
    fi
    
    # HTTP health check
    local url="http://localhost:$port"
    if [ ! -z "$expected_response" ]; then
        url="$url/$expected_response"
    fi
    
    if command -v curl &> /dev/null; then
        local response=$(curl -s -w "%{http_code}" --connect-timeout $CONNECTION_TIMEOUT --max-time $REQUEST_TIMEOUT "$url" -o /dev/null 2>/dev/null)
        if [[ "$response" =~ ^[2-3][0-9][0-9]$ ]]; then
            print_success "$service_name HTTP health check passed (HTTP $response)"
            return 0
        else
            print_error "$service_name HTTP health check failed (HTTP $response)"
            return 1
        fi
    elif command -v wget &> /dev/null; then
        if timeout $REQUEST_TIMEOUT wget -q --spider --timeout=$CONNECTION_TIMEOUT "$url" 2>/dev/null; then
            print_success "$service_name HTTP health check passed"
            return 0
        else
            print_error "$service_name HTTP health check failed"
            return 1
        fi
    else
        print_warning "No HTTP client available (curl/wget) for detailed health check"
        return 0
    fi
}

# Function to test API endpoints
test_api_endpoint() {
    local endpoint=$1
    local expected_status=${2:-200}
    local method=${3:-GET}
    
    print_status "Testing API endpoint: $method $endpoint"
    
    if command -v curl &> /dev/null; then
        local response=$(curl -s -w "%{http_code}" -X "$method" \
            --connect-timeout $CONNECTION_TIMEOUT \
            --max-time $REQUEST_TIMEOUT \
            "http://localhost:$BACKEND_PORT$endpoint" \
            -o /dev/null 2>/dev/null)
        
        if [[ "$response" == "$expected_status" ]]; then
            print_success "API endpoint $endpoint returned expected status $expected_status"
            return 0
        else
            print_warning "API endpoint $endpoint returned status $response (expected $expected_status)"
            return 1
        fi
    else
        print_warning "curl not available for API testing"
        return 0
    fi
}

# Function to check CORS
check_cors() {
    print_status "Testing CORS configuration..."
    
    if command -v curl &> /dev/null; then
        local cors_response=$(curl -s -H "Origin: http://localhost:$FRONTEND_PORT" \
            -H "Access-Control-Request-Method: GET" \
            -H "Access-Control-Request-Headers: Content-Type" \
            --connect-timeout $CONNECTION_TIMEOUT \
            --max-time $REQUEST_TIMEOUT \
            -I "http://localhost:$BACKEND_PORT/api/products" 2>/dev/null | \
            grep -i "access-control-allow-origin")
        
        if [ ! -z "$cors_response" ]; then
            print_success "CORS headers detected: $cors_response"
            return 0
        else
            print_warning "CORS headers not detected (may need configuration)"
            return 1
        fi
    else
        print_warning "curl not available for CORS testing"
        return 0
    fi
}

# Function to check system resources
check_system_resources() {
    print_status "Checking system resources..."
    
    # Memory usage
    if command -v free &> /dev/null; then
        local mem_usage=$(free | grep Mem | awk '{printf "%.1f", $3/$2 * 100.0}')
        print_status "Memory usage: ${mem_usage}%"
        if (( $(echo "$mem_usage > 90" | bc -l) )); then
            print_warning "High memory usage detected: ${mem_usage}%"
        else
            print_success "Memory usage is acceptable: ${mem_usage}%"
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS memory check
        local mem_pressure=$(memory_pressure | grep "System-wide memory free percentage" | awk '{print $5}' | sed 's/%//')
        if [ ! -z "$mem_pressure" ]; then
            print_status "System memory free: ${mem_pressure}%"
            if (( $(echo "$mem_pressure < 10" | bc -l) )); then
                print_warning "Low memory available: ${mem_pressure}%"
            else
                print_success "Memory availability is good: ${mem_pressure}%"
            fi
        fi
    fi
    
    # Disk usage
    local disk_usage=$(df . | tail -1 | awk '{print $5}' | sed 's/%//')
    print_status "Disk usage: ${disk_usage}%"
    if [ "$disk_usage" -gt 90 ]; then
        print_warning "High disk usage: ${disk_usage}%"
    else
        print_success "Disk usage is acceptable: ${disk_usage}%"
    fi
    
    # Load average (Unix-like systems)
    if command -v uptime &> /dev/null; then
        local load_avg=$(uptime | awk -F'load average:' '{print $2}' | awk '{print $1}' | sed 's/,//')
        print_status "System load average: $load_avg"
    fi
}

# Function to check log files for errors
check_logs() {
    print_status "Checking recent log files for errors..."
    
    local error_count=0
    
    # Check backend logs
    if [ -f "$LOGS_DIR/backend.log" ]; then
        local backend_errors=$(tail -50 "$LOGS_DIR/backend.log" 2>/dev/null | grep -i "error\|exception\|failed" | wc -l)
        if [ "$backend_errors" -gt 0 ]; then
            print_warning "Found $backend_errors error entries in backend logs"
            error_count=$((error_count + backend_errors))
        else
            print_success "No recent errors in backend logs"
        fi
    fi
    
    # Check frontend logs
    if [ -f "$LOGS_DIR/frontend.log" ]; then
        local frontend_errors=$(tail -50 "$LOGS_DIR/frontend.log" 2>/dev/null | grep -i "error\|failed" | wc -l)
        if [ "$frontend_errors" -gt 0 ]; then
            print_warning "Found $frontend_errors error entries in frontend logs"
            error_count=$((error_count + frontend_errors))
        else
            print_success "No recent errors in frontend logs"
        fi
    fi
    
    if [ "$error_count" -eq 0 ]; then
        print_success "No critical errors found in recent logs"
    else
        print_warning "Total errors found in logs: $error_count"
    fi
}

# Main health check execution
print_header
print_status "$(date): Starting AIMS health check..." | tee "$HEALTH_LOG_FILE"

backend_healthy=true
frontend_healthy=true
integration_healthy=true

# Check backend
print_status "🔧 Backend Health Check"
if ! check_service_health $BACKEND_PORT "Backend" "actuator/health"; then
    backend_healthy=false
fi

# Check frontend
print_status "🌐 Frontend Health Check"
if ! check_service_health $FRONTEND_PORT "Frontend"; then
    frontend_healthy=false
fi

# API endpoint tests (only if backend is running)
if [ "$backend_healthy" = true ]; then
    print_status "🔗 API Endpoint Tests"
    
    # Test key API endpoints
    test_api_endpoint "/api/products" 200
    test_api_endpoint "/api/auth/status" 200
    test_api_endpoint "/v3/api-docs" 200
    test_api_endpoint "/swagger-ui/index.html" 200
    
    # Test CORS
    if [ "$frontend_healthy" = true ]; then
        if ! check_cors; then
            integration_healthy=false
        fi
    fi
else
    print_error "Skipping API tests - backend not healthy"
    integration_healthy=false
fi

# Integration test (frontend-backend communication)
if [ "$backend_healthy" = true ] && [ "$frontend_healthy" = true ]; then
    print_status "🔄 Integration Test"
    
    # Test if frontend can reach backend
    if command -v curl &> /dev/null; then
        local integration_test=$(curl -s --connect-timeout $CONNECTION_TIMEOUT \
            --max-time $REQUEST_TIMEOUT \
            -H "Origin: http://localhost:$FRONTEND_PORT" \
            "http://localhost:$BACKEND_PORT/api/products" 2>/dev/null)
        
        if [ $? -eq 0 ]; then
            print_success "Frontend-Backend integration test passed"
        else
            print_error "Frontend-Backend integration test failed"
            integration_healthy=false
        fi
    fi
else
    print_warning "Skipping integration tests - services not healthy"
    integration_healthy=false
fi

# System resource check
print_status "💻 System Resources"
check_system_resources

# Log file analysis
print_status "📋 Log Analysis"
check_logs

# Final summary
echo ""
overall_health=true

if [ "$backend_healthy" = false ]; then
    overall_health=false
fi

if [ "$frontend_healthy" = false ]; then
    overall_health=false
fi

if [ "$integration_healthy" = false ]; then
    overall_health=false
fi

if [ "$overall_health" = true ]; then
    echo -e "${GREEN}╔══════════════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║${NC}                              ${GREEN}HEALTH CHECK PASSED${NC}                            ${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}                                                                              ${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}  ✅ Backend: Running and healthy                                           ${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}  ✅ Frontend: Running and healthy                                          ${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}  ✅ Integration: Services communicating properly                           ${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}  ✅ System: Resources within acceptable limits                             ${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}                                                                              ${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}  🌐 Frontend: http://localhost:$FRONTEND_PORT                                       ${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}  🔧 Backend:  http://localhost:$BACKEND_PORT                                        ${GREEN}║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"
    
    print_status "$(date): Health check completed - All systems healthy" | tee -a "$HEALTH_LOG_FILE"
    exit 0
else
    echo -e "${RED}╔══════════════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║${NC}                             ${RED}HEALTH CHECK ISSUES${NC}                             ${RED}║${NC}"
    echo -e "${RED}║${NC}                                                                              ${RED}║${NC}"
    
    if [ "$backend_healthy" = false ]; then
        echo -e "${RED}║${NC}  ❌ Backend: Issues detected                                               ${RED}║${NC}"
    else
        echo -e "${RED}║${NC}  ✅ Backend: Healthy                                                       ${RED}║${NC}"
    fi
    
    if [ "$frontend_healthy" = false ]; then
        echo -e "${RED}║${NC}  ❌ Frontend: Issues detected                                              ${RED}║${NC}"
    else
        echo -e "${RED}║${NC}  ✅ Frontend: Healthy                                                      ${RED}║${NC}"
    fi
    
    if [ "$integration_healthy" = false ]; then
        echo -e "${RED}║${NC}  ❌ Integration: Communication issues                                      ${RED}║${NC}"
    else
        echo -e "${RED}║${NC}  ✅ Integration: Working                                                   ${RED}║${NC}"
    fi
    
    echo -e "${RED}║${NC}                                                                              ${RED}║${NC}"
    echo -e "${RED}║${NC}  💡 Check individual service logs for details                               ${RED}║${NC}"
    echo -e "${RED}║${NC}  📋 Use './start-aims.sh' to restart services                               ${RED}║${NC}"
    echo -e "${RED}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"
    
    print_status "$(date): Health check completed - Issues detected" | tee -a "$HEALTH_LOG_FILE"
    exit 1
fi