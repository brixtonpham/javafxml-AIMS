// Simple integration test for the search functionality
package com.aims.test.integration;

import com.aims.core.application.impl.ProductServiceImpl;
import com.aims.core.application.services.IProductService;
import com.aims.core.infrastructure.database.dao.ProductDAOImpl;
import com.aims.core.entities.Product;
import com.aims.core.shared.utils.SearchResult;
import com.aims.core.infrastructure.database.SQLiteConnector;

import java.sql.SQLException;
import java.util.List;

/**
 * Integration test to verify the complete search functionality pipeline
 */
public class SearchFunctionalityIntegrationTest {
    
    public static void main(String[] args) {
        System.out.println("=== AIMS Search Functionality Integration Test ===");
        
        try {
            // Test database connection
            SQLiteConnector.getInstance().getConnection();
            System.out.println("✓ Database connection established");
            
            // Initialize services
            ProductDAOImpl productDAO = new ProductDAOImpl();
            IProductService productService = new ProductServiceImpl(productDAO);
            System.out.println("✓ Services initialized");
            
            // Test 1: Basic search functionality
            testBasicSearch(productService);
            
            // Test 2: Category filtering
            testCategoryFiltering(productService);
            
            // Test 3: Sorting functionality  
            testSorting(productService);
            
            // Test 4: Pagination
            testPagination(productService);
            
            // Test 5: VAT calculation
            testVATCalculation(productService);
            
            System.out.println("\n=== All Tests Completed Successfully! ===");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed with error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testBasicSearch(IProductService productService) throws SQLException {
        System.out.println("\n--- Test 1: Basic Search ---");
        
        SearchResult<Product> result = productService.advancedSearchProducts(
            "java", null, "title", "ASC", 1, 10);
        
        System.out.println("Search term: 'java'");
        System.out.println("Results found: " + result.totalResults());
        System.out.println("Results on page: " + result.results().size());
        
        if (!result.results().isEmpty()) {
            Product firstProduct = result.results().get(0);
            System.out.println("First result: " + firstProduct.getTitle());
            System.out.println("Price with VAT: " + firstProduct.getPrice() + " VND");
        }
        
        System.out.println("✓ Basic search test completed");
    }
    
    private static void testCategoryFiltering(IProductService productService) throws SQLException {
        System.out.println("\n--- Test 2: Category Filtering ---");
        
        List<String> categories = productService.getAllCategories();
        System.out.println("Available categories: " + categories);
        
        if (!categories.isEmpty()) {
            String testCategory = categories.get(0);
            SearchResult<Product> result = productService.advancedSearchProducts(
                null, testCategory, "title", "ASC", 1, 5);
            
            System.out.println("Category: " + testCategory);
            System.out.println("Results found: " + result.totalResults());
        }
        
        System.out.println("✓ Category filtering test completed");
    }
    
    private static void testSorting(IProductService productService) throws SQLException {
        System.out.println("\n--- Test 3: Sorting ---");
        
        // Test ascending price sort
        SearchResult<Product> ascResult = productService.advancedSearchProducts(
            null, null, "price", "ASC", 1, 3);
        
        // Test descending price sort
        SearchResult<Product> descResult = productService.advancedSearchProducts(
            null, null, "price", "DESC", 1, 3);
        
        System.out.println("Price ASC - first 3 results:");
        for (Product p : ascResult.results()) {
            System.out.println("  " + p.getTitle() + " - " + p.getPrice() + " VND");
        }
        
        System.out.println("Price DESC - first 3 results:");
        for (Product p : descResult.results()) {
            System.out.println("  " + p.getTitle() + " - " + p.getPrice() + " VND");
        }
        
        System.out.println("✓ Sorting test completed");
    }
    
    private static void testPagination(IProductService productService) throws SQLException {
        System.out.println("\n--- Test 4: Pagination ---");
        
        SearchResult<Product> page1 = productService.advancedSearchProducts(
            null, null, "title", "ASC", 1, 5);
        
        SearchResult<Product> page2 = productService.advancedSearchProducts(
            null, null, "title", "ASC", 2, 5);
        
        System.out.println("Page 1: " + page1.results().size() + " results");
        System.out.println("Page 2: " + page2.results().size() + " results");
        System.out.println("Total pages: " + page1.totalPages());
        System.out.println("Total results: " + page1.totalResults());
        
        System.out.println("✓ Pagination test completed");
    }
    
    private static void testVATCalculation(IProductService productService) throws SQLException {
        System.out.println("\n--- Test 5: VAT Calculation ---");
        
        SearchResult<Product> result = productService.advancedSearchProducts(
            null, null, "title", "ASC", 1, 1);
        
        if (!result.results().isEmpty()) {
            Product product = result.results().get(0);
            double priceWithVAT = product.getPrice();
            double basePrice = priceWithVAT / 1.10; // Reverse calculate base price
            double vatAmount = priceWithVAT - basePrice;
            
            System.out.println("Product: " + product.getTitle());
            System.out.println("Price with VAT: " + String.format("%.0f", priceWithVAT) + " VND");
            System.out.println("Base price: " + String.format("%.0f", basePrice) + " VND");
            System.out.println("VAT amount (10%): " + String.format("%.0f", vatAmount) + " VND");
        }
        
        System.out.println("✓ VAT calculation test completed");
    }
}
