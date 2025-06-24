#!/bin/bash

# Test script to verify frontend-backend integration using curl
echo "🧪 Testing Frontend-Backend Integration with curl"
echo "================================================="

FRONTEND_URL="http://localhost:3001"
BACKEND_URL="http://localhost:8080/api"

# Test 1: Check if frontend is accessible
echo -e "\n📱 Test 1: Frontend Accessibility..."
if curl -s --head "$FRONTEND_URL" | head -n 1 | grep -q "200 OK"; then
    echo "✅ Frontend is accessible at $FRONTEND_URL"
else
    echo "❌ Frontend not accessible"
fi

# Test 2: Check if backend API is accessible
echo -e "\n🔧 Test 2: Backend API Products Endpoint..."
PRODUCTS_RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" "$BACKEND_URL/products")
HTTP_STATUS=$(echo "$PRODUCTS_RESPONSE" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
RESPONSE_BODY=$(echo "$PRODUCTS_RESPONSE" | sed 's/HTTPSTATUS:[0-9]*$//')

if [ "$HTTP_STATUS" -eq 200 ]; then
    echo "✅ Backend API is accessible"
    PRODUCT_COUNT=$(echo "$RESPONSE_BODY" | grep -o '"productId"' | wc -l)
    echo "   📊 Retrieved $PRODUCT_COUNT products"
else
    echo "❌ Backend API returned status: $HTTP_STATUS"
fi

# Test 3: Test CORS headers
echo -e "\n🌐 Test 3: CORS Headers..."
CORS_RESPONSE=$(curl -s -H "Origin: $FRONTEND_URL" -H "Accept: application/json" -v "$BACKEND_URL/products" 2>&1)
if echo "$CORS_RESPONSE" | grep -q "Access-Control-Allow-Origin: $FRONTEND_URL"; then
    echo "✅ CORS headers are correctly configured"
    echo "   🎯 Origin allowed: $FRONTEND_URL"
    if echo "$CORS_RESPONSE" | grep -q "Access-Control-Allow-Credentials: true"; then
        echo "   🔐 Credentials: enabled"
    fi
else
    echo "❌ CORS headers missing or incorrect"
    echo "Debug: Checking for CORS headers..."
    echo "$CORS_RESPONSE" | grep -i "access-control" || echo "No CORS headers found"
fi

# Test 4: Test API endpoints
echo -e "\n🎯 Test 4: API Endpoints..."
ENDPOINTS=("/products" "/categories" "/types")

for endpoint in "${ENDPOINTS[@]}"; do
    RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -H "Origin: $FRONTEND_URL" "$BACKEND_URL$endpoint")
    STATUS=$(echo "$RESPONSE" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
    
    if [ "$STATUS" -eq 200 ]; then
        echo "✅ $endpoint - Status: $STATUS"
        # Try to count items in response
        BODY=$(echo "$RESPONSE" | sed 's/HTTPSTATUS:[0-9]*$//')
        if echo "$BODY" | grep -q '"items"'; then
            COUNT=$(echo "$BODY" | grep -o '"productId"\|"title"' | wc -l)
            echo "   📊 Data: $COUNT items"
        elif echo "$BODY" | grep -q '"data"'; then
            COUNT=$(echo "$BODY" | grep -o ',' | wc -l)
            echo "   📊 Data: ~$COUNT items"
        fi
    else
        echo "❌ $endpoint - Status: $STATUS"
    fi
done

echo -e "\n🏁 Integration Test Complete!"
echo "If all tests show ✅, your CORS fix is working correctly!"
