#!/bin/bash

# CORS and API Connectivity Test Script
# This script tests the connection between frontend and backend

echo "🚀 Starting AIMS CORS and API Connectivity Test"
echo "================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Check if backend is running
echo -e "\n${YELLOW}Test 1: Checking if Spring Boot backend is running...${NC}"
if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Backend is running on port 8080${NC}"
    BACKEND_STATUS=$(curl -s http://localhost:8080/api/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    echo "   Backend Status: $BACKEND_STATUS"
else
    echo -e "${RED}❌ Backend is not running on port 8080${NC}"
    echo "   Please start the Spring Boot backend first:"
    echo "   cd /Users/namu10x/workspace/hust/javafxml-AIMS"
    echo "   ./mvnw spring-boot:run"
    exit 1
fi

# Test 2: Test CORS with simple GET request
echo -e "\n${YELLOW}Test 2: Testing CORS configuration...${NC}"
CORS_TEST=$(curl -s -w "%{http_code}" -H "Origin: http://localhost:3000" \
    -H "Access-Control-Request-Method: GET" \
    -H "Access-Control-Request-Headers: Content-Type" \
    -X OPTIONS http://localhost:8080/api/test)

if [[ "$CORS_TEST" == *"200"* ]]; then
    echo -e "${GREEN}✅ CORS preflight request successful${NC}"
else
    echo -e "${RED}❌ CORS preflight request failed (Status: $CORS_TEST)${NC}"
fi

# Test 3: Test API endpoint access
echo -e "\n${YELLOW}Test 3: Testing API endpoint access...${NC}"
API_TEST=$(curl -s -w "%{http_code}" -H "Origin: http://localhost:3000" http://localhost:8080/api/test)
if [[ "$API_TEST" == *"200"* ]]; then
    echo -e "${GREEN}✅ API endpoint accessible from frontend origin${NC}"
else
    echo -e "${RED}❌ API endpoint not accessible (Status: $API_TEST)${NC}"
fi

# Test 4: Test products endpoint
echo -e "\n${YELLOW}Test 4: Testing products endpoint...${NC}"
PRODUCTS_TEST=$(curl -s -w "%{http_code}" -H "Origin: http://localhost:3000" http://localhost:8080/api/products)
if [[ "$PRODUCTS_TEST" == *"200"* ]]; then
    echo -e "${GREEN}✅ Products endpoint accessible${NC}"
else
    echo -e "${RED}❌ Products endpoint not accessible (Status: $PRODUCTS_TEST)${NC}"
fi

# Test 5: Check if frontend development server is running
echo -e "\n${YELLOW}Test 5: Checking if frontend is running...${NC}"
if curl -s http://localhost:3000 > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Frontend is running on port 3000${NC}"
else
    echo -e "${YELLOW}⚠️  Frontend is not running on port 3000${NC}"
    echo "   To start the frontend:"
    echo "   cd /Users/namu10x/workspace/hust/javafxml-AIMS/web-ui"
    echo "   npm run dev"
fi

echo -e "\n${YELLOW}================================================${NC}"
echo "🏁 CORS and API Test Complete"
echo ""
echo "If all tests pass, your CORS configuration is working correctly!"
echo "If tests fail, check the backend logs and ensure all servers are running."
