#!/usr/bin/env node

// Test script to verify frontend-backend integration
const fetch = require('node-fetch');

async function testIntegration() {
  console.log('🧪 Testing Frontend-Backend Integration');
  console.log('======================================');
  
  const frontendUrl = 'http://localhost:3001';
  const backendUrl = 'http://localhost:8080/api';
  
  // Test 1: Check if frontend is accessible
  try {
    console.log('\n📱 Test 1: Frontend Accessibility...');
    const frontendResponse = await fetch(frontendUrl);
    if (frontendResponse.ok) {
      console.log('✅ Frontend is accessible');
    } else {
      console.log('❌ Frontend returned status:', frontendResponse.status);
    }
  } catch (error) {
    console.log('❌ Frontend not accessible:', error.message);
  }
  
  // Test 2: Check if backend API is accessible
  try {
    console.log('\n🔧 Test 2: Backend API Accessibility...');
    const backendResponse = await fetch(`${backendUrl}/products`);
    if (backendResponse.ok) {
      const data = await backendResponse.json();
      console.log('✅ Backend API is accessible');
      console.log(`   📊 Retrieved ${data.items?.length || 0} products`);
    } else {
      console.log('❌ Backend API returned status:', backendResponse.status);
    }
  } catch (error) {
    console.log('❌ Backend API not accessible:', error.message);
  }
  
  // Test 3: Test CORS headers
  try {
    console.log('\n🌐 Test 3: CORS Headers...');
    const corsResponse = await fetch(`${backendUrl}/products`, {
      method: 'GET',
      headers: {
        'Origin': frontendUrl,
        'Accept': 'application/json'
      }
    });
    
    const corsHeaders = {
      'access-control-allow-origin': corsResponse.headers.get('access-control-allow-origin'),
      'access-control-allow-credentials': corsResponse.headers.get('access-control-allow-credentials')
    };
    
    if (corsHeaders['access-control-allow-origin'] === frontendUrl) {
      console.log('✅ CORS headers are correctly configured');
      console.log(`   🎯 Origin: ${corsHeaders['access-control-allow-origin']}`);
      console.log(`   🔐 Credentials: ${corsHeaders['access-control-allow-credentials']}`);
    } else {
      console.log('❌ CORS headers missing or incorrect');
      console.log('   Received:', corsHeaders);
    }
  } catch (error) {
    console.log('❌ CORS test failed:', error.message);
  }
  
  // Test 4: Test API endpoints
  const endpoints = [
    '/products',
    '/categories', 
    '/types'
  ];
  
  console.log('\n🎯 Test 4: API Endpoints...');
  for (const endpoint of endpoints) {
    try {
      const response = await fetch(`${backendUrl}${endpoint}`, {
        headers: { 'Origin': frontendUrl }
      });
      
      if (response.ok) {
        const data = await response.json();
        console.log(`✅ ${endpoint} - Status: ${response.status}`);
        if (data.items) {
          console.log(`   📊 Data: ${data.items.length} items`);
        } else if (data.data) {
          console.log(`   📊 Data: ${data.data.length} items`);
        }
      } else {
        console.log(`❌ ${endpoint} - Status: ${response.status}`);
      }
    } catch (error) {
      console.log(`❌ ${endpoint} - Error: ${error.message}`);
    }
  }
  
  console.log('\n🏁 Integration Test Complete!');
}

testIntegration().catch(console.error);
