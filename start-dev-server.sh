#!/bin/bash

# AIMS Web API Development Server Startup Script
# This script starts the Spring Boot REST API server for development

echo "🚀 Starting AIMS Web API Development Server..."

# Set development environment variables
export SPRING_PROFILES_ACTIVE=dev
export SERVER_PORT=8080
export SPRING_DATASOURCE_URL=jdbc:sqlite:aims_database.db

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Java is installed
if ! command -v java &> /dev/null; then
    print_error "Java is not installed or not in PATH"
    print_status "Please install Java 17 or higher"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [[ "$JAVA_VERSION" -lt 21 ]]; then
    print_error "Java 21 or higher is required. Current version: $JAVA_VERSION"
    exit 1
fi

print_success "Java version check passed"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed or not in PATH"
    print_status "Please install Apache Maven"
    exit 1
fi

print_success "Maven found"

# Clean and compile the project
print_status "Cleaning and compiling the project..."
if mvn clean compile; then
    print_success "Project compiled successfully"
else
    print_error "Failed to compile the project"
    exit 1
fi

# Run tests (optional - can be skipped with -DskipTests)
print_status "Running tests..."
if mvn test -DskipTests=false; then
    print_success "Tests passed"
else
    print_warning "Some tests failed, but continuing..."
fi

# Start the Spring Boot application
print_status "Starting Spring Boot application..."
print_status "API will be available at: http://localhost:8080"
print_status "Swagger UI will be available at: http://localhost:8080/swagger-ui/index.html"
print_status "API documentation will be available at: http://localhost:8080/v3/api-docs"

echo ""
echo "📋 Available API Endpoints:"
echo "  🔐 Authentication: http://localhost:8080/api/auth"
echo "  📦 Products: http://localhost:8080/api/products"
echo "  🛒 Cart: http://localhost:8080/api/cart"
echo "  📋 Orders: http://localhost:8080/api/orders"
echo "  👥 Users: http://localhost:8080/api/users"
echo "  💳 Payments: http://localhost:8080/api/payments"
echo "  🔧 Admin Products: http://localhost:8080/api/admin/products"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

# Start the application
mvn spring-boot:run

print_status "Server stopped"