#!/bin/bash

# AIMS Development Environment Quick Start Script
# This script starts both backend and frontend servers with proper configuration

echo "🚀 Starting AIMS Development Environment"
echo "========================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project root directory
PROJECT_ROOT="/Users/namu10x/workspace/hust/javafxml-AIMS"
WEB_UI_DIR="$PROJECT_ROOT/web-ui"

# Function to check if a port is in use
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null; then
        return 0  # Port is in use
    else
        return 1  # Port is free
    fi
}

# Function to kill process on port
kill_port() {
    local port=$1
    echo -e "${YELLOW}Killing process on port $port...${NC}"
    lsof -ti:$port | xargs kill -9 2>/dev/null
}

echo -e "\n${BLUE}Step 1: Checking ports...${NC}"

# Check if ports are in use
if check_port 8080; then
    echo -e "${YELLOW}⚠️  Port 8080 is already in use${NC}"
    read -p "Kill existing process on port 8080? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        kill_port 8080
    fi
fi

if check_port 3000; then
    echo -e "${YELLOW}⚠️  Port 3000 is already in use${NC}"
    read -p "Kill existing process on port 3000? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        kill_port 3000
    fi
fi

echo -e "\n${BLUE}Step 2: Starting Spring Boot Backend...${NC}"
cd "$PROJECT_ROOT"

# Start backend in background
echo "Starting backend server..."
nohup ./mvnw spring-boot:run > logs/backend.log 2>&1 &
BACKEND_PID=$!
echo "Backend PID: $BACKEND_PID"

# Wait for backend to start
echo "Waiting for backend to start..."
for i in {1..30}; do
    if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Backend started successfully${NC}"
        break
    fi
    echo -n "."
    sleep 2
done

if ! curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
    echo -e "${RED}❌ Backend failed to start${NC}"
    echo "Check logs/backend.log for details"
    exit 1
fi

echo -e "\n${BLUE}Step 3: Starting React Frontend...${NC}"
cd "$WEB_UI_DIR"

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
    echo "Installing frontend dependencies..."
    npm install
fi

# Start frontend in background
echo "Starting frontend server..."
nohup npm run dev > ../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
echo "Frontend PID: $FRONTEND_PID"

# Wait for frontend to start
echo "Waiting for frontend to start..."
for i in {1..20}; do
    if curl -s http://localhost:3000 > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Frontend started successfully${NC}"
        break
    fi
    echo -n "."
    sleep 2
done

echo -e "\n${BLUE}Step 4: Testing CORS configuration...${NC}"
cd "$PROJECT_ROOT"
./test-cors.sh

echo -e "\n${GREEN}🎉 AIMS Development Environment Started Successfully!${NC}"
echo "========================================================"
echo -e "📱 Frontend: ${BLUE}http://localhost:3000${NC}"
echo -e "🖥️  Backend:  ${BLUE}http://localhost:8080${NC}"
echo -e "📊 API Health: ${BLUE}http://localhost:8080/api/health${NC}"
echo ""
echo -e "${YELLOW}Process IDs:${NC}"
echo "Backend PID: $BACKEND_PID"
echo "Frontend PID: $FRONTEND_PID"
echo ""
echo -e "${YELLOW}To stop servers:${NC}"
echo "kill $BACKEND_PID $FRONTEND_PID"
echo "or run: ./stop-aims.sh"
echo ""
echo -e "${YELLOW}Logs:${NC}"
echo "Backend: logs/backend.log"
echo "Frontend: logs/frontend.log"
