#!/usr/bin/env node

/**
 * Product Listing Page Test Script
 * Tests the key functionality of the product listing page
 */

const colors = {
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  reset: '\x1b[0m'
};

const log = {
  success: (msg) => console.log(`${colors.green}✓${colors.reset} ${msg}`),
  error: (msg) => console.log(`${colors.red}✗${colors.reset} ${msg}`),
  warning: (msg) => console.log(`${colors.yellow}⚠${colors.reset} ${msg}`),
  info: (msg) => console.log(`${colors.blue}ℹ${colors.reset} ${msg}`)
};

async function testAPI(url, description) {
  try {
    const response = await fetch(url);
    const data = await response.json();
    
    if (data.success && data.items && Array.isArray(data.items)) {
      log.success(`${description}: ${data.items.length} items returned`);
      return true;
    } else {
      log.error(`${description}: Invalid response format`);
      return false;
    }
  } catch (error) {
    log.error(`${description}: ${error.message}`);
    return false;
  }
}

async function runTests() {
  console.log('🧪 Testing AIMS Product Listing API Endpoints\n');
  
  const baseURL = 'http://localhost:8080/api';
  let passed = 0;
  let total = 0;
  
  // Test 1: Basic product listing
  total++;
  if (await testAPI(`${baseURL}/products`, 'Basic product listing')) {
    passed++;
  }
  
  // Test 2: Pagination
  total++;
  if (await testAPI(`${baseURL}/products?page=2&pageSize=5`, 'Pagination (page 2, size 5)')) {
    passed++;
  }
  
  // Test 3: Keyword search
  total++;
  if (await testAPI(`${baseURL}/products?keyword=thriller`, 'Keyword search (thriller)')) {
    passed++;
  }
  
  // Test 4: Product type filter
  total++;
  if (await testAPI(`${baseURL}/products?productType=DVDs`, 'Product type filter (DVDs)')) {
    passed++;
  }
  
  // Test 5: Combined filters
  total++;
  if (await testAPI(`${baseURL}/products?keyword=collection&productType=DVDs`, 'Combined filters (keyword + type)')) {
    passed++;
  }
  
  // Test 6: Sorting
  total++;
  if (await testAPI(`${baseURL}/products?sortBy=price&sortOrder=ASC`, 'Sorting (price ASC)')) {
    passed++;
  }
  
  // Test 7: Product types endpoint
  total++;
  try {
    const response = await fetch(`${baseURL}/types`);
    const data = await response.json();
    
    if (data.success && data.data && Array.isArray(data.data)) {
      log.success(`Product types endpoint: ${data.data.length} types returned`);
      log.info(`Available types: ${data.data.join(', ')}`);
      passed++;
    } else {
      log.error('Product types endpoint: Invalid response format');
    }
  } catch (error) {
    log.error(`Product types endpoint: ${error.message}`);
  }
  
  // Test 8: Categories endpoint
  total++;
  try {
    const response = await fetch(`${baseURL}/categories`);
    const data = await response.json();
    
    if (data.success && data.data && Array.isArray(data.data)) {
      log.success(`Categories endpoint: ${data.data.length} categories returned`);
      passed++;
    } else {
      log.error('Categories endpoint: Invalid response format');
    }
  } catch (error) {
    log.error(`Categories endpoint: ${error.message}`);
  }
  
  // Summary
  console.log('\n📊 Test Results:');
  console.log(`   Passed: ${passed}/${total}`);
  console.log(`   Success Rate: ${Math.round((passed/total) * 100)}%`);
  
  if (passed === total) {
    log.success('All tests passed! ✨');
  } else {
    log.warning(`${total - passed} test(s) failed`);
  }
  
  console.log('\n🌐 Frontend URLs to test:');
  console.log('   Basic listing: http://localhost:5173/products');
  console.log('   Search: http://localhost:5173/products?keyword=thriller');
  console.log('   Filter: http://localhost:5173/products?productType=DVDs');
  console.log('   Pagination: http://localhost:5173/products?page=2');
  console.log('   Combined: http://localhost:5173/products?keyword=collection&productType=DVDs&page=1');
}

// Run tests if this file is executed directly
if (require.main === module) {
  runTests().catch(console.error);
}

module.exports = { testAPI, runTests };
