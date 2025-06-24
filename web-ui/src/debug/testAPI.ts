// Test the products API directly
import { productsRequest } from '../services/api';
import { productService } from '../services/productService';

// This is a simple test to verify our API changes
async function testProductsAPI() {
  console.log('Testing products API...');
  
  try {
    // Test the direct API call
    console.log('1. Testing productsRequest directly...');
    const directResult = await productsRequest('/products', {
      page: 1,
      pageSize: 5,
      sortBy: 'entryDate',
      sortOrder: 'DESC'
    });
    console.log('Direct result:', directResult);
    
    // Test the service call
    console.log('2. Testing productService.getProducts...');
    const serviceResult = await productService.getProducts({
      page: 1,
      pageSize: 5,
      sortBy: 'entryDate',
      sortOrder: 'DESC'
    });
    console.log('Service result:', serviceResult);
    
    console.log('API test completed successfully!');
    return { success: true, directResult, serviceResult };
  } catch (error) {
    console.error('API test failed:', error);
    return { success: false, error };
  }
}

// Auto-run the test when this module is imported
testProductsAPI();

export { testProductsAPI };
