#!/bin/bash

# API Integration Test Script
# Tests the connection between React frontend and JavaFX Spring Boot backend

echo "🚀 Testing AIMS API Integration"
echo "================================"

API_BASE="http://localhost:8080/api"
FRONTEND_URL="http://localhost:3000"

echo ""
echo "📋 Testing Backend API Endpoints..."
echo ""

# Test 1: Health Check
echo "1. Health Check:"
curl -s -w "Status: %{http_code}\n" "$API_BASE/health" || echo "❌ Backend not running on port 8080"
echo ""

# Test 2: Products Endpoint
echo "2. Products List:"
curl -s -w "Status: %{http_code}\n" "$API_BASE/products?page=1&pageSize=5" || echo "❌ Products endpoint failed"
echo ""

# Test 3: Categories Endpoint
echo "3. Categories:"
curl -s -w "Status: %{http_code}\n" "$API_BASE/products/categories" || echo "❌ Categories endpoint failed"
echo ""

# Test 4: Product Types Endpoint
echo "4. Product Types:"
curl -s -w "Status: %{http_code}\n" "$API_BASE/products/types" || echo "❌ Product types endpoint failed"
echo ""

# Test 5: CORS Preflight (Options request)
echo "5. CORS Preflight Test:"
curl -s -w "Status: %{http_code}\n" -X OPTIONS \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Content-Type" \
  "$API_BASE/products" || echo "❌ CORS preflight failed"
echo ""

# Test 6: Cart Creation
echo "6. Cart Creation:"
curl -s -w "Status: %{http_code}\n" -X POST \
  -H "Content-Type: application/json" \
  -H "Origin: http://localhost:3000" \
  "$API_BASE/cart/create" || echo "❌ Cart creation failed"
echo ""

# Test 7: Authentication Endpoint
echo "7. Authentication Endpoint Check:"
curl -s -w "Status: %{http_code}\n" -X POST \
  -H "Content-Type: application/json" \
  -H "Origin: http://localhost:3000" \
  -d '{"username":"test","password":"test"}' \
  "$API_BASE/auth/login" || echo "❌ Auth endpoint failed"
echo ""

echo "🔍 Testing Frontend Connectivity..."
echo ""

# Test Frontend
echo "8. Frontend Server:"
curl -s -w "Status: %{http_code}\n" "$FRONTEND_URL" || echo "❌ Frontend not running on port 3000"
echo ""

echo "📊 Integration Test Summary:"
echo "================================"
echo "✅ Backend should be running on: http://localhost:8080"
echo "✅ Frontend should be running on: http://localhost:3000"
echo "✅ API Base URL: $API_BASE"
echo "✅ CORS configured for: $FRONTEND_URL"
echo ""
echo "📝 Next Steps:"
echo "1. Start backend: java -jar aims-web-api.jar (or run AimsWebApiApplication.main())"
echo "2. Start frontend: cd web-ui && npm run dev"
echo "3. Test in browser: $FRONTEND_URL"
echo "4. Check browser console for API calls"
echo ""
echo "🔧 If tests fail:"
echo "- Ensure backend is running on port 8080"
echo "- Ensure frontend is running on port 3000"
echo "- Check CORS configuration"
echo "- Verify database connectivity"