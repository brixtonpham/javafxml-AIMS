@echo off
REM AIMS Web API Development Server Startup Script for Windows
REM This script starts the Spring Boot REST API server for development

echo 🚀 Starting AIMS Web API Development Server...

REM Set development environment variables
set SPRING_PROFILES_ACTIVE=dev
set SERVER_PORT=8080
set SPRING_DATASOURCE_URL=jdbc:sqlite:aims_database.db

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java is not installed or not in PATH
    echo [INFO] Please install Java 17 or higher
    pause
    exit /b 1
)

echo [SUCCESS] Java version check passed

REM Check if Maven is installed
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Maven is not installed or not in PATH
    echo [INFO] Please install Apache Maven
    pause
    exit /b 1
)

echo [SUCCESS] Maven found

REM Clean and compile the project
echo [INFO] Cleaning and compiling the project...
call mvn clean compile
if %errorlevel% neq 0 (
    echo [ERROR] Failed to compile the project
    pause
    exit /b 1
)

echo [SUCCESS] Project compiled successfully

REM Run tests (optional - can be skipped with -DskipTests)
echo [INFO] Running tests...
call mvn test -DskipTests=false
if %errorlevel% neq 0 (
    echo [WARNING] Some tests failed, but continuing...
) else (
    echo [SUCCESS] Tests passed
)

REM Start the Spring Boot application
echo [INFO] Starting Spring Boot application...
echo [INFO] API will be available at: http://localhost:8080
echo [INFO] Swagger UI will be available at: http://localhost:8080/swagger-ui/index.html
echo [INFO] API documentation will be available at: http://localhost:8080/v3/api-docs

echo.
echo 📋 Available API Endpoints:
echo   🔐 Authentication: http://localhost:8080/api/auth
echo   📦 Products: http://localhost:8080/api/products
echo   🛒 Cart: http://localhost:8080/api/cart
echo   📋 Orders: http://localhost:8080/api/orders
echo   👥 Users: http://localhost:8080/api/users
echo   💳 Payments: http://localhost:8080/api/payments
echo   🔧 Admin Products: http://localhost:8080/api/admin/products
echo.
echo Press Ctrl+C to stop the server
echo.

REM Start the application
call mvn spring-boot:run

echo [INFO] Server stopped
pause